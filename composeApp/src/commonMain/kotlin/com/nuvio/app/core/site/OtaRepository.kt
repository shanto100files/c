package com.nuvio.app.core.site

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the panel's over-the-air config patches, merged into one [OtaConfig].
 *
 * Fetched once at startup (next to the bootstrap call) and cached for offline launches. It is
 * deliberately decoupled from anything that gates the app: a failed or slow fetch only leaves
 * the overrides empty, so the app simply keeps its built-in defaults. It never blocks startup
 * and never throws into the UI.
 *
 * Feature code reads an override through [string]/[bool]/[int] on the current [state] value -
 * a key the panel never set falls straight through to the app's own default, so adding a patch
 * is purely additive and removing one restores the built-in behaviour.
 */
object OtaRepository {
    private val _state = MutableStateFlow(OtaConfig.EMPTY)
    val state: StateFlow<OtaConfig> = _state.asStateFlow()

    init {
        // Paint immediately from cache while a fresh fetch runs in the background.
        SiteApi.cachedOtaPayload()?.let { cached -> _state.value = cached }
    }

    /** Fetch fresh patches. Never throws; a failure keeps whatever is already applied. */
    suspend fun refresh() {
        val fresh = SiteApi.otaPayload() ?: return
        // Skip the state write when nothing moved, so collectors are not woken needlessly.
        if (fresh.rev == _state.value.rev && fresh.config == _state.value.config) return
        _state.value = fresh
    }

    /** Explicit re-read from cache, e.g. after a profile or site-URL switch. */
    suspend fun reloadFromCache() {
        _state.value = SiteApi.cachedOtaPayload() ?: OtaConfig.EMPTY
    }

    /** The config currently in effect. Safe to call from anywhere, including composition. */
    val current: OtaConfig get() = _state.value

    /** A string override, or [fallback] when the panel never set this key. */
    fun string(key: String, fallback: String): String = current.string(key) ?: fallback

    /** A boolean override, or [fallback] when the panel never set this key. */
    fun bool(key: String, fallback: Boolean): Boolean = current.bool(key) ?: fallback

    /** An integer override, or [fallback] when the panel never set this key. */
    fun int(key: String, fallback: Int): Int = current.int(key) ?: fallback

    /** A boolean override in a nested group (e.g. `features` / `p2pEnabled`), or [fallback]. */
    fun boolAt(group: String, key: String, fallback: Boolean): Boolean =
        current.boolAt(group, key) ?: fallback

    /** A string override in a nested group, or [fallback]. */
    fun stringAt(group: String, key: String, fallback: String): String =
        current.stringAt(group, key) ?: fallback

    /** An integer override in a nested group, or [fallback]. */
    fun intAt(group: String, key: String, fallback: Int): Int =
        current.intAt(group, key) ?: fallback
}
