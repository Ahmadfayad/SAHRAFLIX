package com.sahraflix.presentation.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.media3.common.Player

@Composable
fun PreviewSurface(player: Player, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.width(360.dp).height(202.dp).padding(16.dp),
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                this.player = player
            }
        }
    )
}
