package com.sahraflix.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme as M3Theme
import androidx.compose.material3.darkColorScheme as m3Dark
import androidx.tv.material3.MaterialTheme as TvTheme
import androidx.tv.material3.Typography as TvTypography
import androidx.tv.material3.darkColorScheme as tvDark

private val TvColors = tvDark(
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

// The app mixes TV Material (cards, buttons) with Compose Material3 (text fields, navigation,
// dialogs). Both need the dark palette, otherwise M3 widgets render in the default light theme.
private val M3Colors = m3Dark(
    primary = SahraGold,
    onPrimary = CinematicCharcoal,
    secondary = EmbersGlow,
    background = CinematicCharcoal,
    onBackground = CinemaWhite,
    surface = SurfaceCard,
    onSurface = CinemaWhite,
    surfaceVariant = CinematicCharcoal,
    onSurfaceVariant = MutedSilver,
    surfaceContainer = DeepShadow,
    secondaryContainer = GoldWash,
    onSecondaryContainer = SahraGold,
    scrim = DeepShadow
)

private val Type = TvTypography(
    displaySmall = TextStyle(color = CinemaWhite, fontSize = 36.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(color = CinemaWhite, fontSize = 32.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(color = CinemaWhite, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(color = CinemaWhite, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(color = CinemaWhite, fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(color = CinemaWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(color = MutedSilver, fontSize = 16.sp),
    bodyMedium = TextStyle(color = MutedSilver, fontSize = 14.sp),
    bodySmall = TextStyle(color = MutedSilver, fontSize = 12.sp),
    labelMedium = TextStyle(color = MutedSilver, fontSize = 12.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)
)

@Composable
fun SahraflixTheme(content: @Composable () -> Unit) {
    M3Theme(colorScheme = M3Colors) {
        TvTheme(colorScheme = TvColors, typography = Type, content = content)
    }
}
