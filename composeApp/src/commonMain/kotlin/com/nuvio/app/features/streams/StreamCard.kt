package com.nuvio.app.features.streams

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.SkeletonBlock
import com.nuvio.app.features.debrid.DebridProviders
import com.nuvio.app.features.details.components.DownloadStatusGlyph
import com.nuvio.app.features.downloads.DownloadStatus

/**
 * Download state of one stream row, resolved by the caller from [com.nuvio.app.features.downloads.DownloadsUiState].
 *
 * Null-valued [StreamCard.downloadStatus] means "nothing tracked for this row"; a non-null status
 * always travels with the progress that drives the ring.
 */
internal data class StreamDownloadState(
    val status: DownloadStatus,
    val progress: Float,
    val downloadId: String? = null,
)

private val QualityTokenPattern = Regex("""(?i)\b(2160p|4k|1440p|1080p|720p|480p|360p)\b""")

private val QualityUhd = Color(0xFFFFB300)
private val Quality1080 = Color(0xFFB388FF)
private val Quality720 = Color(0xFF4FC3F7)
private val QualitySd = Color(0xFF69F0AE)

/** First quality token of the row label ("720p" of "MLBDProvider - 720p"), if any. */
private fun streamQualityToken(label: String): String? =
    QualityTokenPattern.find(label)?.groupValues?.get(1)?.uppercase()

private fun qualityAccentColor(token: String?): Color? = when (token?.uppercase()) {
    "2160P", "4K" -> QualityUhd
    "1440P" -> QualityUhd.copy(alpha = 0.85f)
    "1080P" -> Quality1080
    "720P" -> Quality720
    "480P", "360P" -> QualitySd
    else -> null
}

private val EpisodeCodeRegex = Regex("""(?i)\b(s\d{1,2}\s?e\d{1,3}|s\d{1,2}|e\d{1,3}|ep\s?\d{1,3}|episode\s?\d{1,3})\b""")

internal fun ensureEpisodeCodeInStreamLabel(
    label: String,
    seasonNumber: Int?,
    episodeNumber: Int?,
): String {
    if (seasonNumber == null || episodeNumber == null) return label
    if (EpisodeCodeRegex.containsMatchIn(label)) return label

    val sPad = if (seasonNumber in 0..9) "0$seasonNumber" else "$seasonNumber"
    val ePad = if (episodeNumber in 0..9) "0$episodeNumber" else "$episodeNumber"
    val epCode = "S${sPad}E${ePad}"
    val qualityMatch = QualityTokenPattern.find(label)
    return if (qualityMatch != null) {
        val idx = qualityMatch.range.first
        val prefix = label.substring(0, idx).trimEnd('-', ' ', '•', '|')
        val suffix = label.substring(idx)
        if (prefix.isNotBlank()) "$prefix $epCode - $suffix" else "$epCode $suffix"
    } else {
        "$label $epCode"
    }
}

