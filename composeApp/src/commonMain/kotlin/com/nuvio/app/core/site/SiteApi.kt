package com.nuvio.app.core.site

import com.nuvio.app.core.build.AppVersionConfig
import com.nuvio.app.features.addons.httpRequestRaw
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.cancellation.CancellationException

/**
 * Result of one call to the site API.
 *
 * [transportError] is non-null only when the request never produced an HTTP status (DNS
 * failure, timeout, connection refused...). The server's own text for a rejected call lives in
 * [message], because that is what the user should actually read.
 */
data class SiteCall(
    val code: Int,
    val body: JsonObject?,
    val transportError: String? = null,
) {
    val isTransportFailure: Boolean get() = transportError != null

    val ok: Boolean get() = transportError == null && code in 200..299

    /** The message the server wrote, if any. */
    val message: String?
        get() = body?.stringOrNull("message")?.takeIf { it.isNotBlank() }

    fun errorText(fallback: String): String = message ?: transportError ?: fallback
}

/**
 * The app's only connection to its own backend.
 *
 * Everything the site controls - account, source allow-list, kill switch - arrives through these
 * calls, so the base URL and the API key live here rather than being scattered across
 * repositories. They sit in their own store so that logging out (which clears the session) can
 * never erase them.
 *
 * A 403 from [verifyKey] is the only thing that blocks the app; every other failure is reported
 * and then ignored, because an unreachable server must not turn a phone into a brick.
 */
object SiteApi {

    /**
     * Where the panel lives. A blank `SITE_BASE_URL` build property falls back to this.
     *
     * `myapp.cinepix.top` is the app's own A record (163.128.216.61, DNS-only, no Cloudflare edge),
     * so requests reach the panel straight instead of hopping through the Worker. Port 9999 because
     * nothing answers on 80/443 at the origin - cleartext is fine, `network_security_config.xml`
     * permits it - and the panel derives every absolute URL it hands out from the Host it was asked
     * for, so a direct hit also rewrites the addon transport URLs to this host.
     */
    const val PUBLIC_BASE_URL = "http://163.128.216.61:9999"

    /** Overridable from gradle.properties as `SITE_BASE_URL`, for pointing a build elsewhere. */
    val DEFAULT_BASE_URL: String
        get() = SiteConfig.BASE_URL.trim().trimEnd('/').takeIf { it.isNotEmpty() } ?: PUBLIC_BASE_URL

    /** Same key, baked at build time; the panel's value normally wins from Settings. */
    val BAKED_API_KEY: String get() = SiteConfig.API_KEY.trim()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // ---- configuration -------------------------------------------------------------------

    /** The panel's override (Settings) wins over whatever this build was given. */
    val baseUrl: String
        get() = SiteStorage.loadBaseUrlOverride()
            ?.trim()
            ?.trimEnd('/')
            ?.takeIf { it.isNotEmpty() }
            ?: DEFAULT_BASE_URL

    val apiKey: String
        get() = SiteStorage.loadApiKeyOverride()?.trim()?.takeIf { it.isNotEmpty() } ?: BAKED_API_KEY

    fun setBaseUrl(url: String) = SiteStorage.saveBaseUrlOverride(url.trim().trimEnd('/'))

    fun setApiKey(key: String) = SiteStorage.saveApiKeyOverride(key.trim())

    // ---- session -------------------------------------------------------------------------

    val sessionToken: String? get() = SiteStorage.loadSessionToken()

    val sessionEmail: String? get() = SiteStorage.loadSessionEmail()

    /**
     * Guest mode: the app opens with no site account at all (login is optional).
     */
    val isGuest: Boolean get() = SiteStorage.loadGuest()

    fun setGuest(value: Boolean) = SiteStorage.saveGuest(value)

    // ---- calls ---------------------------------------------------------------------------

    suspend fun login(email: String, password: String): SiteCall {
        val call = post("/api/login", json.encodeToString(SiteCredentials(email, password)))
        val token = call.body?.stringOrNull("token").orEmpty()
        if (call.ok && token.isNotBlank()) {
            SiteStorage.saveSession(token, email)
            SiteStorage.saveGuest(false)
        }
        return call
    }

    suspend fun register(email: String, password: String, marketing: Boolean): SiteCall {
        val call = post(
            "/api/register",
            json.encodeToString(SiteRegisterRequest(email, password, marketing)),
        )
        val token = call.body?.stringOrNull("token").orEmpty()
        if (call.ok && token.isNotBlank()) {
            SiteStorage.saveSession(token, email)
            SiteStorage.saveGuest(false)
        }
        return call
    }

