package com.nuvio.app.core.site

import android.content.Context
import android.content.SharedPreferences

internal actual object SiteStorage {
    /**
     * Its own preferences file, and deliberately named the same way as the panel's other app, so
     * logging out clears the session without ever touching the base URL or the API key - which
     * would otherwise turn a logout into "the app can no longer reach its own server".
     */
    private const val PREFS_NAME = "site_api"

    private const val KEY_SESSION_TOKEN = "x_session_token"
    private const val KEY_SESSION_EMAIL = "x_session_email"
    private const val KEY_GUEST = "guest"
    private const val KEY_BASE_URL = "admin_server_url"
    private const val KEY_API_KEY = "site_api_key"
    private const val KEY_BOOTSTRAP_CACHE = "bootstrap_cache"
    private const val KEY_SOCIAL_CACHE = "social_cache"
    private const val KEY_HOME_CATALOG_CACHE = "home_catalog_cache"
    private const val KEY_ADS_CACHE = "ads_cache"
    private const val KEY_OTA_CACHE = "ota_cache"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadSessionToken(): String? =
        preferences?.getString(KEY_SESSION_TOKEN, null)?.takeIf { it.isNotBlank() }

    actual fun loadSessionEmail(): String? =
        preferences?.getString(KEY_SESSION_EMAIL, null)?.takeIf { it.isNotBlank() }

    actual fun saveSession(token: String, email: String) {
        preferences?.edit()
            ?.putString(KEY_SESSION_TOKEN, token)
            ?.putString(KEY_SESSION_EMAIL, email)
            ?.apply()
    }

    actual fun clearSession() {
        preferences?.edit()
            ?.remove(KEY_SESSION_TOKEN)
            ?.remove(KEY_SESSION_EMAIL)
            ?.remove(KEY_GUEST)
            ?.apply()
    }

    actual fun loadGuest(): Boolean = preferences?.getBoolean(KEY_GUEST, false) == true

    actual fun saveGuest(value: Boolean) {
        preferences?.edit()?.putBoolean(KEY_GUEST, value)?.apply()
    }

    actual fun loadBaseUrlOverride(): String? =
        preferences?.getString(KEY_BASE_URL, null)?.takeIf { it.isNotBlank() }

    actual fun saveBaseUrlOverride(value: String?) {
        val editor = preferences?.edit() ?: return
        if (value == null) editor.remove(KEY_BASE_URL) else editor.putString(KEY_BASE_URL, value)
        editor.apply()
    }

    actual fun loadApiKeyOverride(): String? =
        preferences?.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }

    actual fun saveApiKeyOverride(value: String?) {
        val editor = preferences?.edit() ?: return
        if (value == null) editor.remove(KEY_API_KEY) else editor.putString(KEY_API_KEY, value)
        editor.apply()
    }

    actual fun loadBootstrapCache(): String? =
        preferences?.getString(KEY_BOOTSTRAP_CACHE, null)?.takeIf { it.isNotBlank() }

    actual fun saveBootstrapCache(raw: String) {
        preferences?.edit()?.putString(KEY_BOOTSTRAP_CACHE, raw)?.apply()
    }

    actual fun loadSocialCache(): String? =
        preferences?.getString(KEY_SOCIAL_CACHE, null)?.takeIf { it.isNotBlank() }

    actual fun saveSocialCache(raw: String) {
        preferences?.edit()?.putString(KEY_SOCIAL_CACHE, raw)?.apply()
    }

    actual fun loadHomeCatalogCache(): String? =
        preferences?.getString(KEY_HOME_CATALOG_CACHE, null)?.takeIf { it.isNotBlank() }

    actual fun saveHomeCatalogCache(raw: String) {
        preferences?.edit()?.putString(KEY_HOME_CATALOG_CACHE, raw)?.apply()
    }

    actual fun loadAdsCache(): String? =
        preferences?.getString(KEY_ADS_CACHE, null)?.takeIf { it.isNotBlank() }

    actual fun saveAdsCache(raw: String) {
        preferences?.edit()?.putString(KEY_ADS_CACHE, raw)?.apply()
    }

    actual fun loadOtaCache(): String? =
        preferences?.getString(KEY_OTA_CACHE, null)?.takeIf { it.isNotBlank() }

    actual fun saveOtaCache(raw: String) {
        preferences?.edit()?.putString(KEY_OTA_CACHE, raw)?.apply()
    }
}
