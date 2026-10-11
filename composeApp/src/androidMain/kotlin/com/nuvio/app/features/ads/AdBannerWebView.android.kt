package com.nuvio.app.features.ads

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Android: a real WebView sized to the banner box.
 *
 * JavaScript and DOM storage are on because ad-network direct links are JS-driven. Navigation is
 * pinned to stay inside the box - the link's whole point is to render the ad right here, and an
 * interstitial that hands off to the system browser would break the banner and yank the user
 * out of the app.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal actual fun AdBannerWebView(
    url: String,
    heightDp: Int,
    modifier: Modifier,
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                // Keeps the user inside the box; nothing hands off to the browser.
                webViewClient = WebViewClient()
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                loadUrl(url)
            }
        },
        update = { webView ->
            // Only reload when the operator swaps in a different link.
            if (webView.url != url) webView.loadUrl(url)
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
        },
    )
}
