package com.nuvio.app.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.episodes_cd_watched
import org.jetbrains.compose.resources.stringResource

@Composable
fun NuvioWatchedBadge(
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette
    Box(
        modifier = modifier
            .size(NuvioTokens.Icon.md)
            .clip(tokens.shapes.avatar)
            .background(palette.accentBrush()),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = stringResource(Res.string.episodes_cd_watched),
            tint = palette.onSecondary,
            modifier = Modifier.size(NuvioTokens.Icon.xs),
        )
    }
}

@Composable
fun NuvioAnimatedWatchedBadge(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        NuvioWatchedBadge()
    }
}

@Composable
fun BoxScope.NuvioPosterWatchedOverlay(
    isWatched: Boolean,
    modifier: Modifier = Modifier,
    padding: Dp = NuvioTokens.Space.s6,
) {
    NuvioAnimatedWatchedBadge(
        isVisible = isWatched,
        modifier = modifier
            .align(Alignment.TopEnd)
            .padding(padding),
    )
}

/**
 * Small pills on the top-left of a poster telling what is inside the item:
 * a season range ("S1-S4") in the accent colour and a language label
 * ("Bangla") on a translucent dark chip. The watched mark sits top-right,
 * so the two never overlap.
 */
@Composable
fun BoxScope.NuvioPosterBadges(
    seasonBadge: String?,
    languageBadge: String?,
    extraBadge: String? = null,
    modifier: Modifier = Modifier,
) {
    val badges = listOfNotNull(
        seasonBadge?.let { it to true },
        extraBadge?.let { it to true },
        languageBadge?.let { it to false },
    )
    if (badges.isEmpty()) return
    val palette = MaterialTheme.themePalette
    Row(
        modifier = modifier
            .align(Alignment.TopStart)
            .padding(NuvioTokens.Space.s6),
        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s4),
    ) {
        badges.forEach { (text, isSeason) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(NuvioTokens.Space.s4))
                    .background(
                        if (isSeason) {
                            palette.accentBrush()
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.72f),
                                    Color.Black.copy(alpha = 0.72f),
                                ),
                            )
                        },
                    )
                    .padding(horizontal = NuvioTokens.Space.s5, vertical = NuvioTokens.Space.s3),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSeason) palette.onSecondary else Color.White,
                    maxLines = 1,
                )
            }
        }
    }
}
