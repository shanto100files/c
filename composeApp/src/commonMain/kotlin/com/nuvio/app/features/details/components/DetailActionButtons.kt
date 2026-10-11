package com.nuvio.app.features.details.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.AppIconResource
import com.nuvio.app.core.ui.appIconPainter
import com.nuvio.app.features.downloads.DownloadStatus
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_download
import nuvio.composeapp.generated.resources.action_play
import nuvio.composeapp.generated.resources.action_retry
import nuvio.composeapp.generated.resources.details_actions_menu_label
import nuvio.composeapp.generated.resources.hero_add_to_library
import nuvio.composeapp.generated.resources.hero_mark_unwatched
import nuvio.composeapp.generated.resources.hero_mark_watched
import nuvio.composeapp.generated.resources.hero_remove_from_library
import nuvio.composeapp.generated.resources.playback_unavailable
import nuvio.composeapp.generated.resources.provider_downloaded
import nuvio.composeapp.generated.resources.random_episode_title
import nuvio.composeapp.generated.resources.shuffle_stop
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private val DownloadCompleteGreen = Color(0xFF4CAF50)

data class DetailSecondaryAction(
    val label: String,
    val icon: ImageVector,
    val isActive: Boolean = false,
    val onClick: () -> Unit = {},
    val onLongClick: (() -> Unit)? = null,
)

/**
 * Download entry point shown beside Play on the detail hero.
 *
 * [status] is null while nothing has been downloaded yet, which is the state where the button
 * acts as "pick a stream to download". Once an item exists, [progress] drives the ring and the
 * caller decides what a tap should do (jump to Downloads, play the finished file, ...).
 */
