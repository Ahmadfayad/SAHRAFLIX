package com.sahraflix.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.domain.model.StreamType
import com.sahraflix.presentation.components.EntryRail
import com.sahraflix.presentation.player.LocalPlayerViewModel

@Composable
fun SearchScreen(onOpenDetail: (CatalogEntry) -> Unit, viewModel: SearchViewModel = hiltViewModel()) {
    val player = LocalPlayerViewModel.current
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val open: (CatalogEntry) -> Unit = { e ->
        when (e) {
            is CatalogEntry.Iptv -> if (e.stream.streamType == StreamType.SERIES) onOpenDetail(e) else player.play(e)
            is CatalogEntry.Tmdb -> onOpenDetail(e)
        }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                singleLine = true,
                label = { androidx.compose.material3.Text("Search channels, movies, series, programmes") },
                modifier = Modifier.widthIn(min = 420.dp, max = 720.dp)
            )
        }
        val empty = results.channels.isEmpty() && results.vod.isEmpty() && results.programmes.isEmpty() && results.tmdb.isEmpty()
        if (query.trim().length >= 2 && empty) item { Text("No results for \"$query\"", style = MaterialTheme.typography.bodyLarge) }
        item { EntryRail("Channels", results.channels, open, onLongClick = { player.toggleFavorite(it) }) }
        item { EntryRail("Movies & series in your playlists", results.vod, open) }
        item {
            EntryRail("In the TV guide", results.programmes.map { it.first }.distinctBy { it.id }, open,
                subtitle = { e -> results.programmes.firstOrNull { it.first.id == e.id }?.second })
        }
        item { EntryRail("More titles (TMDB)", results.tmdb, open) }
    }
}
