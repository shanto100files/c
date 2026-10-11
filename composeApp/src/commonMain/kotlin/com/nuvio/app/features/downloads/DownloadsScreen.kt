package com.nuvio.app.features.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.i18n.localizedByteUnit
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.core.ui.NuvioStatusModal
import com.nuvio.app.core.ui.NuvioToastController
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun DownloadsScreen(
    // Nullable: as a pushed route there is always somewhere to go back to, but as a
    // root tab there is not - and NuvioScreenHeader only draws the arrow when this
    // is non-null.
    onBack: (() -> Unit)? = null,
    onOpenDownload: (DownloadItem) -> Unit,
    initialShowId: String? = null,
    onNavigateToShow: ((showId: String, title: String) -> Unit)? = null,
    onBackFromShow: (() -> Unit)? = null,
) {
    val uiState by remember {
        DownloadsRepository.ensureLoaded()
        DownloadsRepository.uiState
    }.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.completedItems.size) {
        DownloadsRepository.markCompletedSeen()
    }

    var selectedShowId by rememberSaveable(initialShowId) { mutableStateOf(initialShowId) }
    var downloadPendingDeletionId by rememberSaveable { mutableStateOf<String?>(null) }
    var clearCompletedIds by remember { mutableStateOf<List<String>?>(null) }
    val openDownloadsDirectoryFailedText = stringResource(Res.string.downloads_open_directory_failed)
    // As a pushed route there is always somewhere to return to; as a root tab there is
    // not, and NuvioScreenHeader only draws the arrow when this is non-null.
    val headerBack: (() -> Unit)? = if (selectedShowId != null) {
        {
            onBackFromShow?.invoke() ?: run { selectedShowId = null }
        }
    } else {
        onBack
    }

    val completedEpisodes = remember(uiState.items) {
        uiState.completedItems
            .filter { it.isEpisode }
            .sortedForSeriesDownloads()
    }

    val selectedShowTitle = remember(selectedShowId, completedEpisodes) {
        selectedShowId?.let { showId ->
            val raw = completedEpisodes.firstOrNull { it.parentMetaId == showId }?.title
            raw?.let { cleanShowTitle(it) }
        }
    }

    NuvioScreen {
        stickyHeader {
            NuvioScreenHeader(
                title = if (selectedShowId == null) {
                    stringResource(Res.string.compose_settings_root_downloads_title)
                } else {
                    selectedShowTitle ?: stringResource(Res.string.downloads_show_downloads)
                },
                onBack = headerBack,
                actions = {
                    IconButton(
                        onClick = {
                            if (!DownloadsPlatformDownloader.openDownloadsDirectory()) {
                                NuvioToastController.show(openDownloadsDirectoryFailedText)
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = stringResource(Res.string.downloads_open_directory),
                        )
                    }
                },
            )
        }

        if (selectedShowId == null) {
            downloadsRootContent(
                uiState = uiState,
                onOpenDownload = onOpenDownload,
                onOpenShow = { showId, title ->
                    onNavigateToShow?.invoke(showId, title) ?: run { selectedShowId = showId }
                },
                onDeleteDownload = { downloadPendingDeletionId = it },
                onClearCompleted = { ids -> clearCompletedIds = ids },
            )
        } else {
            downloadsShowContent(
                showId = selectedShowId.orEmpty(),
                episodes = completedEpisodes,
                onOpenDownload = onOpenDownload,
                onDeleteDownload = { downloadPendingDeletionId = it },
            )
        }
    }

    val pendingDeletionId = downloadPendingDeletionId
    if (pendingDeletionId != null) {
        NuvioStatusModal(
            title = stringResource(Res.string.action_delete_confirm_title),
            message = stringResource(Res.string.action_delete_confirm_message),
            isVisible = true,
            destructive = true,
            confirmText = stringResource(Res.string.action_yes),
            dismissText = stringResource(Res.string.action_no),
            onConfirm = {
                DownloadsRepository.cancelDownload(pendingDeletionId)
                downloadPendingDeletionId = null
            },
            onDismiss = { downloadPendingDeletionId = null },
        )
    }

    val pendingClearIds = clearCompletedIds
    if (pendingClearIds != null) {
        NuvioStatusModal(
            title = stringResource(Res.string.downloads_clear_completed),
            message = stringResource(Res.string.action_delete_confirm_message),
            isVisible = true,
            destructive = true,
            confirmText = stringResource(Res.string.action_yes),
            dismissText = stringResource(Res.string.action_no),
            onConfirm = {
                pendingClearIds.forEach { DownloadsRepository.cancelDownload(it) }
                clearCompletedIds = null
            },
            onDismiss = { clearCompletedIds = null },
        )
    }
}

