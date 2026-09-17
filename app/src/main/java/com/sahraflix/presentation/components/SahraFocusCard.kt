package com.sahraflix.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import com.sahraflix.presentation.theme.EmbersGlow
import com.sahraflix.presentation.theme.SahraGold

@Composable
fun SahraFocusCard(
    onClick: () -> Unit,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, label = "focus-scale")
    val shape = RoundedCornerShape(12.dp)
    Card(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged {
                focused = it.isFocused
                onFocusChanged?.invoke(it.isFocused)
            }
            .scale(scale)
            .shadow(if (focused) 16.dp else 2.dp, shape, ambientColor = EmbersGlow.copy(alpha = 0.40f), spotColor = EmbersGlow.copy(alpha = 0.40f))
            .border(if (focused) 2.dp else 1.dp, if (focused) SahraGold else Color.Transparent, shape)
            .focusable()
    ) { Box(Modifier.fillMaxSize()) { content() } }
}
