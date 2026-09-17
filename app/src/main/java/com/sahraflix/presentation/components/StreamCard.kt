package com.sahraflix.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.domain.model.ContentSource
import com.sahraflix.presentation.theme.CinematicCharcoal
import com.sahraflix.presentation.theme.MutedSilver
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

@Composable
fun StreamCard(
    stream: CatalogEntry,
    onClick: (CatalogEntry) -> Unit,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val source = (stream as? CatalogEntry.Iptv)?.source
        ?: (stream as? CatalogEntry.Streaming)?.source
        ?: ContentSource.VIDSRC_TMDB
    SahraFocusCard(
        onClick = { onClick(stream) },
        onFocusChanged = onFocusChanged,
        modifier = modifier
            .width(180.dp)
            .height(110.dp)
    ) {
            SubcomposeAsyncImage(
                model = stream.posterUrl,
                contentDescription = stream.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    val transition = rememberInfiniteTransition(label = "poster-shimmer")
                    val offset by transition.animateFloat(
                        initialValue = -1f,
                        targetValue = 2f,
                        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
                        label = "poster-shimmer-offset"
                    )
                    androidx.compose.foundation.layout.Box(
                        Modifier.fillMaxSize().background(
                            Brush.linearGradient(
                                listOf(CinematicCharcoal, MutedSilver.copy(alpha = 0.12f), CinematicCharcoal),
                                start = Offset(offset * 180f, 0f),
                                end = Offset((offset + 1f) * 180f, 0f)
                            )
                        )
                    )
                },
                error = {
                    androidx.compose.foundation.layout.Box(
                        Modifier.fillMaxSize().background(Color(0xFF1B2027))
                    )
                }
            )
            SourceBadge(source, modifier = Modifier.padding(6.dp))
    }
}
