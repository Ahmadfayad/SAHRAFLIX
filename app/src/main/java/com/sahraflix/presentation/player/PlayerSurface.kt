package com.sahraflix.presentation.player

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.tv.material3.Text
import androidx.tv.material3.Button
import androidx.media3.ui.PlayerView
import androidx.media3.ui.CaptionStyleCompat
import androidx.hilt.navigation.compose.hiltViewModel
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient

@Composable
fun PlayerSurface(
    viewModel: PlayerViewModel,
    subtitleSettingsViewModel: SubtitleSettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val playerState by viewModel.player.playerState.collectAsState()
    val embedUrl by viewModel.embedUrl.collectAsState()
    val playerError by viewModel.playerError.collectAsState()
    val subtitleSettings by subtitleSettingsViewModel.settings.collectAsState()

    Box(modifier = modifier) {
    if (embedUrl != null) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context -> WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                webChromeClient = object : WebChromeClient() {
                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: android.os.Message?
                    ): Boolean = false
                }
                setBackgroundColor(Color.BLACK)
                settings.mediaPlaybackRequiresUserGesture = false
                loadUrl(embedUrl!!)
            }.also { webView ->
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        view.evaluateJavascript(
                            """(function(){var s=document.createElement('style');s.innerHTML='html,body{margin:0!important;background:#000!important;overflow:hidden!important}video,iframe{width:100vw!important;height:100vh!important}';document.head.appendChild(s);document.querySelectorAll('[class*=ad],[id*=ad],[class*=popup],[id*=popup]').forEach(function(e){e.remove()});})();""",
                            null
                        )
                    }
                }
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
                applySubtitleStyle(this, subtitleSettings)
            } },
            update = { playerView ->
                playerView.player = viewModel.player.player
                playerView.contentDescription = "Player: $playerState"
                applySubtitleStyle(playerView, subtitleSettings)
            }
        )
    }
        Button(
            onClick = { viewModel.playExternally() },
            modifier = Modifier.align(Alignment.TopEnd)
        ) { Text("Play externally") }
        playerError?.let { error ->
            Text(error, modifier = Modifier.fillMaxSize())
        }
    }
}

private fun applySubtitleStyle(playerView: PlayerView, settings: SubtitleSettings) {
    val textSize = when (settings.fontSize) {
        "Small" -> 0.045f
        "Large" -> 0.085f
        else -> 0.06f
    }
    playerView.subtitleView?.apply {
        setStyle(CaptionStyleCompat(
            settings.textColor,
            android.graphics.Color.argb(settings.backgroundOpacity * 255 / 100, 0, 0, 0),
            android.graphics.Color.TRANSPARENT,
            CaptionStyleCompat.EDGE_TYPE_OUTLINE,
            settings.edgeColor,
            null
        ))
        setFractionalTextSize(textSize)
    }
}
