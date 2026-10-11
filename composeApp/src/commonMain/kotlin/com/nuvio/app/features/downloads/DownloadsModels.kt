package com.nuvio.app.features.downloads

import com.nuvio.app.features.player.SubtitleAddonRequest
import com.nuvio.app.features.streams.StreamSubtitle
import kotlinx.serialization.Serializable
import kotlinx.coroutines.runBlocking
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.downloads_enqueue_missing_url
import nuvio.composeapp.generated.resources.downloads_enqueue_replaced
import nuvio.composeapp.generated.resources.downloads_enqueue_started
import nuvio.composeapp.generated.resources.downloads_enqueue_unsupported_format
import nuvio.composeapp.generated.resources.downloads_trying_next_source
import org.jetbrains.compose.resources.getString

@Serializable
enum class DownloadStatus {
    Downloading,
    Paused,
    Completed,
    Failed,
}

@Serializable
data class DownloadItem(
    val id: String,
    val contentType: String,
    val parentMetaId: String,
    val parentMetaType: String,
    val videoId: String,
    val title: String,
    val logo: String? = null,
    val poster: String? = null,
    val background: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
    val episodeThumbnail: String? = null,
    val streamTitle: String,
    val streamSubtitle: String? = null,
    val providerName: String,
    val providerAddonId: String? = null,
    val sourceUrl: String,
    val sourceHeaders: Map<String, String> = emptyMap(),
    val sourceResponseHeaders: Map<String, String> = emptyMap(),
    val subtitleRequests: List<SubtitleAddonRequest> = emptyList(),
    val sourceSubtitles: List<StreamSubtitle> = emptyList(),
    val localFileUri: String? = null,
    val fileName: String,
    val status: DownloadStatus,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val errorMessage: String? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    /**
     * Sibling mirrors captured when the download was queued. The first dead
     * link walks this list automatically before the row turns Failed.
     */
    val fallbackSources: List<DownloadFallbackSource> = emptyList(),
    /** How many mirrors have already been tried; [fallbackSources].size is the cap. */
    val fallbackUsed: Int = 0,
    /** Transient note shown next to the status ("Trying next source (2 of 3)"). */
    val statusNote: String? = null,
) {
    val isEpisode: Boolean
        get() = seasonNumber != null && episodeNumber != null

    val isPlayable: Boolean
        get() = status == DownloadStatus.Completed && !localFileUri.isNullOrBlank()

    val displaySubtitle: String
        get() = episodeTitle.orEmpty()

    val progressFraction: Float
        get() {
            val total = totalBytes?.takeIf { it > 0L } ?: return 0f
            return (downloadedBytes.toDouble() / total.toDouble())
                .toFloat()
                .coerceIn(0f, 1f)
        }

    val logicalContentKey: String
        get() = if (isEpisode) {
            "${parentMetaId.trim()}|${seasonNumber ?: -1}|${episodeNumber ?: -1}"
        } else {
            "${parentMetaId.trim()}|movie"
        }

    /**
     * The next mirror to try after a dead link, or null once they are spent.
     * A switch moves the whole identity of the row onto the new source: URL,
     * headers, provider label and subtitles all follow the mirror being tried,
     * so the user sees an honest "who is downloading this" at every moment.
     */
    fun nextFallbackOrNull(): DownloadItem? {
        val source = fallbackSources.getOrNull(fallbackUsed) ?: return null
        val nextIndex = fallbackUsed + 1
        return copy(
            sourceUrl = source.url,
            sourceHeaders = source.headers,
            providerName = source.providerName.ifBlank { providerName },
            providerAddonId = source.providerAddonId ?: providerAddonId,
            streamTitle = source.streamTitle.ifBlank { streamTitle },
            streamSubtitle = source.streamSubtitle ?: streamSubtitle,
            sourceSubtitles = source.subtitles.ifEmpty { sourceSubtitles },
            fallbackUsed = nextIndex,
            status = DownloadStatus.Downloading,
            statusNote = runBlocking {
                getString(Res.string.downloads_trying_next_source, nextIndex, fallbackSources.size)
            },
            errorMessage = null,
            downloadedBytes = 0L,
            totalBytes = null,
            updatedAtEpochMs = DownloadsClock.nowEpochMs(),
        )
    }
}

/**
 * One sibling mirror captured at enqueue time: enough to restart the transfer
 * without re-resolving any addon (a dead debrid link is why we are here).
 */
@Serializable
data class DownloadFallbackSource(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val providerName: String = "",
    val providerAddonId: String? = null,
    val streamTitle: String = "",
    val streamSubtitle: String? = null,
    val subtitles: List<StreamSubtitle> = emptyList(),
)

data class DownloadsUiState(
    val items: List<DownloadItem> = emptyList(),
) {
    val activeItems: List<DownloadItem>
        get() = items.filter { it.status != DownloadStatus.Completed }

    val completedItems: List<DownloadItem>
        get() = items.filter { it.status == DownloadStatus.Completed }
}

enum class DownloadEnqueueResult {
    Started,
    Replaced,
    MissingUrl,
    UnsupportedFormat;

    fun toastMessage(): String = runBlocking {
        when (this@DownloadEnqueueResult) {
            Started -> getString(Res.string.downloads_enqueue_started)
            Replaced -> getString(Res.string.downloads_enqueue_replaced)
            MissingUrl -> getString(Res.string.downloads_enqueue_missing_url)
            UnsupportedFormat -> getString(Res.string.downloads_enqueue_unsupported_format)
        }
    }
}

internal fun List<DownloadItem>.sortedForSeriesDownloads(): List<DownloadItem> =
    sortedWith(downloadSeriesEpisodeComparator)

internal val downloadSeriesEpisodeComparator: Comparator<DownloadItem> =
    compareBy<DownloadItem> { it.seasonNumber ?: Int.MAX_VALUE }
        .thenBy { it.episodeNumber ?: Int.MAX_VALUE }
        .thenBy { it.episodeTitle?.trim().orEmpty().lowercase() }
        .thenBy { it.title.trim().lowercase() }
        .thenBy { it.id }
