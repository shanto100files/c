package com.nuvio.app.features.details

import com.nuvio.app.core.format.extractReleaseYearForDisplay

private fun isEndedSeriesStatus(status: String?): Boolean {
    if (status.isNullOrBlank()) return false
    val s = status.trim().lowercase()
    if ("returning" in s || "in production" in s) return false
    return "ended" in s || "canceled" in s || "cancelled" in s
}

/**
 * Compact release line under the details hero: movies → year only; TV → "2025 -" or "2021 - 2028".
 */
fun formatMetaReleaseLineForDetails(meta: MetaDetails): String? {
    val raw = meta.releaseInfo?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    // A bridged or manually authored post may carry `other` for its type; the
    // episode list decides, so "2021 -" is shown instead of a bare year.
    if (!meta.isSeriesLike) {
        return extractReleaseYearForDisplay(raw)?.toString()
    }
    val startYear = extractReleaseYearForDisplay(raw) ?: return raw
    val endYear = meta.lastAirDate?.let { extractReleaseYearForDisplay(it) }
    return when {
        isEndedSeriesStatus(meta.status) && endYear != null ->
            if (endYear == startYear) startYear.toString()
            else "$startYear - $endYear"
        isEndedSeriesStatus(meta.status) -> startYear.toString()
        else -> "$startYear -"
    }
}
