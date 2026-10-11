package com.nuvio.app.core.site

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the one in-app banner ad the Home-screen WebView box shows.
 *
 * The payload is public and cheap, so it is fetched once at startup and cached for offline
 * launches. It is deliberately decoupled from anything that gates the app: a failed or slow
 * fetch only leaves the box empty, it never blocks startup or throws into the UI.
 */
object AppBannerAdsRepository {
    private val _state = MutableStateFlow(AppBannerAds())
    val state: StateFlow<AppBannerAds> = _state.asStateFlow()

    init {
        // Paint immediately from cache while a fresh fetch runs in the background.
        SiteApi.cachedBannerAdsPayload()?.let { cached ->
            if (cached.ad != null) _state.value = cached
        }
    }

    /** Fetch fresh contents. Never throws; a failure keeps whatever is already showing. */
    suspend fun refresh() {
        val fresh = SiteApi.bannerAdsPayload() ?: return
        _state.value = fresh
    }

    /** Explicit re-read, e.g. after a login/logout changes who is asking. */
    suspend fun reloadFromCache() {
        _state.value = SiteApi.cachedBannerAdsPayload() ?: AppBannerAds()
    }
}