@Composable
internal fun StreamCard(
    stream: StreamItem,
    enabled: Boolean,
    appendInstantServiceToDefaultName: Boolean,
    showFileSizeBadges: Boolean,
    showAddonLogo: Boolean,
    badgePlacement: StreamBadgePlacement,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isCurrent: Boolean = false,
    currentLabel: String? = null,
    downloadState: StreamDownloadState? = null,
    seasonNumber: Int? = null,
    episodeNumber: Int? = null,
    onDownloadClick: (() -> Unit)? = null,
    onPauseDownload: ((downloadId: String) -> Unit)? = null,
    onResumeDownload: ((downloadId: String) -> Unit)? = null,
    onCancelDownload: ((downloadId: String) -> Unit)? = null,
) {
    val displayLabel = remember(stream.streamLabel, seasonNumber, episodeNumber) {
        ensureEpisodeCodeInStreamLabel(stream.streamLabel, seasonNumber, episodeNumber)
    }
    val cardShape = RoundedCornerShape(12.dp)
    val badgeImages = stream.badges.filter { it.imageURL.isNotBlank() }
    val hasBadges = badgeImages.isNotEmpty() || (showFileSizeBadges && stream.behaviorHints.videoSize != null)
    val qualityToken = remember(displayLabel) { streamQualityToken(displayLabel) }
    val accentColor = remember(qualityToken) { qualityAccentColor(qualityToken) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .shadow(
                elevation = 2.dp,
                shape = cardShape,
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.04f),
            )
            .clip(cardShape)
            .background(
                if (isCurrent) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    Color.White.copy(alpha = 0.05f)
                },
            )
            .background(
                if (isCurrent || accentColor == null) {
                    Color.Transparent
                } else {
                    // A whisper of the row's quality colour, layered over the base card.
                    accentColor.copy(alpha = 0.06f)
                },
            )
            .then(
                if (isCurrent) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.52f),
                        shape = cardShape,
                    )
                } else {
                    Modifier
                },
            )
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        Column(modifier = Modifier.weight(1f)) {
            if (hasBadges && badgePlacement == StreamBadgePlacement.TOP) {
                StreamCardBadgeRow(
                    badgeImages = badgeImages,
                    stream = stream,
                    showFileSizeBadges = showFileSizeBadges,
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            StreamNameWithInstantService(
                stream = stream,
                displayLabel = displayLabel,
                appendInstantServiceToDefaultName = appendInstantServiceToDefaultName,
                accentColor = accentColor,
            ) {
                if (isCurrent && !currentLabel.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CurrentStreamBadge(label = currentLabel)
                }
            }

            val subtitle = stream.streamSubtitle
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (hasBadges && badgePlacement == StreamBadgePlacement.BOTTOM) {
                Spacer(modifier = Modifier.height(5.dp))
                StreamCardBadgeRow(
                    badgeImages = badgeImages,
                    stream = stream,
                    showFileSizeBadges = showFileSizeBadges,
                )
            }
        }

        if (showAddonLogo) {
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!stream.addonLogo.isNullOrBlank()) {
                    AsyncImage(
                        model = stream.addonLogo,
                        contentDescription = stream.addonName,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stream.addonName,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (onDownloadClick != null || downloadState != null) {
            Spacer(modifier = Modifier.width(if (showAddonLogo) 10.dp else 6.dp))
            StreamDownloadControls(
                downloadState = downloadState,
                accentColor = accentColor,
                onDownloadClick = onDownloadClick,
                onPauseDownload = onPauseDownload,
                onResumeDownload = onResumeDownload,
                onCancelDownload = onCancelDownload,
            )
        }
        }
    }
}

@Composable
private fun StreamDownloadControls(
    downloadState: StreamDownloadState?,
    accentColor: Color?,
    onDownloadClick: (() -> Unit)?,
    onPauseDownload: ((downloadId: String) -> Unit)?,
    onResumeDownload: ((downloadId: String) -> Unit)?,
    onCancelDownload: ((downloadId: String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val status = downloadState?.status
    val downloadId = downloadState?.downloadId
    val controlAccent = accentColor ?: MaterialTheme.colorScheme.primary
    when (status) {
        DownloadStatus.Downloading -> Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DownloadStatusGlyph(
                status = status,
                progress = downloadState?.progress ?: 0f,
                showPercent = true,
                modifier = Modifier.size(46.dp),
            )
            Spacer(modifier = Modifier.width(2.dp))
            if (downloadId != null && onPauseDownload != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onPauseDownload(downloadId) }
                        .background(controlAccent.copy(alpha = 0.14f))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = controlAccent,
                    )
                }
            }
            if (downloadId != null && onCancelDownload != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onCancelDownload(downloadId) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }

        DownloadStatus.Paused -> Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DownloadStatusGlyph(
                status = status,
                progress = downloadState?.progress ?: 0f,
                showPercent = true,
                modifier = Modifier.size(46.dp),
            )
            Spacer(modifier = Modifier.width(2.dp))
            if (downloadId != null && onResumeDownload != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onResumeDownload(downloadId) }
                        .background(controlAccent.copy(alpha = 0.14f))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = controlAccent,
                    )
                }
            }
            if (downloadId != null && onCancelDownload != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onCancelDownload(downloadId) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }

        else -> {
            if (onDownloadClick != null) {
                Box(
                    modifier = modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(
                            // A running transfer must not be re-enqueued by an accidental tap.
                            enabled = status != DownloadStatus.Downloading,
                            onClick = onDownloadClick,
                        )
                        .background(controlAccent.copy(alpha = 0.14f))
                        .padding(9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    DownloadStatusGlyph(
                        status = status,
                        progress = downloadState?.progress ?: 0f,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else if (status != null) {
                DownloadStatusGlyph(
                    status = status,
                    progress = downloadState?.progress ?: 0f,
                    modifier = modifier.size(32.dp),
                )
            }
        }
    }
}

@Composable
internal fun StreamCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.55f), height = 14.dp, cornerRadius = 4.dp)
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.85f), height = 10.dp, cornerRadius = 4.dp)
    }
}

@Composable
private fun StreamCardBadgeRow(
    badgeImages: List<StreamBadge>,
    stream: StreamItem,
    showFileSizeBadges: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        badgeImages.forEach { badge ->
            StreamBadgeImage(badge = badge)
        }
        if (showFileSizeBadges) {
            StreamFileSizeBadge(stream = stream)
        }
    }
}

@Composable
private fun StreamNameWithInstantService(
    stream: StreamItem,
    displayLabel: String,
    appendInstantServiceToDefaultName: Boolean,
    accentColor: Color? = null,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val nameStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    )
    val instantLabel = if (appendInstantServiceToDefaultName) {
        stream.instantServiceLabel()
    } else {
        null
    }
    val showInstantLabel = instantLabel != null
    val visibleState = remember(displayLabel) {
        MutableTransitionState(showInstantLabel)
    }
    visibleState.targetState = showInstantLabel

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = coloredStreamLabel(displayLabel, accentColor),
            modifier = Modifier.weight(1f, fill = false),
            style = nameStyle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(animationSpec = tween(durationMillis = 260)) +
                expandHorizontally(
                    animationSpec = tween(durationMillis = 260),
                    expandFrom = Alignment.Start,
                ),
            exit = fadeOut(animationSpec = tween(durationMillis = 120)) +
                shrinkHorizontally(
                    animationSpec = tween(durationMillis = 120),
                    shrinkTowards = Alignment.Start,
                ),
            label = "streamNameInstantService",
        ) {
            Text(
                text = " ${instantLabel.orEmpty()}",
                style = nameStyle,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        trailingContent()
    }
}

/** Label with its quality token ("480p") painted in the quality colour; unchanged otherwise. */
private fun coloredStreamLabel(label: String, accentColor: Color?): AnnotatedString {
    if (accentColor == null) return AnnotatedString(label)
    val match = QualityTokenPattern.find(label) ?: return AnnotatedString(label)
    return buildAnnotatedString {
        append(label.substring(0, match.range.first))
        withStyle(SpanStyle(color = accentColor)) {
            append(match.value)
        }
        append(label.substring(match.range.last + 1))
    }
}

@Composable
private fun CurrentStreamBadge(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun StreamItem.instantServiceLabel(): String? {
    val status = debridCacheStatus ?: return null
    if (status.state != StreamDebridCacheState.CACHED) return null
    val providerLabel = DebridProviders.shortName(status.providerId)
        .ifBlank { status.providerName.trim() }
        .ifBlank { DebridProviders.displayName(status.providerId) }
    return "- $providerLabel Instant"
}
