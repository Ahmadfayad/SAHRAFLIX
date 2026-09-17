package com.sahraflix.presentation.epg

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.remember
import java.util.concurrent.TimeUnit
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text

@Composable
fun EpgGuideScreen(viewModel: EpgViewModel = hiltViewModel()) {
    val playlists by viewModel.playlists.collectAsState(initial = emptyList())
    val selected = playlists.firstOrNull()
    val channels = viewModel.channels(selected?.id.orEmpty()).collectAsLazyPagingItems()
    val now = remember { System.currentTimeMillis() }
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("EPG Guide")
        if (playlists.isEmpty()) {
            Text("No playlists configured")
        } else {
            Text("Playlist: ${selected?.name}")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(playlists) { playlist -> Text(playlist.name) }
            }
            EpgTimeline(
                channels = channels,
                viewModel = viewModel,
                windowStart = now,
                windowEnd = now + TimeUnit.HOURS.toMillis(6),
                onProgramClick = { }
            )
        }
    }
}
