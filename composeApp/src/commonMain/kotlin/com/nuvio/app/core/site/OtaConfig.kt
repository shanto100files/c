package com.nuvio.app.core.site

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * One entry in the panel's OTA changelog: what the operator shipped, most recent first.
 */
data class OtaChangelogEntry(
    val id: String,
    val label: String,
    val createdAtEpochMs: Long,
)

/**
 * `/api/app/ota` reduced to what the app acts on.
 *
 * The panel lets the operator push config changes to already-installed apps without shipping a
 * new APK. Each patch is a small JSON object the app merges over its own defaults - text, a
 * colour, a feature flag, a layout switch, a banner. [rev] only moves when the effective merged
 * config changes, so the app can skip re-applying when nothing moved.
 *
 * The [config] is kept as a raw [JsonObject]: the keys the app understands are read by name via
 * [string]/[bool]/[int], and anything else is ignored. That way the panel can add a key for a
 * future app version before that version exists, and older builds simply skip it.
 */
data class OtaConfig(
    val rev: String,
    val config: JsonObject,
    val changelog: List<OtaChangelogEntry>,
) {
    /** A string override, or null when the panel never set this key. */
    fun string(key: String): String? = config.stringOrNull(key)

    /** A boolean override, or null when the panel never set this key. */
    fun bool(key: String): Boolean? = config.boolOrNull(key)

    /** An integer override, or null when the panel never set this key. */
    fun int(key: String): Int? = config.intOrNull(key)

    /**
     * A boolean override inside a nested group (e.g. `features.p2pEnabled`), or null when the
     * panel never set it. Groups keep related keys together in the panel's payload editor.
     */
    fun boolAt(group: String, key: String): Boolean? = config.child(group)?.boolOrNull(key)

    /** A string override inside a nested group, or null when the panel never set it. */
    fun stringAt(group: String, key: String): String? = config.child(group)?.stringOrNull(key)

    /** An integer override inside a nested group, or null when the panel never set it. */
    fun intAt(group: String, key: String): Int? = config.child(group)?.intOrNull(key)

    companion object {
        /** A neutral identity so callers never have to null-check before the first fetch. */
        val EMPTY = OtaConfig(rev = "", config = JsonObject(emptyMap()), changelog = emptyList())

        /**
         * Lenient on purpose: the wire shape is the panel's to change, and a field in an
         * unexpected shape must never turn "the config could not be read" into a launch crash.
         */
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        fun parse(raw: String): OtaConfig? = runCatching {
            val root = json.parseToJsonElement(raw).jsonObject
            OtaConfig(
                rev = root["rev"]?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }.orEmpty(),
                config = root["config"] as? JsonObject ?: JsonObject(emptyMap()),
                changelog = runCatching {
                    (root["changelog"] as? JsonArray).orEmpty().mapNotNull { entry ->
                        val obj = entry as? JsonObject ?: return@mapNotNull null
                        OtaChangelogEntry(
                            id = obj.stringOrNull("id").orEmpty(),
                            label = obj.stringOrNull("label").orEmpty(),
                            createdAtEpochMs = obj.stringOrNull("createdAtEpochMs")?.toLongOrNull() ?: 0L,
                        )
                    }
                }.getOrDefault(emptyList()),
            )
        }.getOrNull()
    }
}

/** Lenient string read: any non-primitive value is treated as "not set" rather than failing. */
private fun JsonObject.stringOrNull(key: String): String? =
    runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull()?.takeIf { it.isNotBlank() }

private fun JsonObject.boolOrNull(key: String): Boolean? =
    runCatching { this[key]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() }.getOrNull()

private fun JsonObject.intOrNull(key: String): Int? =
    runCatching { this[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull() }.getOrNull()

private fun JsonObject.child(group: String): JsonObject? = this[group] as? JsonObject
