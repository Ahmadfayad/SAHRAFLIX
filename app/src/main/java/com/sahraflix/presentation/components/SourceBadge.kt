package com.sahraflix.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.sahraflix.domain.model.ContentSource

@Composable
fun SourceBadge(source: ContentSource, modifier: Modifier = Modifier) {
    val color = when (source) {
        ContentSource.IPTV_XTREAM -> Color(0xFF2474D8)
        ContentSource.IPTV_M3U -> Color(0xFF198754)
        ContentSource.IPTV_STALKER, ContentSource.IPTV_MAC -> Color(0xFFCC7A00)
        ContentSource.VIDSRC_TMDB -> Color(0xFF8244C7)
    }
    Text(
        text = source.label(),
        modifier = modifier
            .background(color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        color = Color.White
    )
}

private fun ContentSource.label(): String = when (this) {
    ContentSource.IPTV_XTREAM -> "Xtream"
    ContentSource.IPTV_M3U -> "M3U"
    ContentSource.IPTV_STALKER -> "Stalker"
    ContentSource.IPTV_MAC -> "MAC"
    ContentSource.VIDSRC_TMDB -> "TMDB"
}