    /** Tells the server to drop the token, then drops it locally regardless of the answer. */
    suspend fun logout(): SiteCall {
        val call = post("/api/admin/logout", body = "", session = true)
        SiteStorage.clearSession()
        return call
    }

    /** Who am I. Returns `user: null` when the token is stale, which callers treat as logged out. */
    suspend fun account(): SiteCall = get("/api/account/me", session = true)

    /** Public source/kill-switch payload. Never needs a session. */
    suspend fun bootstrap(): SiteCall = get("/api/app/bootstrap", session = false)

    /** 403 here means "your key is not welcome any more" - the only thing that blocks the app. */
    suspend fun verifyKey(): SiteCall = get("/api/app/verify-key", session = false, appKey = true)

    // ---- bootstrap (the source allow-list + kill switch) ---------------------------------

    /**
     * Fresh payload, cached on success so tomorrow's offline launch still enforces today's
     * source list. Returns null when neither the network nor the cache has one.
     */
    suspend fun bootstrapPayload(): AppBootstrap? {
        val call = bootstrap()
        val raw = if (call.ok) call.body?.toString() else null
        if (raw != null) SiteStorage.saveBootstrapCache(raw)
        return raw?.let { AppBootstrap.parse(it) } ?: cachedBootstrapPayload()
    }

    fun cachedBootstrapPayload(): AppBootstrap? =
        SiteStorage.loadBootstrapCache()?.let { AppBootstrap.parse(it) }

    // ---- app banner ads ---------------------------------------------------------------------

    /**
     * One banner for the in-app WebView box. `session = true` so a Premium member is recognised
     * and handed an empty list - members never see ads. The site filters to the `app-banner`
     * placement and the WebView (ad-network link) type, so whatever comes back is meant for
     * this box.
     */
    suspend fun appBannerAds(): SiteCall = get("/api/ads?placement=app-banner", session = true)

    /**
     * The in-app banner box's contents, cached on success so an offline launch still paints the
     * last banner rather than nothing. An empty list is a real answer (nothing to show), so it
     * is cached too; only a transport/HTTP failure falls back to whatever was stored.
     */
    suspend fun bannerAdsPayload(): AppBannerAds? {
        val call = appBannerAds()
        if (!call.ok) return cachedBannerAdsPayload()
        val raw = call.body?.toString() ?: return cachedBannerAdsPayload()
        SiteStorage.saveAdsCache(raw)
        return AppBannerAds.parse(raw)
    }

    fun cachedBannerAdsPayload(): AppBannerAds? =
        SiteStorage.loadAdsCache()?.let { AppBannerAds.parse(it) }

    // ---- OTA patches (config pushed without a new APK) -------------------------------------

    /** The panel's over-the-air config patches. Public and cheap, so no session is needed. */
    suspend fun otaPatches(): SiteCall = get("/api/app/ota", session = false)

    /**
     * Fresh patches, cached on success so an offline launch still applies the last-known
     * config. An empty `config` is a real answer ("no overrides"), so it is cached too; only a
     * transport/HTTP failure falls back to whatever was stored.
     */
    suspend fun otaPayload(): OtaConfig? {
        val call = otaPatches()
        if (!call.ok) return cachedOtaPayload()
        val raw = call.body?.toString() ?: return cachedOtaPayload()
        SiteStorage.saveOtaCache(raw)
        return OtaConfig.parse(raw)
    }

    fun cachedOtaPayload(): OtaConfig? =
        SiteStorage.loadOtaCache()?.let { OtaConfig.parse(it) }

    // ---- social (friends / groups / shares) ----------------------------------

    /** A member session is required for every social call; guests only browse. */
    val isSignedIn: Boolean get() = !isGuest && !sessionToken.isNullOrBlank()

    suspend fun socialState(): SiteCall = get("/api/app/social", session = true)

    suspend fun socialAddFriend(email: String, name: String): SiteCall =
        post("/api/app/social/friend", json.encodeToString(SocialFriendRequest(email, name)), session = true)

    suspend fun socialUnfriend(friendId: String): SiteCall =
        post("/api/app/social/unfriend", json.encodeToString(SocialFriendIdRequest(friendId)), session = true)

