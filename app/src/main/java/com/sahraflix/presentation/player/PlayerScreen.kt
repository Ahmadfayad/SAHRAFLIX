package com.sahraflix.presentation.player

import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.sahraflix.domain.model.TrackInfo
import com.sahraflix.domain.repository.PlayerState
import com.sahraflix.presentation.theme.SahraGold

private enum class Panel { AUDIO, SUBTITLES, QUALITY, SPEED, ZOOM }

/**
 * Full-screen player. Media3's PlayerView provides D-pad aware transport controls (seek, play/pause);
 * we add a track/speed/zoom panel, a buffering indicator and actionable error states.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    isInPip: Boolean,
    subtitleSettingsViewModel: SubtitleSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.player.playerState.collectAsState()
    val error by viewModel.player.lastError.collectAsState()
    val current by viewModel.player.current.collectAsState()
    val subtitleSettings by subtitleSettingsViewModel.settings.collectAsState()
    var panel by remember { mutableStateOf<Panel?>(null) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player.player
                    useController = true
                    controllerShowTimeoutMs = 4_000
                    setShowSubtitleButton(false)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER) // we draw our own
                    keepScreenOn = true
                    // Menu key opens our options panel.
                    setOnKeyListener { _, keyCode, event ->
                        if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_MENU) {
                            panel = Panel.AUDIO; true
                        } else false
                    }
                    requestFocus()
                }
            },
            update = { view ->
                view.player = viewModel.player.player
                view.resizeMode = resizeMode
                view.useController = !isInPip && panel == null
                view.subtitleView?.apply {
                    setStyle(
                        CaptionStyleCompat(
                            subtitleSettings.textColor,
                            android.graphics.Color.argb(subtitleSettings.backgroundOpacity * 255 / 100, 0, 0, 0),
                            android.graphics.Color.TRANSPARENT,
                            CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                            subtitleSettings.edgeColor,
                            null
                        )
                    )
                    setFractionalTextSize(
                        when (subtitleSettings.fontSize) { "Small" -> 0.045f; "Large" -> 0.075f; else -> 0.058f }
                    )
                }
            }
        )

        if (!isInPip) {
            if (state == PlayerState.BUFFERING || (state == PlayerState.IDLE && current != null && error == null)) {
                CircularProgressIndicator(color = SahraGold, modifier = Modifier.align(Alignment.Center).size(56.dp))
            }

            current?.let { req ->
                Row(
                    Modifier.align(Alignment.TopStart).padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(req.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    if (req.isLive) Text("● LIVE", color = Color(0xFFE53935), style = MaterialTheme.typography.labelLarge)
                }
                if (error == null && panel == null) {
                    Row(Modifier.align(Alignment.TopEnd).padding(20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { panel = Panel.AUDIO }) { Text("Audio & subtitles") }
                        OutlinedButton(onClick = { panel = Panel.QUALITY }) { Text("Quality") }
                        if (!req.isLive) OutlinedButton(onClick = { panel = Panel.SPEED }) { Text("Speed") }
                        OutlinedButton(onClick = { panel = Panel.ZOOM }) { Text("Aspect") }
                    }
                }
            }

            error?.let { failure ->
                val focus = remember { FocusRequester() }
                Column(
                    Modifier.align(Alignment.Center).widthIn(max = 560.dp)
                        .background(Color(0xE6101218), MaterialTheme.shapes.medium).padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Can't play this", style = MaterialTheme.typography.headlineSmall)
                    Text(failure.message, style = MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = viewModel::retry, modifier = Modifier.focusRequester(focus)) { Text("Try again") }
                        OutlinedButton(onClick = viewModel::openExternally) { Text("External player") }
                        OutlinedButton(onClick = viewModel::closePlayer) { Text("Back") }
                    }
                }
                LaunchedEffect(failure) { runCatching { focus.requestFocus() } }
            }

            panel?.let { p ->
                OptionsPanel(
                    panel = p,
                    viewModel = viewModel,
                    currentResize = resizeMode,
                    onResize = { resizeMode = it },
                    onSwitch = { panel = it },
                    onDismiss = { panel = null },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun OptionsPanel(
    panel: Panel,
    viewModel: PlayerViewModel,
    currentResize: Int,
    onResize: (Int) -> Unit,
    onSwitch: (Panel) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier
) {
    val player = viewModel.player
    val first = remember { FocusRequester() }
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    Column(
        modifier.widthIn(min = 320.dp, max = 380.dp).fillMaxHeight()
            .background(Color(0xF0101218)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Panel.AUDIO to "Audio", Panel.SUBTITLES to "Subs", Panel.QUALITY to "Quality").forEach { (p, label) ->
                if (p == panel) Button(onClick = {}) { Text(label) } else OutlinedButton(onClick = { onSwitch(p) }) { Text(label) }
            }
        }
        val options: List<Pair<String, () -> Unit>> = when (panel) {
            Panel.AUDIO -> player.getAvailableAudioTracks().map { t -> label(t) to { player.setAudioTrack(t.id); onDismiss() } }
            Panel.SUBTITLES -> listOf<Pair<String, () -> Unit>>("Off" to { player.setSubtitleTrack(null); onDismiss() }) +
                player.getAvailableSubtitleTracks().map { t -> label(t) to { player.setSubtitleTrack(t.id); onDismiss() } }
            Panel.QUALITY -> listOf<Pair<String, () -> Unit>>("Auto" to { player.setVideoTrack(null); onDismiss() }) +
                player.getAvailableVideoTracks().map { t -> label(t) to { player.setVideoTrack(t.id); onDismiss() } }
            Panel.SPEED -> listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).map { s -> "${s}×" to { player.setPlaybackSpeed(s); onDismiss() } }
            Panel.ZOOM -> listOf(
                "Fit" to AspectRatioFrameLayout.RESIZE_MODE_FIT,
                "Fill (crop)" to AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                "Stretch" to AspectRatioFrameLayout.RESIZE_MODE_FILL
            ).map { (name, mode) -> (if (mode == currentResize) "✓ $name" else name) to { onResize(mode); onDismiss() } }
        }
        if (options.isEmpty()) Text("No alternatives in this stream", style = MaterialTheme.typography.bodyMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(options.withIndex().toList(), key = { it.index }) { (i, option) ->
                OutlinedButton(
                    onClick = option.second,
                    modifier = if (i == 0) Modifier.focusRequester(first) else Modifier
                ) { Text(option.first) }
            }
        }
    }
    LaunchedEffect(panel) { runCatching { first.requestFocus() } }
}

private fun label(t: TrackInfo) = (if (t.selected) "✓ " else "") + t.name
