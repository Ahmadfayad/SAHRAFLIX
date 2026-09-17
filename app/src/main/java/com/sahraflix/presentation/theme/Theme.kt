package com.sahraflix.presentation.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme

private val SahraColors = darkColorScheme(
    primary = SahraGold,
    onPrimary = CinematicCharcoal,
    secondary = EmbersGlow,
    background = CinematicCharcoal,
    onBackground = CinemaWhite,
    surface = SurfaceCard,
    onSurface = CinemaWhite,
    onSurfaceVariant = MutedSilver,
    scrim = DeepShadow
)

private val SahraTypography = Typography(
    headlineLarge = TextStyle(color = CinemaWhite, fontSize = 32.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(color = CinemaWhite, fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(color = MutedSilver, fontSize = 16.sp),
    bodyMedium = TextStyle(color = MutedSilver, fontSize = 14.sp),
    labelMedium = TextStyle(color = MutedSilver, fontSize = 12.sp),
    labelLarge = TextStyle(color = CinematicCharcoal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
)

@Composable
fun SahraflixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SahraColors,
        typography = SahraTypography,
        content = content
    )
}
