package com.sahraflix.presentation.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.sahraflix.domain.model.ContentDetails
import com.sahraflix.presentation.player.PlayerViewModel

@Composable
fun CatalogDetailScreen(
    viewModel: CatalogDetailViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val details by viewModel.details.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (val content = details) {
            null -> Text("Loading details")
            is ContentDetails.Iptv -> Text(content.entry.title)
            is ContentDetails.Streaming -> {
                Text(content.entry.title)
                content.description?.let { Text(it) }
                Text("Rating: ${content.rating ?: "-"}  Year: ${content.releaseYear ?: "-"}")
                if (content.seasons.isEmpty()) {
                    Button(onClick = { viewModel.play { playerViewModel.playStream(it) } }) {
                        Text("Play")
                    }
                } else {
                    content.seasons.forEach { season ->
                        Text(season.name)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items((1..season.episodeCount).toList()) { episode ->
                                Button(onClick = {
                                    viewModel.play(season.seasonNumber, episode) { playerViewModel.playStream(it) }
                                }) { Text("Episode $episode") }
                            }
                        }
                    }
                }
            }
        }
    }
}
