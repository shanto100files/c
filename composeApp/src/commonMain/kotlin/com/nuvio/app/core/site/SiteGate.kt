package com.nuvio.app.core.site

import kotlinx.coroutines.withTimeoutOrNull

/**
 * Whether the app is allowed to show anything at all.
 *
 * Named [SiteGate] rather than `AppGate` because this module already has an `AppGate` composable
 * (`com.nuvio.app.AppGate`) that decides something else entirely - startup, profile and auth.
 * This one is only about the backend's verdict on this build.
 *
 * Decided before the first screen renders: the site is asked to verify the key baked into this
 * build, and a 403 is the only answer that shuts the door. No answer (offline, DNS, timeout)
 * leaves it [Open] - an unreachable server must never turn a phone into a brick.
 */
sealed interface SiteGate {
    /** Still asking. Shows a splash, not a rejection. */
    data object Checking : SiteGate

    data object Open : SiteGate

    data class Blocked(val reason: String) : SiteGate
}

/**
 * Asks the site for a verdict and turns the answer into a [SiteGate].
 *
 * Only an explicit 403 closes the door. Everything else - no network, a DNS failure, a 5xx, even
 * a missing endpoint - is an absence of an answer rather than a "no", and an app that refuses to
 * start because it cannot reach its own server is worse than one that starts.
 */
/**
 * Asks the site for a verdict and turns the answer into a [SiteGate].
 *
 * Only an explicit 403 closes the door. Everything else - no network, a DNS failure, a 5xx, even
 * a missing endpoint - is an absence of an answer rather than a "no". The timeout is what keeps
 * that promise practical: without it a device on a dead wifi would sit on the spinner for as long
 * as OkHttp waits, which is indistinguishable from a hang.
 */
suspend fun SiteApi.gate(): SiteGate {
    val call = withTimeoutOrNull(GATE_TIMEOUT_MS) { verifyKey() } ?: return SiteGate.Open
    return if (call.code == 403) {
        SiteGate.Blocked(call.errorText("This build is no longer authorised for this site."))
    } else {
        SiteGate.Open
    }
}

private const val GATE_TIMEOUT_MS = 5_000L
