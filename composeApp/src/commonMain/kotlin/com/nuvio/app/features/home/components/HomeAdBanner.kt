package com.nuvio.app.features.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.site.AppBannerAd
import com.nuvio.app.core.ui.NuvioSurfaceCard
import com.nuvio.app.features.ads.AdBannerWebView

/**
 * The bottom-of-Home ad box. It hosts one ad-network link inside a native WebView sized to
 * [AppBannerAd.heightDp].
 *
 * It is deliberately the very last row of the Home list: a member scrolls past real content to
 * reach it, so the ad never interrupts browsing and nothing pops over the poster grid. The
 * whole card is a thin frame around the WebView - the ad supplies its own artwork.
 */
@Composable
fun HomeAdBanner(
    ad: AppBannerAd,
    modifier: Modifier = Modifier,
) {
    NuvioSurfaceCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Sponsored",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp),
            )
            AdBannerWebView(
                url = ad.link,
                heightDp = ad.heightDp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ad.heightDp.dp),
            )
        }
    }
}
