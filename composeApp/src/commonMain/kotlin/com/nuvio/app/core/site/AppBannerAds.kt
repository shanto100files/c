package com.nuvio.app.core.site

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One in-app banner slot fed by the panel's `app-banner` ads.
 *
 * The website never renders this placement; the app opens [link] inside a small WebView box
 * pinned at the bottom of the Home screen. The panel's operator pastes an ad-network direct
 * link (e.g. Adsterra) and the phone shows whatever that link serves.
 */
data class AppBannerAd(
    val id: String,
    val link: String,
    val heightDp: Int,
) {
    val isValid: Boolean get() = id.isNotBlank() && link.isNotBlank()
}

/**
 * `/api/ads?placement=app-banner` reduced to the single banner the box will show.
 *
 * The server already filters by placement and by Premium status, so whatever arrives here is
 * meant for this device. Several configured ads are collapsed to one deterministic pick (the
 * lowest id) so every device in a session shows the same banner rather than flickering.
 */
data class AppBannerAds(
    val ad: AppBannerAd? = null,
) {
    companion object {
        const val CACHE_KEY = "app_banner_ads"

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        fun parse(raw: String): AppBannerAds? = runCatching {
            val wire = json.decodeFromString<AppBannerAdsWire>(raw)
            val ads = wire.ads.orEmpty()
                .filter { it.link.isNullOrBlank().not() }
                .mapNotNull { item ->
                    val link = item.link.orEmpty().trim()
                    val id = item.id.orEmpty().trim().ifBlank { link }
                    if (id.isBlank() || link.isBlank()) return@mapNotNull null
                    AppBannerAd(
                        id = id,
                        link = link,
                        heightDp = (item.heightDp ?: DEFAULT_HEIGHT_DP)
                            .coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP),
                    )
                }
            // Deterministic pick: first by id so a device keeps one banner per launch.
            AppBannerAds(ad = ads.minByOrNull { it.id })
        }.getOrNull()

        const val DEFAULT_HEIGHT_DP = 120
        const val MIN_HEIGHT_DP = 40
        const val MAX_HEIGHT_DP = 400
    }
}

/** The raw payload. Every field optional so a missing key means "not set", never "malformed". */
@Serializable
private data class AppBannerAdsWire(
    val ads: List<AppBannerAdWire>? = null,
)

@Serializable
private data class AppBannerAdWire(
    val id: String? = null,
    val type: String? = null,
    val link: String? = null,
    val heightDp: Int? = null,
)
