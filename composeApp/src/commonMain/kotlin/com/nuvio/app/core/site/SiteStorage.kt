package com.nuvio.app.core.site

/**
 * Where the site session and the panel overrides live between launches.
 *
 * Carries the session token, the email it belongs to, the guest flag, an overridable base URL
 * and API key, plus the last bootstrap payload - so an offline launch still enforces today's
 * allow-list. Split per platform the way this app stores everything else: SharedPreferences on
 * Android, NSUserDefaults on iOS.
 *
 * Only Android exposes `initialize(context)`; iOS reaches NSUserDefaults directly and needs no
 * setup. Every getter is null-safe so a caller that ran before initialization degrades to the
 * default instead of crashing.
 */
internal expect object SiteStorage {
    fun loadSessionToken(): String?
    fun loadSessionEmail(): String?
    fun saveSession(token: String, email: String)

    /** Drops the session only - the base URL and API key deliberately survive a logout. */
    fun clearSession()

    fun loadGuest(): Boolean
    fun saveGuest(value: Boolean)

    fun loadBaseUrlOverride(): String?
    fun saveBaseUrlOverride(value: String?)

    fun loadApiKeyOverride(): String?
    fun saveApiKeyOverride(value: String?)

    fun loadBootstrapCache(): String?
    fun saveBootstrapCache(raw: String)

    /** Cached social graph (friends / groups / feeds) so an offline launch still shows them. */
    fun loadSocialCache(): String?
    fun saveSocialCache(raw: String)

    /** Last-known home rows so a cold start paints instantly while fresh data downloads. */
    fun loadHomeCatalogCache(): String?
    fun saveHomeCatalogCache(raw: String)

    /** Last app-banner ad so a cold start still paints the box before a fresh fetch lands. */
    fun loadAdsCache(): String?
    fun saveAdsCache(raw: String)

    /** Last OTA patch payload so an offline launch still applies today's config overrides. */
    fun loadOtaCache(): String?
    fun saveOtaCache(raw: String)
}
