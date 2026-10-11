package com.nuvio.app.features.home

import com.nuvio.app.core.site.SiteStorage
import com.nuvio.app.features.catalog.CatalogTarget
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Disk copy of the home rows, so an app cold start paints last session's content instantly
 * while fresh rows download in the background (stale-while-revalidate).
 *
 * The live models ([HomeCatalogSection], [MetaPreview]) stay plain data classes; this file
 * mirrors them field-for-field instead of annotating them, and `ignoreUnknownKeys` plus
 * defaults on both directions make a cache written by an older or newer build a harmless
 * miss rather than a crash.
 */
internal object HomeCatalogCache {
    private const val VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Map key is the definition's [HomeCatalogDefinition.cacheKey], same as the in-memory cache. */
    fun load(): Map<String, HomeCatalogSection> {
        val raw = SiteStorage.loadHomeCatalogCache() ?: return emptyMap()
        val cached = runCatching { json.decodeFromString<CachedCatalog>(raw) }
            .getOrNull()
            ?: return emptyMap()
        if (cached.version != VERSION) return emptyMap()
        return cached.sections
            .mapNotNull { entry -> entry.toSectionOrNull()?.let { section -> entry.cacheKey to section } }
            .toMap()
    }

    fun save(sections: Map<String, HomeCatalogSection>) {
        if (sections.isEmpty()) {
            // Nothing worth showing is loaded (or every addon was turned off): drop the file so
            // a later launch cannot resurrect rows the user deliberately removed.
            SiteStorage.saveHomeCatalogCache("")
            return
        }
        val payload = sections.mapNotNull { (cacheKey, section) -> section.toCachedOrNull(cacheKey) }
        if (payload.isEmpty()) {
            SiteStorage.saveHomeCatalogCache("")
            return
        }
        SiteStorage.saveHomeCatalogCache(
            json.encodeToString(CachedCatalog(version = VERSION, sections = payload)),
        )
    }

    private fun HomeCatalogSection.toCachedOrNull(cacheKey: String): CachedSection? {
        val addon = target as? CatalogTarget.Addon ?: return null
        return CachedSection(
            cacheKey = cacheKey,
            key = key,
            title = title,
            addonName = addonName,
            manifestUrl = addon.manifestUrl,
            contentType = addon.contentType,
            catalogId = addon.catalogId,
            supportsPagination = addon.supportsPagination,
            availableItemCount = availableItemCount,
            hasMore = hasMore,
            items = items.map { it.toCached() },
        )
    }

    private fun CachedSection.toSectionOrNull(): HomeCatalogSection? {
        if (manifestUrl.isBlank()) return null
        return HomeCatalogSection(
            key = key,
            title = title,
            subtitle = addonName,
            addonName = addonName,
            target = CatalogTarget.Addon(
                manifestUrl = manifestUrl,
                contentType = contentType,
                catalogId = catalogId,
                supportsPagination = supportsPagination,
            ),
            items = items.map { it.toPreview() },
            availableItemCount = availableItemCount,
            hasMore = hasMore,
        )
    }

    private fun MetaPreview.toCached() = CachedMetaPreview(
        id = id,
        type = type,
        name = name,
        poster = poster,
        banner = banner,
        logo = logo,
        posterShape = posterShape.name,
        description = description,
        releaseInfo = releaseInfo,
        rawReleaseDate = rawReleaseDate,
        popularity = popularity,
        voteCount = voteCount,
        imdbRating = imdbRating,
        genres = genres,
        rawPosterUrl = rawPosterUrl,
        landscapePoster = landscapePoster,
        rawLandscapePosterUrl = rawLandscapePosterUrl,
        language = language,
        seasonCount = seasonCount,
    )

    private fun CachedMetaPreview.toPreview() = MetaPreview(
        id = id,
        type = type,
        name = name,
        poster = poster,
        banner = banner,
        logo = logo,
        posterShape = runCatching { PosterShape.valueOf(posterShape) }
            .getOrDefault(PosterShape.Poster),
        description = description,
        releaseInfo = releaseInfo,
        rawReleaseDate = rawReleaseDate,
        popularity = popularity,
        voteCount = voteCount,
        imdbRating = imdbRating,
        genres = genres,
        rawPosterUrl = rawPosterUrl,
        landscapePoster = landscapePoster,
        rawLandscapePosterUrl = rawLandscapePosterUrl,
        language = language,
        seasonCount = seasonCount,
    )

    @Serializable
    private data class CachedCatalog(
        val version: Int = VERSION,
        val sections: List<CachedSection> = emptyList(),
    )

    @Serializable
    private data class CachedSection(
        val cacheKey: String,
        val key: String,
        val title: String,
        val addonName: String,
        val manifestUrl: String,
        val contentType: String,
        val catalogId: String,
        val supportsPagination: Boolean,
        val availableItemCount: Int = 0,
        val hasMore: Boolean = false,
        val items: List<CachedMetaPreview> = emptyList(),
    )

    @Serializable
    private data class CachedMetaPreview(
        val id: String,
        val type: String,
        val name: String,
        val poster: String? = null,
        val banner: String? = null,
        val logo: String? = null,
        val posterShape: String = PosterShape.Poster.name,
        val description: String? = null,
        val releaseInfo: String? = null,
        val rawReleaseDate: String? = null,
        val popularity: Double? = null,
        val voteCount: Int? = null,
        val imdbRating: String? = null,
        val genres: List<String> = emptyList(),
        val rawPosterUrl: String? = null,
        val landscapePoster: String? = null,
        val rawLandscapePosterUrl: String? = null,
        val language: String? = null,
        val seasonCount: Int? = null,
    )
}
