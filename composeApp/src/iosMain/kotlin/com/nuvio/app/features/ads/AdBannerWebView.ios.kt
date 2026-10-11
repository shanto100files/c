package com.nuvio.app.features.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * iOS: the ad box is Android-only for now, so it renders nothing.
 *
 * A WKWebView embed is deliberately not wired up yet - it needs its own testing pass and an
 * untestable embed is worse than a clean no-op. The box simply takes up no visible space.
 */
@Composable
internal actual fun AdBannerWebView(
    url: String,
    heightDp: Int,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(0.dp),
    )
}
