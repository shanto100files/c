package com.nuvio.app.core.site

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.AddAddonResult
import com.nuvio.app.features.addons.AddonRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What one pass of [SiteAddonSync.enforce] actually did. */
sealed interface SiteSyncOutcome {
    /**
     * No payload from the network and nothing cached. The policy is unknown, so every installed
     * source was deliberately left alone - offline must never read as "nothing is allowed".
     */
    data object Unavailable : SiteSyncOutcome

    /**
     * The panel offered no source at all. Treated as misconfiguration rather than permission to
     * wipe, because an empty allow-list leaves the user with no way back in.
     */
    data object EmptyAllowList : SiteSyncOutcome

    data class Applied(
        val installed: Int,
        val removed: Int,
        val failed: Int,
    ) : SiteSyncOutcome
}

/**
 * Makes the installed source set match the panel's allow-list exactly.
 *
 * The list is the site's alone: `/api/app/bootstrap` says what may be installed, and anything
 * else - whether the user added it by hand or a previous sync did - goes. Nuvio's own Supabase
 * addon pull is bypassed for exactly this reason, so that two servers never fight over one list.
 *
 * Two rules keep a bad day from becoming a bricked app:
 *
 *  - it never uninstalls on an [SiteSyncOutcome.Unavailable] answer, only on a payload it has;
 *  - it installs before it removes, so a manifest that fails to fetch costs nothing.
 */
object SiteAddonSync {
    private val log = Logger.withTag("SiteAddonSync")
    private val mutex = Mutex()

    suspend fun enforce(): SiteSyncOutcome = mutex.withLock { enforceLocked() }

    private suspend fun enforceLocked(): SiteSyncOutcome {
        val bootstrap = SiteApi.bootstrapPayload()
        if (bootstrap == null) {
            log.i { "enforce(): no payload (offline, no cache) - leaving installed sources alone" }
            return SiteSyncOutcome.Unavailable
        }

        // normalised key -> the URL as the panel spelled it, which is what gets installed
        val allowed = LinkedHashMap<String, String>()
        bootstrap.addons.forEach { addon ->
            val key = manifestKey(addon.transportUrl)
            if (key.isBlank()) return@forEach
            // An explicit "off" beats an offer: the panel may list a source it has since disabled.
            if (key in bootstrap.blockedUrls) return@forEach
            if (key !in allowed) allowed[key] = addon.transportUrl
        }

        if (allowed.isEmpty()) {
            log.e { "enforce(): allow-list is empty (rev=${bootstrap.rev}) - refusing to uninstall anything" }
            return SiteSyncOutcome.EmptyAllowList
        }

        // Pulls what is on disk into state first, so "installed" is never read as an empty list
        // just because nothing has populated it yet. A second call is a no-op.
        AddonRepository.initialize()

        val installedKeys = AddonRepository.uiState.value.addons
            .map { manifestKey(it.manifestUrl) }
            .toSet()

        // 1. Install what is missing first.
        var installed = 0
        var failed = 0
        allowed.forEach { (key, url) ->
            if (key in installedKeys) return@forEach
            when (AddonRepository.addAddon(url)) {
                is AddAddonResult.Success -> {
                    installed++
                    log.i { "enforce(): installed $url" }
                }

                is AddAddonResult.Error -> {
                    failed++
                    log.e { "enforce(): could not install $url" }
                }
            }
        }

        // 2. Only now drop whatever the panel does not offer.
        var removed = 0
        AddonRepository.uiState.value.addons
            .map { it.manifestUrl }
            .distinct()
            .forEach { manifestUrl ->
                if (manifestKey(manifestUrl) in allowed) return@forEach
                AddonRepository.removeAddon(manifestUrl)
                removed++
                log.i { "enforce(): removed $manifestUrl" }
            }

        log.i {
            "enforce(): rev=${bootstrap.rev} allowed=${allowed.size} " +
                "installed=$installed removed=$removed failed=$failed"
        }
        return SiteSyncOutcome.Applied(installed = installed, removed = removed, failed = failed)
    }

    /**
     * The comparison key for one manifest URL.
     *
     * Nuvio stores sources with Nuvio's own spelling (`.../manifest.json`, scheme forced, no
     * fragment) while the panel writes the URL as it did. Comparing the raw strings would report
     * the same source as two, and a single one of those would uninstall everything the app has,
     * so both sides go through this first.
     */
    private fun manifestKey(url: String): String {
        val withoutFragment = url.trim().substringBefore("#")
        val query = withoutFragment.substringAfter("?", "")
        val path = withoutFragment.substringBefore("?").trimEnd('/')
        if (path.isEmpty()) return ""
        val withJson = if (path.endsWith("/manifest.json")) path else "$path/manifest.json"
        return normalizeAddonUrl(if (query.isEmpty()) withJson else "$withJson?$query")
    }
}
