package com.sahraflix.presentation.player

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.tv.material3.Text
import androidx.media3.ui.PlayerView
import android.webkit.WebView
import android.webkit.WebViewClient

@Composable
fun PlayerSurface(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val playerState by viewModel.player.playerState.collectAsState()
    val embedUrl by viewModel.embedUrl.collectAsState()
    val playerError by viewModel.playerError.collectAsState()

    Box(modifier = modifier) {
    if (embedUrl != null) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context -> WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                setBackgroundColor(Color.BLACK)
                loadUrl(embedUrl!!)
            } },
            update = { webView -> if (webView.url != embedUrl) webView.loadUrl(embedUrl!!) }
        )
    } else {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context -> PlayerView(context).apply {
                useController = false
                setBackgroundColor(Color.TRANSPARENT)
                setShutterBackgroundColor(Color.TRANSPARENT)
                player = viewModel.player.player
            } },
            update = { playerView ->
                playerView.player = viewModel.player.player
                playerView.contentDescription = "Player: $playerState"
            }
        )
    }
        playerError?.let { error ->
            Text(error, modifier = Modifier.fillMaxSize())
        }
    }
}
