package com.sahraflix.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import coil.compose.SubcomposeAsyncImage
import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.domain.model.ContentSource

@Composable
fun StreamCard(
    stream: CatalogEntry,
    onClick: (CatalogEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f,
        label = "stream-card-scale"
    )
    val shape = RoundedCornerShape(8.dp)
    val source = (stream as? CatalogEntry.Iptv)?.source
        ?: (stream as? CatalogEntry.Streaming)?.source
        ?: ContentSource.VIDSRC_TMDB
    val sourceColor = when (source) {
        ContentSource.IPTV_XTREAM -> Color(0xFF2474D8)
        ContentSource.IPTV_M3U -> Color(0xFF198754)
        ContentSource.IPTV_STALKER, ContentSource.IPTV_MAC -> Color(0xFFCC7A00)
        ContentSource.VIDSRC_TMDB -> Color(0xFF8244C7)
    }

    Card(
        onClick = { onClick(stream) },
        modifier = modifier
            .width(180.dp)
            .height(110.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 14.dp else 2.dp,
                shape = shape
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else sourceColor,
                shape = shape
            )
            .clip(shape)
            .focusable()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SubcomposeAsyncImage(
                model = stream.posterUrl,
                contentDescription = stream.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    androidx.compose.foundation.layout.Box(
                        Modifier.fillMaxSize().background(Color(0xFF252A31))
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
}
