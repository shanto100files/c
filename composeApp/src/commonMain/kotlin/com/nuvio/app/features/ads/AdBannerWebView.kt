package com.nuvio.app.features.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A banner-sized box that loads one ad-network URL inside a native WebView.
 *
 * Ad networks (Adsterra and friends) hand the operator a single direct link; the box simply
 * renders whatever that link serves. Android uses a real WebView. iOS renders nothing for now -
 * the box is only wired up for Android, and an untestable WKWebView embed is worse than a
 * clean no-op.
 */
@Composable
internal expect fun AdBannerWebView(
    url: String,
    heightDp: Int,
    modifier: Modifier = Modifier,
)
