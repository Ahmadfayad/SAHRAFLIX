package com.sahraflix.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.sahraflix.presentation.theme.CinematicCharcoal
import com.sahraflix.presentation.theme.DeepShadow
import com.sahraflix.presentation.theme.GoldWash
import com.sahraflix.presentation.theme.SurfaceCard

fun Modifier.sahraCinematicGradient(): Modifier = background(
    Brush.verticalGradient(listOf(CinematicCharcoal, SurfaceCard, DeepShadow))
).background(
    Brush.radialGradient(listOf(GoldWash, androidx.compose.ui.graphics.Color.Transparent))
)

fun Modifier.sahraLoadingBackground(): Modifier = background(DeepShadow)

@Composable
fun HeroScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(androidx.compose.ui.graphics.Color.Transparent, DeepShadow.copy(alpha = 0.8f))
            )
        )
    )
}
