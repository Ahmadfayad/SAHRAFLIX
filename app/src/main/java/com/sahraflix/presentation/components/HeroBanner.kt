package com.sahraflix.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.presentation.theme.CinematicBlack
import com.sahraflix.presentation.theme.SahraGold
import com.sahraflix.presentation.theme.SahraRed

@Composable
fun HeroBanner(
    entry: CatalogEntry?,
    onPlayClick: (CatalogEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    if (entry == null) return

    val backdropUrl = entry.posterUrl
    val title = entry.title
    val rating = when (entry) {
        is CatalogEntry.Tmdb -> entry.rating?.let { "★ %.1f".format(it) } ?: "★ 8.5"
        else -> "★ 8.0"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(CinematicBlack)
    ) {
        // Backdrop Image
        AsyncImage(
            model = backdropUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient Scrim Overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            CinematicBlack.copy(alpha = 0.95f),
                            CinematicBlack.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            CinematicBlack.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        // Content Details
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(SahraGold, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    SahraText(
                        text = rating,
                        style = TextStyle(color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    )
                }
                SahraText(
                    text = "Featured Stream",
                    style = TextStyle(color = SahraRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SahraText(
                text = title,
                style = TextStyle(color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SahraButton(
                    onClick = { onPlayClick(entry) }
                ) {
                    SahraText("▶  Watch Now", style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
