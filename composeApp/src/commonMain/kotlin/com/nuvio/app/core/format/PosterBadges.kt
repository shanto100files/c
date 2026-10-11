package com.nuvio.app.core.format

/**
 * Poster-card badge texts (see NuvioPosterBadges): a season range for series
 * and a language label, so a card says what is inside before it is opened.
 */

/** "S1" / "S1-S4"; null for movies or when the season count is unknown. */
fun formatSeasonBadge(type: String, seasonCount: Int?): String? {
    if (!type.equals("series", ignoreCase = true)) return null
    val count = seasonCount ?: return null
    if (count <= 0) return null
    return if (count == 1) "S1" else "S1-S$count"
}

/** ISO-639-1 → readable label ("bn" → "Bangla"); unknown codes stay uppercase. */
private val LANGUAGE_NAMES = mapOf(
    "bn" to "Bangla",
    "hi" to "Hindi",
    "en" to "English",
    "ur" to "Urdu",
    "ta" to "Tamil",
    "te" to "Telugu",
    "ml" to "Malayalam",
    "kn" to "Kannada",
    "mr" to "Marathi",
    "pa" to "Punjabi",
    "ne" to "Nepali",
    "si" to "Sinhala",
    "ja" to "Japanese",
    "ko" to "Korean",
    "zh" to "Chinese",
    "cn" to "Chinese",
    "ru" to "Russian",
    "fr" to "French",
    "es" to "Spanish",
    "pt" to "Portuguese",
    "de" to "German",
    "it" to "Italian",
    "nl" to "Dutch",
    "tr" to "Turkish",
    "ar" to "Arabic",
    "fa" to "Persian",
    "th" to "Thai",
    "vi" to "Vietnamese",
    "id" to "Indonesian",
    "ms" to "Malay",
    "fil" to "Filipino",
)

fun formatLanguageBadge(language: String?): String? {
    val raw = language?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
    val code = raw.substringBefore('-')
    return LANGUAGE_NAMES[code]
        ?: LANGUAGE_NAMES[raw]
        ?: code.uppercase().take(3)
}

private val SeasonPattern = Regex("""(?i)\b(?:season|s)\s*(\d+(?:\s*-\s*\d+)?)\b""")
private val EpisodeRangePattern = Regex("""(?i)\b(?:episodes?|eps?|ch|chapter|ep)\s*(\d+(?:\s*-\s*\d+)?)\b""")
private val CompletePattern = Regex("""(?i)\b(complete|completed)\b""")
private val LanguageKeywords = mapOf(
    "bengali" to "Bengali",
    "bangla" to "Bangla",
    "hindi" to "Hindi",
    "english" to "English",
    "tamil" to "Tamil",
    "telugu" to "Telugu",
    "malayalam" to "Malayalam",
    "kannada" to "Kannada",
    "marathi" to "Marathi",
    "punjabi" to "Punjabi",
    "dual audio" to "Dual Audio",
    "bengalibongobd" to "Bengali",
)

fun extractSeasonBadge(
    type: String,
    seasonCount: Int?,
    title: String? = null,
    releaseInfo: String? = null,
    description: String? = null,
): String? {
    val base = formatSeasonBadge(type, seasonCount)
    if (base != null) return base

    val combined = listOfNotNull(title, releaseInfo, description).joinToString(" ")
    val seasonMatch = SeasonPattern.find(combined)
    if (seasonMatch != null) {
        val group = seasonMatch.groupValues[1]
        return "S$group"
    }

    val epMatch = EpisodeRangePattern.find(combined)
    if (epMatch != null) {
        return "E${epMatch.groupValues[1]}"
    }
    return null
}

fun extractExtraBadge(
    title: String? = null,
    releaseInfo: String? = null,
    description: String? = null,
): String? {
    val combined = listOfNotNull(title, releaseInfo, description).joinToString(" ")
    if (CompletePattern.containsMatchIn(combined)) {
        return "Complete"
    }
    return null
}

fun extractLanguageBadge(
    language: String?,
    title: String? = null,
    releaseInfo: String? = null,
    description: String? = null,
): String? {
    val base = formatLanguageBadge(language)
    if (base != null && base != language?.uppercase()) return base

    val combined = listOfNotNull(title, releaseInfo, description).joinToString(" ")
    for ((keyword, label) in LanguageKeywords) {
        if (combined.contains(keyword, ignoreCase = true)) {
            return label
        }
    }
    return base
}
