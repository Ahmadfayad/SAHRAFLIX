package com.sahraflix.presentation.epg

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.sahraflix.presentation.player.LocalPlayerViewModel
import java.util.concurrent.TimeUnit

@Composable
fun EpgGuideScreen(viewModel: EpgViewModel = hiltViewModel()) {
    val player = LocalPlayerViewModel.current
    val playlists by viewModel.playlists.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val channels = viewModel.channels.collectAsLazyPagingItems()
    // Window: 2h back (catch-up) to 6h ahead, aligned to the half hour.
    val windowStart = remember {
        val now = System.currentTimeMillis()
        now - now % TimeUnit.MINUTES.toMillis(30) - TimeUnit.HOURS.toMillis(2)
    }
    val windowEnd = windowStart + TimeUnit.HOURS.toMillis(8)

    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("TV guide", style = MaterialTheme.typography.headlineLarge)
        if (playlists.size > 1) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (selected == null) Button(onClick = {}) { Text("All") } else OutlinedButton(onClick = { viewModel.select(null) }) { Text("All") }
            playlists.forEach { p ->
                if (p.id == selected) Button(onClick = {}) { Text(p.name) } else OutlinedButton(onClick = { viewModel.select(p.id) }) { Text(p.name) }
            }
        }
        if (channels.itemCount == 0) {
            Text("No live channels yet. Add a playlist in Settings.", style = MaterialTheme.typography.bodyLarge)
        } else {
            EpgTimeline(
                channels = channels,
                viewModel = viewModel,
                windowStart = windowStart,
                windowEnd = windowEnd,
                onChannelClick = { player.play(it) },
                onProgramClick = { channel, program ->
                    val now = System.currentTimeMillis()
                    val hasArchive = channel.stream.catchupSource != null || channel.stream.catchupType == "shift"
                    if (program.isPast(now) && hasArchive) {
                        player.playCatchup(channel.id, program.startTime, program.endTime)
                    } else {
                        player.play(channel) // live now, future, or no archive → live channel
                    }
                }
            )
        }
    }
}
