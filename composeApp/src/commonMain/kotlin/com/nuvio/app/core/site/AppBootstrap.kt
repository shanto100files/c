package com.nuvio.app.core.site

import com.nuvio.app.core.build.AppVersionConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One source the site offers to the app. Everything else must not be installed. */
@Serializable
data class BootstrapAddon(
    val id: String = "",
    val name: String = "",
    val transportUrl: String = "",
    val version: String? = null,
)

/** `config.killSwitch` - the site's ability to stop a build outright. */
data class KillSwitchConfig(
    val enabled: Boolean = false,
    /** Empty means "every version". */
    val blockedVersions: List<Int> = emptyList(),
    val message: String = "",
) {
    fun blocks(versionCode: Int): Boolean =
        enabled && (blockedVersions.isEmpty() || versionCode in blockedVersions)
}

/** `config.appUpdate` - the panel's only way to push a build to installed apps. */
data class SiteUpdate(
    val versionName: String,
    val versionCode: Int,
    val apkUrl: String,
    val sha256Url: String?,
    val releaseNotes: String,
    /** Panel's "Force update" toggle - when on, a newer build must replace this one. */
    val forceUpdate: Boolean = false,
) {
    /** Blank apkUrl means the operator has not configured a push, so there is nothing to offer. */
    val isPublished: Boolean get() = apkUrl.isNotBlank()

    val isNewerThanInstalled: Boolean
        get() = isPublished && versionCode > AppVersionConfig.VERSION_CODE

    /**
     * Force-update is only meaningful once there is actually a newer build to move to: the panel
     * toggling it on without publishing a higher versionCode must not lock the installed app.
     */
    val isForceUpdateRequired: Boolean
        get() = forceUpdate && isNewerThanInstalled
}

/**
 * `/api/app/bootstrap` reduced to what the app actually acts on.
 *
 * [addons] is the allow-list: install exactly these, uninstall anything else the host will let
 * go of, and show nothing but these in the UI.
 */
data class AppBootstrap(
    val rev: Long,
    val siteUrl: String,
    val addons: List<BootstrapAddon>,
    /** Turned off or removed by the operator - never installed, and stripped if present. */
    val blockedUrls: Set<String>,
    val killSwitch: KillSwitchConfig,
    val maintenance: Boolean,
    val maintenanceMessage: String,
    val announcementEnabled: Boolean,
    val announcementMessage: String,
    val announcementLink: String,
    /** Null when the panel has never filled in the App update form. */
    val siteUpdate: SiteUpdate? = null,
) {
    /** Normalised so `.../manifest.json` compares equal however the site spelled it. */
    val allowedUrls: Set<String>
        get() = addons.map { normalizeAddonUrl(it.transportUrl) }.toSet()

    companion object {
        /**
         * Lenient on purpose. The wire shape is the panel's to change, and a field that arrives
         * in an unexpected shape must never turn "the allow-list could not be read" into a crash
         * on the launch screen.
         */
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        fun parse(raw: String): AppBootstrap? = runCatching {
            fromWire(json.decodeFromString<SiteBootstrapWire>(raw))
        }.getOrNull()

        private fun fromWire(wire: SiteBootstrapWire): AppBootstrap {
            val addons = wire.addons
                .orEmpty()
                .filter { it.transportUrl.isNotBlank() }
                .map { addon ->
                    addon.copy(
                        transportUrl = addon.transportUrl.trim(),
                        name = addon.name.ifBlank { addon.id },
                    )
                }

            // Two spellings of the panel's "off" list, both normalised so that
            // `.../manifest.json` is never counted as two different sources.
            val blocked = mutableSetOf<String>()
            listOf(wire.offAddons, wire.removedAddons).forEach { entries ->
                entries.orEmpty().forEach { entry ->
                    normalizeAddonUrl(entry).takeIf { it.isNotBlank() }?.let(blocked::add)
                }
            }

            val branding = wire.branding
            val announcement = wire.announcement

            return AppBootstrap(
                rev = wire.rev ?: 0L,
                siteUrl = wire.siteUrl.orEmpty(),
                addons = addons,
                blockedUrls = blocked,
                killSwitch = KillSwitchConfig(
                    enabled = wire.killSwitch?.enabled == true,
                    blockedVersions = wire.killSwitch?.blockedVersions.orEmpty(),
                    message = wire.killSwitch?.message.orEmpty(),
                ),
                maintenance = wire.maintenance == true || branding?.maintenanceEnabled == true,
                maintenanceMessage = wire.maintenanceMessage
                    .orEmpty()
                    .ifBlank { branding?.maintenanceMessage.orEmpty() },
                announcementEnabled = announcement?.enabled == true,
                announcementMessage = announcement?.message.orEmpty(),
                announcementLink = announcement?.link.orEmpty(),
                siteUpdate = wire.appUpdate
                    ?.takeIf { !it.apkUrl.isNullOrBlank() }
                    ?.let { update ->
                        SiteUpdate(
                            versionName = update.versionName.orEmpty().trim(),
                            versionCode = update.versionCode ?: 0,
                            apkUrl = update.apkUrl.orEmpty().trim(),
                            sha256Url = update.sha256Url.orEmpty().trim().takeIf { it.isNotBlank() },
                            releaseNotes = update.releaseNotes.orEmpty(),
                            forceUpdate = update.forceUpdate == true,
                        )
                    },
            )
        }
    }
}

/**
 * The raw payload. Every field is optional because `org.json`'s `opt*` semantics are what the
 * original parse relied on: a missing or null key meant "not set", never "malformed".
 */
@Serializable
private data class SiteBootstrapWire(
    val rev: Long? = null,
    val siteUrl: String? = null,
    val addons: List<BootstrapAddon>? = null,
    val offAddons: List<String>? = null,
    val removedAddons: List<String>? = null,
    val killSwitch: SiteKillSwitchWire? = null,
    val maintenance: Boolean? = null,
    val maintenanceMessage: String? = null,
    val branding: SiteBrandingWire? = null,
    val announcement: SiteAnnouncementWire? = null,
    val appUpdate: SiteAppUpdateWire? = null,
)

@Serializable
private data class SiteKillSwitchWire(
    val enabled: Boolean? = null,
    val blockedVersions: List<Int>? = null,
    val message: String? = null,
)

@Serializable
private data class SiteBrandingWire(
    val maintenanceEnabled: Boolean? = null,
    val maintenanceMessage: String? = null,
)

@Serializable
private data class SiteAnnouncementWire(
    val enabled: Boolean? = null,
    val message: String? = null,
    val link: String? = null,
)

@Serializable
private data class SiteAppUpdateWire(
    val versionName: String? = null,
    val versionCode: Int? = null,
    val apkUrl: String? = null,
    val sha256Url: String? = null,
    val releaseNotes: String? = null,
    val forceUpdate: Boolean? = null,
)

/**
 * Two spellings of the same manifest URL must not read as two different sources, otherwise a
 * sync would install a copy and immediately uninstall it again on the next launch.
 */
fun normalizeAddonUrl(url: String): String =
    url.trim().removeSuffix("/").lowercase()
