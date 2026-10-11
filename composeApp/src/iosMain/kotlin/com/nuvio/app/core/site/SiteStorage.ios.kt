package com.nuvio.app.core.site

import platform.Foundation.NSUserDefaults

internal actual object SiteStorage {
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

    actual fun loadSessionToken(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_SESSION_TOKEN)?.takeIf { it.isNotBlank() }

    actual fun loadSessionEmail(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_SESSION_EMAIL)?.takeIf { it.isNotBlank() }

    actual fun saveSession(token: String, email: String) {
        NSUserDefaults.standardUserDefaults.apply {
            setObject(token, forKey = KEY_SESSION_TOKEN)
            setObject(email, forKey = KEY_SESSION_EMAIL)
        }
    }

    actual fun clearSession() {
        NSUserDefaults.standardUserDefaults.apply {
            removeObjectForKey(KEY_SESSION_TOKEN)
            removeObjectForKey(KEY_SESSION_EMAIL)
            removeObjectForKey(KEY_GUEST)
        }
    }

    actual fun loadGuest(): Boolean =
        NSUserDefaults.standardUserDefaults.boolForKey(KEY_GUEST)

    actual fun saveGuest(value: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(value, forKey = KEY_GUEST)
    }

    actual fun loadBaseUrlOverride(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_BASE_URL)?.takeIf { it.isNotBlank() }

    actual fun saveBaseUrlOverride(value: String?) {
        if (value == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY_BASE_URL)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(value, forKey = KEY_BASE_URL)
        }
    }

    actual fun loadApiKeyOverride(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_API_KEY)?.takeIf { it.isNotBlank() }

    actual fun saveApiKeyOverride(value: String?) {
        if (value == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY_API_KEY)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(value, forKey = KEY_API_KEY)
        }
    }

    actual fun loadBootstrapCache(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_BOOTSTRAP_CACHE)?.takeIf { it.isNotBlank() }

    actual fun saveBootstrapCache(raw: String) {
        NSUserDefaults.standardUserDefaults.setObject(raw, forKey = KEY_BOOTSTRAP_CACHE)
    }

    actual fun loadSocialCache(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_SOCIAL_CACHE)?.takeIf { it.isNotBlank() }

    actual fun saveSocialCache(raw: String) {
        NSUserDefaults.standardUserDefaults.setObject(raw, forKey = KEY_SOCIAL_CACHE)
    }

    actual fun loadHomeCatalogCache(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_HOME_CATALOG_CACHE)?.takeIf { it.isNotBlank() }

    actual fun saveHomeCatalogCache(raw: String) {
        NSUserDefaults.standardUserDefaults.setObject(raw, forKey = KEY_HOME_CATALOG_CACHE)
    }

    actual fun loadAdsCache(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_ADS_CACHE)?.takeIf { it.isNotBlank() }

    actual fun saveAdsCache(raw: String) {
        NSUserDefaults.standardUserDefaults.setObject(raw, forKey = KEY_ADS_CACHE)
    }

    actual fun loadOtaCache(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_OTA_CACHE)?.takeIf { it.isNotBlank() }

    actual fun saveOtaCache(raw: String) {
        NSUserDefaults.standardUserDefaults.setObject(raw, forKey = KEY_OTA_CACHE)
    }
}