private fun LazyListScope.downloadsRootContent(
    uiState: DownloadsUiState,
    onOpenDownload: (DownloadItem) -> Unit,
    onOpenShow: (showId: String, title: String) -> Unit,
    onDeleteDownload: (String) -> Unit,
    onClearCompleted: (List<String>) -> Unit,
) {
    val activeItems = uiState.activeItems
    val completedMovies = uiState.completedItems.filterNot(DownloadItem::isEpisode)
    val completedShows = uiState.completedItems
        .filter(DownloadItem::isEpisode)
        .groupBy { it.parentMetaId }
        .mapNotNull { (_, episodes) ->
            episodes.firstOrNull()?.let { first ->
                first to episodes
            }
        }
        .sortedBy { (item, _) -> item.title.lowercase() }

    if (uiState.items.isNotEmpty()) {
        item {
            DownloadStatsCard(
                movieCount = uiState.items.count { !it.isEpisode },
                episodeCount = uiState.items.count { it.isEpisode },
                totalBytes = uiState.items.sumOf { it.totalBytes ?: it.downloadedBytes },
            )
        }
    }

    if (activeItems.isNotEmpty()) {
        item {
            SectionTitle(
                title = stringResource(Res.string.downloads_section_active),
                actionLabel = stringResource(Res.string.downloads_pause_all)
                    .takeIf { activeItems.any { it.status == DownloadStatus.Downloading } },
                onAction = { DownloadsRepository.pauseActiveDownloads() },
            )
        }
        items(
            items = activeItems,
            key = { it.id },
        ) { item ->
            DownloadRow(
                item = item,
                onOpen = { onOpenDownload(item) },
                onPause = { DownloadsRepository.pauseDownload(item.id) },
                onResume = { DownloadsRepository.resumeDownload(item.id) },
                onRetry = { DownloadsRepository.retryDownload(item.id) },
                onDelete = { onDeleteDownload(item.id) },
            )
        }
    }

    if (completedMovies.isNotEmpty()) {
        item {
            SectionTitle(
                title = stringResource(Res.string.downloads_section_movies),
                actionLabel = stringResource(Res.string.downloads_clear_completed),
                onAction = { onClearCompleted(completedMovies.map { it.id }) },
            )
        }
        items(
            items = completedMovies,
            key = { it.id },
        ) { item ->
            DownloadRow(
                item = item,
                onOpen = { onOpenDownload(item) },
                onPause = { DownloadsRepository.pauseDownload(item.id) },
                onResume = { DownloadsRepository.resumeDownload(item.id) },
                onRetry = { DownloadsRepository.retryDownload(item.id) },
                onDelete = { onDeleteDownload(item.id) },
            )
        }
    }

    if (completedShows.isNotEmpty()) {
        val completedEpisodeIds = completedShows.flatMap { (_, episodes) -> episodes.map { it.id } }
        item {
            SectionTitle(
                title = stringResource(Res.string.downloads_section_shows),
                actionLabel = stringResource(Res.string.downloads_clear_completed),
                onAction = { onClearCompleted(completedEpisodeIds) },
            )
        }
        items(
            items = completedShows,
            key = { (item, _) -> item.parentMetaId },
        ) { (item, episodes) ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable { onOpenShow(item.parentMetaId, item.title) },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (episodes.size == 1) {
                                stringResource(Res.string.downloads_episode_count_one, episodes.size)
                            } else {
                                stringResource(Res.string.downloads_episode_count, episodes.size)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (uiState.items.isEmpty()) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = stringResource(Res.string.downloads_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.downloads_empty_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun LazyListScope.downloadsShowContent(
    showId: String,
    episodes: List<DownloadItem>,
    onOpenDownload: (DownloadItem) -> Unit,
    onDeleteDownload: (String) -> Unit,
) {
    val showEpisodes = episodes
        .filter { it.parentMetaId == showId }
        .sortedForSeriesDownloads()

    val seasons = showEpisodes
        .groupBy { it.seasonNumber ?: 0 }
        .toList()
        .sortedWith(
            compareBy<Pair<Int, List<DownloadItem>>> { (season, _) ->
                if (season == 0) 0 else 1
            }.thenBy { (season, _) -> if (season == 0) 0 else season },
        )

    if (seasons.isEmpty()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.downloads_empty_episodes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    seasons.forEach { (seasonNumber, entries) ->
        item {
            SectionTitle(
                if (seasonNumber == 0) {
                    stringResource(Res.string.episodes_specials)
                } else {
                    stringResource(Res.string.episodes_season, seasonNumber)
                },
            )
        }

        val sortedEpisodes = entries.sortedForSeriesDownloads()

        items(
            items = sortedEpisodes,
            key = { it.id },
        ) { item ->
            DownloadRow(
                item = item,
                onOpen = { onOpenDownload(item) },
                onPause = { DownloadsRepository.pauseDownload(item.id) },
                onResume = { DownloadsRepository.resumeDownload(item.id) },
                onRetry = { DownloadsRepository.retryDownload(item.id) },
                onDelete = { onDeleteDownload(item.id) },
            )
        }
    }
}

@Composable
private fun DownloadRow(
    item: DownloadItem,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    val displayTitle = item.displayTitle()
    val displaySubtitle = downloadDisplaySubtitle(
        item = item,
        displayTitle = displayTitle,
    )
    val qualityToken = remember(item.id, item.streamTitle, item.title) { downloadQualityToken(item) }
    val qualityColor = remember(qualityToken) { downloadQualityColor(qualityToken) }
    val statusColor = when (item.status) {
        DownloadStatus.Downloading -> MaterialTheme.colorScheme.primary
        DownloadStatus.Paused -> MaterialTheme.colorScheme.onSurfaceVariant
        DownloadStatus.Failed -> MaterialTheme.colorScheme.error
        DownloadStatus.Completed -> Color(0xFF4CAF50)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(enabled = item.isPlayable, onClick = onOpen),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DownloadPosterThumb(
                    posterUrl = item.poster,
                    title = displayTitle,
                    fallbackColor = qualityColor ?: MaterialTheme.colorScheme.primary,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (qualityToken != null && qualityColor != null) {
                        DownloadQualityPill(token = qualityToken, color = qualityColor)
                    }
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = displaySubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = downloadStatusLine(item),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (item.status) {
                        DownloadStatus.Downloading -> {
                            IconButton(onClick = onPause) {
                                Icon(
                                    imageVector = Icons.Rounded.Pause,
                                    contentDescription = stringResource(Res.string.compose_action_pause),
                                )
                            }
                        }
                        DownloadStatus.Paused -> {
                            IconButton(onClick = onResume) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = stringResource(Res.string.action_resume),
                                )
                            }
                        }
                        DownloadStatus.Failed -> {
                            IconButton(onClick = onRetry) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = stringResource(Res.string.action_retry),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        DownloadStatus.Completed -> {
                            IconButton(onClick = onOpen) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = stringResource(Res.string.action_play),
                                    tint = Color(0xFF4CAF50),
                                )
                            }
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(Res.string.action_delete),
                        )
                    }
                }
            }

            if (item.status == DownloadStatus.Downloading) {
                LinearProgressIndicator(
                    progress = { item.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                )
            }
        }
    }
}

@Composable
private fun DownloadPosterThumb(
    posterUrl: String?,
    title: String,
    fallbackColor: Color,
    modifier: Modifier = Modifier,
) {
    if (!posterUrl.isNullOrBlank()) {
        AsyncImage(
            model = posterUrl,
            contentDescription = title,
            modifier = modifier
                .size(width = 52.dp, height = 74.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier
                .size(width = 52.dp, height = 74.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(fallbackColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title.trim().firstOrNull()?.uppercase() ?: "•",
                style = MaterialTheme.typography.titleLarge,
                color = fallbackColor,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun downloadStatusLine(item: DownloadItem): String {
    val size = if (item.totalBytes != null && item.totalBytes > 0L) {
        "${formatBytes(item.downloadedBytes)} / ${formatBytes(item.totalBytes)}"
    } else {
        formatBytes(item.downloadedBytes)
    }
    val percent = if (item.status == DownloadStatus.Downloading && item.progressFraction > 0f) {
        " • ${(item.progressFraction * 100).toInt()}%"
    } else {
        ""
    }

    return when (item.status) {
        DownloadStatus.Downloading -> {
            val base = stringResource(Res.string.downloads_status_downloading, size) + percent
            // "Trying next source (2 of 3)" rides along with the progress text.
            item.statusNote?.let { note -> "$base • $note" } ?: base
        }
        DownloadStatus.Paused -> stringResource(Res.string.downloads_status_paused, size)
        DownloadStatus.Completed -> stringResource(
            Res.string.downloads_status_completed,
            formatBytes(item.totalBytes ?: item.downloadedBytes),
        )
        DownloadStatus.Failed -> item.errorMessage ?: stringResource(Res.string.downloads_status_failed)
    }
}

private fun DownloadItem.displayTitle(): String =
    if (isEpisode) {
        episodeTitle?.trim()?.takeIf { it.isNotBlank() } ?: title
    } else {
        title
    }

private fun cleanShowTitle(rawTitle: String): String {
    var cleaned = rawTitle.trim()
    cleaned = cleaned.replace(Regex("""\[[^]]*]"""), "").trim()
    val match = Regex("""(?i)\s*(?:\(Season\s*\d+\)|\bSeason\s*\d+|\bChapter\s*\d+|\bS\d{1,2}E\d{1,3}|\bS\d{1,2}\b).*""").find(cleaned)
    if (match != null) {
        cleaned = cleaned.substring(0, match.range.first).trim()
    }
    return cleaned.ifBlank { rawTitle }
}

@Composable
private fun downloadDisplaySubtitle(
    item: DownloadItem,
    displayTitle: String,
): String {
    val seasonNumber = item.seasonNumber
    val episodeNumber = item.episodeNumber
    if (seasonNumber == null || episodeNumber == null) {
        return item.displaySubtitle
    }

    val episodeCode = stringResource(
        Res.string.compose_player_episode_code_full,
        seasonNumber,
        episodeNumber,
    )
    return listOf(
        episodeCode,
        item.episodeTitle?.trim().orEmpty().takeIf { it.isNotBlank() && it != displayTitle },
        item.title.trim().takeIf { it.isNotBlank() && it != displayTitle },
    ).filterNotNull().joinToString(" • ")
}

@Composable
private fun SectionTitle(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                modifier = Modifier.clickable { onAction() },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DownloadStatsCard(
    movieCount: Int,
    episodeCount: Int,
    totalBytes: Long,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = stringResource(
                    Res.string.downloads_stats_summary,
                    movieCount,
                    episodeCount,
                    formatBytes(totalBytes),
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val DownloadQualityTokenPattern = Regex("""(?i)\b(2160p|4k|1440p|1080p|720p|480p|360p)\b""")

private fun downloadQualityToken(item: DownloadItem): String? =
    DownloadQualityTokenPattern.find("${item.streamTitle} ${item.title}")?.groupValues?.get(1)?.uppercase()

private fun downloadQualityColor(token: String?): Color? = when (token?.uppercase()) {
    "2160P", "4K" -> Color(0xFFFFB300)
    "1440P" -> Color(0xFFFFB300).copy(alpha = 0.85f)
    "1080P" -> Color(0xFFB388FF)
    "720P" -> Color(0xFF4FC3F7)
    "480P", "360P" -> Color(0xFF69F0AE)
    else -> null
}

@Composable
private fun DownloadQualityPill(
    token: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = token.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
            ),
            color = color,
            maxLines = 1,
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 ${localizedByteUnit("B")}"
    val kib = 1024.0
    val mib = kib * 1024.0
    val gib = mib * 1024.0
    val value = bytes.toDouble()
    return when {
        value >= gib -> "${((value / gib) * 10.0).toInt() / 10.0} ${localizedByteUnit("GB")}"
        value >= mib -> "${((value / mib) * 10.0).toInt() / 10.0} ${localizedByteUnit("MB")}"
        value >= kib -> "${((value / kib) * 10.0).toInt() / 10.0} ${localizedByteUnit("KB")}"
        else -> "$bytes ${localizedByteUnit("B")}"
    }
}