    /** `id == null` creates a group; anything else updates that group. */
    suspend fun socialSaveGroup(
        id: String?,
        name: String,
        description: String,
        memberIds: List<String>,
    ): SiteCall = post(
        "/api/app/social/group",
        json.encodeToString(SocialGroupRequest(id, name, description, memberIds)),
        session = true,
    )

    suspend fun socialDeleteGroup(id: String): SiteCall =
        post("/api/app/social/group/delete", json.encodeToString(SocialIdRequest(id)), session = true)

    suspend fun socialShare(
        recipientId: String,
        isGroup: Boolean,
        metaId: String,
        metaType: String,
        title: String,
        poster: String?,
    ): SiteCall = post(
        "/api/app/social/share",
        json.encodeToString(SocialShareRequest(recipientId, isGroup, metaId, metaType, title, poster)),
        session = true,
    )

    suspend fun socialDeleteShare(id: String): SiteCall =
        post("/api/app/social/share/delete", json.encodeToString(SocialIdRequest(id)), session = true)

    // ---- profile (display name, bio, avatar) ---------------------------------

    /** Saves the text half of my profile; the avatar picture has its own call. */
    suspend fun profileSave(displayName: String, bio: String): SiteCall =
        post("/api/account/profile", json.encodeToString(ProfileSaveRequest(displayName, bio)), session = true)

    /** Avatar by link (`https://...`); file upload lives on the site's /me page. */
    suspend fun profileAvatarUrl(url: String): SiteCall =
        post("/api/account/avatar", json.encodeToString(ProfileAvatarUrlRequest(url)), session = true)

    // ---- transport -----------------------------------------------------------------------

    private fun authHeaders(session: Boolean, appKey: Boolean): Map<String, String> {
        val headers = mutableMapOf(
            "Accept" to "application/json",
            "User-Agent" to "NuvioMobile/${AppVersionConfig.VERSION_NAME.ifBlank { "dev" }}",
        )
        if (session) SiteStorage.loadSessionToken()?.let { headers["X-Session-Token"] = it }
        if (appKey) headers["X-App-Key"] = apiKey
        return headers
    }

    private suspend fun get(path: String, session: Boolean, appKey: Boolean = false): SiteCall =
        call("GET", path, body = "", session = session, appKey = appKey)

    private suspend fun post(
        path: String,
        body: String,
        session: Boolean = false,
        appKey: Boolean = false,
    ): SiteCall = call("POST", path, body, session = session, appKey = appKey)

    private suspend fun call(
        method: String,
        path: String,
        body: String,
        session: Boolean,
        appKey: Boolean,
    ): SiteCall {
        val headers = authHeaders(session, appKey).let { base ->
            if (method == "POST") {
                base + ("Content-Type" to "application/json; charset=utf-8")
            } else {
                base
            }
        }
        val response = try {
            httpRequestRaw(
                method = method,
                url = baseUrl + path,
                headers = headers,
                body = if (method == "POST") body else "",
            )
        } catch (cancellation: CancellationException) {
            // Never swallow a cancelled scope - that is how jobs leak past their screen.
            throw cancellation
        } catch (error: Throwable) {
            return SiteCall(
                code = -1,
                body = null,
                transportError = error.message ?: error::class.simpleName ?: "network error",
            )
        }
        currentCoroutineContext().ensureActive()
        return SiteCall(
            code = response.status,
            body = runCatching { json.parseToJsonElement(response.body) as? JsonObject }.getOrNull(),
        )
    }
}

@Serializable
private data class SiteCredentials(
    val email: String,
    val password: String,
)

@Serializable
private data class SiteRegisterRequest(
    val email: String,
    val password: String,
    val marketing: Boolean,
)

@Serializable
private data class SocialFriendRequest(val email: String, val name: String)

@Serializable
private data class ProfileSaveRequest(val displayName: String, val bio: String)

@Serializable
private data class ProfileAvatarUrlRequest(val url: String)

@Serializable
private data class SocialFriendIdRequest(val friendId: String)

@Serializable
private data class SocialIdRequest(val id: String)

@Serializable
private data class SocialGroupRequest(
    val id: String? = null,
    val name: String,
    val description: String,
    val memberIds: List<String>,
)

@Serializable
private data class SocialShareRequest(
    val recipientId: String,
    val isGroup: Boolean,
    val metaId: String,
    val metaType: String,
    val title: String,
    val poster: String? = null,
)

private fun JsonObject.stringOrNull(key: String): String? = this[key]?.let { element ->
    runCatching { element.jsonPrimitive.contentOrNull }.getOrNull()
}