data class DetailDownloadAction(
    val label: String,
    val status: DownloadStatus? = null,
    val progress: Float = 0f,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailActionButtons(
    modifier: Modifier = Modifier,
    playLabel: String = stringResource(Res.string.action_play),
    playEnabled: Boolean = true,
    download: DetailDownloadAction? = null,
    pinnedAction: DetailSecondaryAction? = null,
    secondaryActions: List<DetailSecondaryAction> = emptyList(),
    actionsMenuLabel: String = stringResource(Res.string.details_actions_menu_label),
    isTablet: Boolean = false,
    onPlayClick: () -> Unit = {},
    onPlayLongClick: (() -> Unit)? = null,
) {
    val playPainter = appIconPainter(AppIconResource.PlayerPlay)
    val buttonHeight = if (isTablet) 56.dp else 52.dp
    val iconButtonSize = buttonHeight
    val playShape = RoundedCornerShape(40.dp)
    val hapticFeedback = LocalHapticFeedback.current
    var actionsExpanded by remember { mutableStateOf(false) }
    val menuProgress by animateFloatAsState(
        targetValue = if (actionsExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "detail_action_menu_progress",
    )
    val hasSecondaryActions = secondaryActions.isNotEmpty()

    Box(
        modifier = modifier
            .widthIn(max = if (isTablet) 520.dp else 420.dp)
            .fillMaxWidth()
            .height(buttonHeight),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(buttonHeight),
                shape = playShape,
                color = if (playEnabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (playEnabled) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            enabled = playEnabled,
                            onClick = {
                                onPlayClick()
                            },
                            onLongClick = onPlayLongClick,
                            role = Role.Button,
                        )
                        .height(buttonHeight),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = playPainter,
                        contentDescription = null,
                        modifier = Modifier.size(if (isTablet) 20.dp else 18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = playLabel,
                        style = if (isTablet) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.titleSmall
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (download != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    shape = playShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (download.enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                enabled = download.enabled && download.onClick != null,
                                onClick = { download.onClick?.invoke() },
                                onLongClick = null,
                                role = Role.Button,
                            )
                            .height(buttonHeight),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DownloadStatusGlyph(
                            status = download.status,
                            progress = download.progress,
                            modifier = Modifier.size(if (isTablet) 20.dp else 18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = download.label,
                            style = if (isTablet) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.titleSmall
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (pinnedAction != null) {
                Spacer(modifier = Modifier.width(12.dp))
                DetailIconAction(
                    label = pinnedAction.label,
                    icon = pinnedAction.icon,
                    active = pinnedAction.isActive,
                    progress = 1f,
                    size = iconButtonSize,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        pinnedAction.onClick()
                    },
                )
            }

            if (hasSecondaryActions) {
                Spacer(modifier = Modifier.width(12.dp))
                secondaryActions.forEachIndexed { index, action ->
                    Box(
                        modifier = Modifier
                            .width(iconButtonSize * menuProgress)
                            .height(iconButtonSize)
                            .graphicsLayer {
                                clip = true
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (actionsExpanded || menuProgress > 0.01f) {
                            DetailIconAction(
                                label = action.label,
                                icon = action.icon,
                                active = action.isActive,
                                progress = menuProgress,
                                size = iconButtonSize,
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    action.onClick()
                                },
                                onLongClick = action.onLongClick?.let { longClick ->
                                    {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        longClick()
                                    }
                                },
                            )
                        }
                    }

                    if (index != secondaryActions.lastIndex) {
                        Spacer(modifier = Modifier.width(12.dp * menuProgress))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp * menuProgress))
            }

            if (hasSecondaryActions) {
                Surface(
                    modifier = Modifier.size(iconButtonSize),
                    shape = CircleShape,
                    color = if (actionsExpanded) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
                    },
                    contentColor = if (actionsExpanded) {
                        MaterialTheme.colorScheme.background
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                ) {
                    Box(
                        modifier = Modifier
                            .size(iconButtonSize)
                            .clickable(role = Role.Button) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                actionsExpanded = !actionsExpanded
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = actionsMenuLabel,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    rotationZ = 90f * menuProgress
                                },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailActions(
    playLabel: String,
    playEnabled: Boolean,
    download: DetailDownloadAction? = null,
    isSaved: Boolean,
    isWatched: Boolean,
    isTablet: Boolean,
    shuffleEnabled: Boolean,
    onPlayClick: () -> Unit,
    onPlayLongClick: (() -> Unit)?,
    onShuffleClick: (() -> Unit)?,
    onWatchedClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveLongClick: (() -> Unit)?,
    onShareClick: (() -> Unit)? = null,
) {
    val shuffleAction = onShuffleClick?.let { onClick ->
        DetailSecondaryAction(
            label = stringResource(if (shuffleEnabled) Res.string.shuffle_stop else Res.string.random_episode_title),
            icon = Icons.Default.Shuffle,
            isActive = shuffleEnabled,
            onClick = onClick,
        )
    }
    DetailActionButtons(
        playLabel = if (playEnabled) playLabel else stringResource(Res.string.playback_unavailable),
        playEnabled = playEnabled,
        download = download,
        pinnedAction = shuffleAction?.takeIf { shuffleEnabled },
        secondaryActions = buildList {
            if (!shuffleEnabled) shuffleAction?.let(::add)
            onShareClick?.let { onClick ->
                add(
                    DetailSecondaryAction(
                        label = "Share",
                        icon = Icons.Default.Share,
                        onClick = onClick,
                    ),
                )
            }
            add(
                DetailSecondaryAction(
                    label = stringResource(if (isWatched) Res.string.hero_mark_unwatched else Res.string.hero_mark_watched),
                    icon = if (isWatched) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                    isActive = isWatched,
                    onClick = onWatchedClick,
                ),
            )
            add(
                DetailSecondaryAction(
                    label = stringResource(if (isSaved) Res.string.hero_remove_from_library else Res.string.hero_add_to_library),
                    icon = if (isSaved) Icons.Default.Check else Icons.Default.Add,
                    isActive = isSaved,
                    onClick = onSaveClick,
                    onLongClick = onSaveLongClick,
                ),
            )
        },
        isTablet = isTablet,
        onPlayClick = onPlayClick,
        onPlayLongClick = onPlayLongClick,
    )
}

/**
 * Status glyph for the hero download button: the plain download icon while nothing is tracked,
 * then a circular progress ring around a state colour while the transfer is running.
 *
 * Shared with the stream rows, which show the same state in a much smaller target.
 */
@Composable
internal fun DownloadStatusGlyph(
    status: DownloadStatus?,
    progress: Float,
    modifier: Modifier = Modifier,
    showPercent: Boolean = false,
) {
    when (status) {
        null -> Icon(
            imageVector = Icons.Default.Download,
            contentDescription = null,
            modifier = modifier,
            tint = MaterialTheme.colorScheme.primary,
        )

        DownloadStatus.Completed -> Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = modifier,
            tint = DownloadCompleteGreen,
        )

        DownloadStatus.Failed -> Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = modifier,
            tint = MaterialTheme.colorScheme.error,
        )

        DownloadStatus.Downloading, DownloadStatus.Paused -> {
            val animatedProgress by animateFloatAsState(
                targetValue = progress.coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 350),
                label = "downloadProgress",
            )
            val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
            val primary = MaterialTheme.colorScheme.primary
            val sweepBrush = if (status == DownloadStatus.Paused) {
                null
            } else {
                Brush.sweepGradient(listOf(primary, MaterialTheme.colorScheme.tertiary, primary))
            }
            val pausedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            val strokeWidth = 3.dp
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val stroke = strokeWidth.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    val sweep = animatedProgress * 360f
                    if (sweep > 0f) {
                        if (sweepBrush != null) {
                            drawArc(
                                brush = sweepBrush,
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round),
                            )
                        } else {
                            drawArc(
                                color = pausedColor,
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round),
                            )
                        }
                    }
                }
                if (showPercent) {
                    Text(
                        text = "${(animatedProgress * 100f).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailIconAction(
    label: String,
    icon: ImageVector,
    active: Boolean,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp,
    onLongClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.graphicsLayer {
            alpha = progress
            scaleX = 0.86f + (0.14f * progress)
            scaleY = 0.86f + (0.14f * progress)
        },
        shape = CircleShape,
        color = if (active) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (active) {
            MaterialTheme.colorScheme.background
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        tonalElevation = 6.dp,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    role = Role.Button,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}
