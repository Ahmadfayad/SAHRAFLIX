package com.sahraflix.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Text
import com.sahraflix.R
import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.domain.model.StreamType
import com.sahraflix.presentation.components.StreamCard
import com.sahraflix.presentation.player.PlayerViewModel
import com.sahraflix.domain.model.toCatalogEntry

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val isOnline by viewModel.isOnline.collectAsState()
    val liveStreams = viewModel.liveStreams.collectAsLazyPagingItems()
    val movieStreams = viewModel.movieStreams.collectAsLazyPagingItems()
    val seriesStreams = viewModel.seriesStreams.collectAsLazyPagingItems()
    val streamingMovies by viewModel.streamingMovies.collectAsState()
    val streamingShows by viewModel.streamingShows.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState(initial = emptyList())
    val favoriteLive by viewModel.favoriteLive.collectAsState(initial = emptyList())
    val epgHighlights by viewModel.epgHighlights.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isOnline) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFB00020), shape = RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.offline_notice),
                        color = Color.White
                    )
                }
            }
        }
        item { DashboardRail(stringResource(R.string.dashboard_continue_watching), continueWatching, playerViewModel) }
        item { DashboardRail(stringResource(R.string.dashboard_favorites), favoriteLive, playerViewModel) }
        item { DashboardRail(stringResource(R.string.dashboard_whats_on), epgHighlights, playerViewModel) }
        item {
            Text(stringResource(R.string.home_live_tv))
        }
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(liveStreams.itemCount) { index ->
                    liveStreams[index]?.let { stream ->
                        StreamCard(
                            stream = stream,
                            onClick = { playerViewModel.playCatalogEntry(it) },
                            onFocusChanged = { focused ->
                                val live = (stream as? CatalogEntry.Iptv)
                                    ?.stream?.streamType == StreamType.LIVE
                                if (live && focused) {
                                    (stream as? CatalogEntry.Iptv)
                                        ?.let { playerViewModel.previewStream(it.stream.streamUrl) }
                                }
                                else if (live && !focused) playerViewModel.stopPreview()
                            }
                        )
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.home_movies))
        }
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(movieStreams.itemCount) { index ->
                    movieStreams[index]?.let { stream ->
                        StreamCard(
                            stream = stream,
                            onClick = { playerViewModel.playCatalogEntry(it) }
                        )
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.home_series))
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(seriesStreams.itemCount) { index ->
                    seriesStreams[index]?.let { entry ->
                        StreamCard(entry, onClick = playerViewModel::playCatalogEntry)
                    }
                }
            }
        }
        item { Text(stringResource(R.string.home_streaming_movies)) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(streamingMovies) { content ->
                    StreamCard(
                        stream = content.toCatalogEntry(),
                        onClick = { playerViewModel.playStreamingContent(content) }
                    )
                }
            }
        }
        item { Text(stringResource(R.string.home_streaming_series)) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(streamingShows) { content ->
                    StreamCard(
                        stream = content.toCatalogEntry(),
                        onClick = { playerViewModel.playStreamingContent(content) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardRail(
    title: String,
    entries: List<CatalogEntry>,
    playerViewModel: PlayerViewModel
) {
    if (entries.isEmpty()) return
    Text(title)
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(entries) { entry ->
            StreamCard(entry, onClick = playerViewModel::playCatalogEntry)
        }
    }
}
