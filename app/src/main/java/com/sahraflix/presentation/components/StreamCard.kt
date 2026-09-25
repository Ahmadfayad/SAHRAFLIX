package com.sahraflix.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.SubcomposeAsyncImage
import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.domain.model.StreamType
import com.sahraflix.presentation.theme.CinematicCharcoal
import com.sahraflix.presentation.theme.CinemaWhite
import com.sahraflix.presentation.theme.DeepShadow
import com.sahraflix.presentation.theme.SahraGold

enum class CardStyle { LANDSCAPE, POSTER }

fun CatalogEntry.cardStyle(): CardStyle = when (this) {
    is CatalogEntry.Iptv -> if (stream.streamType == StreamType.LIVE) CardStyle.LANDSCAPE else CardStyle.POSTER
    is CatalogEntry.Tmdb -> CardStyle.POSTER
}

/**
 * Channel (landscape, logo on dark plate) or movie/series (poster) card with a title fallback
 * when artwork is missing — IPTV logos are frequently broken links.
 */
@Composable
fun StreamCard(
    entry: CatalogEntry,
    onClick: (CatalogEntry) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    progress: Float? = null,
    onLongClick: ((CatalogEntry) -> Unit)? = null,
    onFocused: ((CatalogEntry) -> Unit)? = null
) {
    val style = entry.cardStyle()
    val size = if (style == CardStyle.LANDSCAPE) Modifier.width(200.dp).height(112.dp) else Modifier.width(140.dp).height(210.dp)
    Column(modifier = modifier.width(if (style == CardStyle.LANDSCAPE) 200.dp else 140.dp)) {
        SahraFocusCard(
            onClick = { onClick(entry) },
            onLongClick = onLongClick?.let { { it(entry) } },
            onFocusChanged = { if (it) onFocused?.invoke(entry) },
            modifier = size
        ) {
            val fallback: @Composable () -> Unit = {
                Box(
                    Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CinematicCharcoal, DeepShadow))).padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(entry.title, maxLines = 3, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleSmall, color = CinemaWhite)
                }
            }
            if (entry.posterUrl.isNullOrBlank()) fallback() else SubcomposeAsyncImage(
                model = entry.posterUrl,
                contentDescription = entry.title,
                modifier = Modifier.fillMaxSize().then(if (style == CardStyle.LANDSCAPE) Modifier.padding(18.dp) else Modifier),
                contentScale = if (style == CardStyle.LANDSCAPE) ContentScale.Fit else ContentScale.Crop,
                loading = { Box(Modifier.fillMaxSize().background(CinematicCharcoal)) },
                error = { fallback() }
            )
            if (progress != null && progress > 0f) {
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Color.Black.copy(alpha = 0.6f))) {
                    Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).background(SahraGold, RoundedCornerShape(2.dp)))
                }
            }
        }
        Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium,
            color = CinemaWhite, modifier = Modifier.padding(top = 6.dp, start = 2.dp))
        subtitle?.let {
            Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 2.dp))
        }
    }
}
