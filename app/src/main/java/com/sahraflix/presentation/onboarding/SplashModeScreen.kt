package com.sahraflix.presentation.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahraflix.presentation.settings.UiMode
import com.sahraflix.presentation.theme.CinemaWhite
import com.sahraflix.presentation.theme.CinematicCharcoal
import com.sahraflix.presentation.theme.DeepShadow
import com.sahraflix.presentation.theme.EmbersGlow
import com.sahraflix.presentation.theme.MutedSilver
import com.sahraflix.presentation.theme.SahraGold
import com.sahraflix.presentation.theme.SurfaceCard

@Composable
fun SplashModeScreen(onComplete: (UiMode) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "fade"
    )

    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to DeepShadow,
                        0.5f to CinematicCharcoal,
                        1.0f to Color(0xFF0E0E18)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .alpha(contentAlpha)
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Brand name
            Text(
                text = "SAHRAFLIX",
                color = SahraGold,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 8.sp
            )
            Spacer(Modifier.height(6.dp))
            // Decorative gold rule
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, SahraGold, Color.Transparent)
                        )
                    )
            )
            Spacer(Modifier.height(36.dp))
            Text(
                text = "Choose your viewing experience",
                color = MutedSilver,
                fontSize = 17.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(52.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.Top
            ) {
                ModeCard(
                    icon = Icons.Default.Tv,
                    title = "TV Mode",
                    description = "D-pad navigation\noptimised for big screens",
                    accentColor = SahraGold,
                    onClick = { onComplete(UiMode.TV) }
                )
                ModeCard(
                    icon = Icons.Default.PhoneAndroid,
                    title = "Mobile Mode",
                    description = "Touch-first layout for\nphones & tablets",
                    accentColor = EmbersGlow,
                    onClick = { onComplete(UiMode.MOBILE) }
                )
            }

            Spacer(Modifier.height(44.dp))
            Text(
                text = "You can change this later in Settings",
                color = MutedSilver.copy(alpha = 0.45f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ModeCard(
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else if (isFocused) 1.04f else 1f,
        animationSpec = tween(120),
        label = "cardScale"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused || isPressed) 1f else 0.25f,
        animationSpec = tween(150),
        label = "borderAlpha"
    )

    Column(
        modifier = Modifier
            .scale(scale)
            .width(192.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceCard)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = accentColor.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accentColor.copy(alpha = if (isFocused) 0.18f else 0.09f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(40.dp)
            )
        }

        Text(
            text = title,
            color = CinemaWhite,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Text(
            text = description,
            color = MutedSilver,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
    }
}
