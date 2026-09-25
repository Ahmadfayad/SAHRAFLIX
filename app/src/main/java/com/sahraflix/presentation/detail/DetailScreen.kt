package com.sahraflix.presentation.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.sahraflix.domain.model.ContentDetails
import com.sahraflix.domain.model.WatchProvider
import com.sahraflix.presentation.components.RailTitle
import com.sahraflix.presentation.components.StreamCard
import com.sahraflix.presentation.player.LocalPlayerViewModel
import com.sahraflix.presentation.theme.DeepShadow
import com.sahraflix.presentation.theme.SahraGold

@Composable
fun DetailScreen(viewModel: DetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            DetailState.Loading -> CircularProgressIndicator(color = SahraGold, modifier = Modifier.align(Alignment.Center))
            is DetailState.Failed -> Column(Modifier.align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(s.message, style = MaterialTheme.typography.titleMedium)
                Button(onClick = viewModel::load) { Text("Try again") }
            }
            is DetailState.Loaded -> DetailContent(s.details)
        }
    }
}

@Composable
private fun DetailContent(details: ContentDetails) {
    val player = LocalPlayerViewModel.current
    val context = LocalContext.current
    Box(Modifier.fillMaxSize()) {
        details.backdropUrl?.let {
            AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.35f)
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(DeepShadow, DeepShadow.copy(alpha = 0.6f), Color.Transparent))))
        LazyColumn(
            contentPadding = PaddingValues(48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    details.posterUrl?.let {
                        AsyncImage(it, details.title, Modifier.width(180.dp).height(270.dp), contentScale = ContentScale.Crop)
                    }
                    Column(Modifier.widthIn(max = 680.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(details.title, style = MaterialTheme.typography.displaySmall)
                        Text(metaLine(details), style = MaterialTheme.typography.bodyMedium)
                        details.description?.let { Text(it, maxLines = 6, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge) }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            when (details) {
                                is ContentDetails.Iptv -> if (details.seasons.isEmpty()) {
                                    Button(onClick = { player.play(details.entry) }) { Text("Play") }
                                    OutlinedButton(onClick = { player.playFromStart(details.entry) }) { Text("From start") }
                                    OutlinedButton(onClick = { player.toggleFavorite(details.entry) }) { Text("Favourite") }
                                }
                                is ContentDetails.Tmdb -> {
                                    details.libraryMatches.firstOrNull()?.let { match ->
                                        if (!details.entry.isSeries) Button(onClick = { player.play(match) }) { Text("Play from your library") }
                                    }
                                    details.trailerYoutubeKey?.let { key ->
                                        OutlinedButton(onClick = {
                                            val app = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$key"))
                                            val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$key"))
                                            try { context.startActivity(app) } catch (_: ActivityNotFoundException) {
                                                runCatching { context.startActivity(web) }
                                            }
                                        }) { Text("Trailer") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            when (details) {
                is ContentDetails.Iptv -> episodes(details, onPlay = { ep -> player.playEpisode(details.entry, ep) })
                is ContentDetails.Tmdb -> tmdbSections(details)
            }
        }
    }
}

private fun metaLine(d: ContentDetails): String = when (d) {
    is ContentDetails.Iptv -> listOfNotNull(d.releaseYear?.toString(), d.rating?.let { "★ %.1f".format(it) },
        d.seasons.takeIf { it.isNotEmpty() }?.let { "${it.size} season(s)" }).joinToString("  ·  ")
    is ContentDetails.Tmdb -> listOfNotNull(d.releaseYear?.toString(), d.rating?.let { "★ %.1f".format(it) },
        d.runtimeMinutes?.let { "${it} min" }, d.genres.take(3).joinToString(", ").ifBlank { null }).joinToString("  ·  ")
}

private fun androidx.compose.foundation.lazy.LazyListScope.episodes(
    d: ContentDetails.Iptv,
    onPlay: (com.sahraflix.domain.model.IptvEpisode) -> Unit
) {
    if (d.seasons.isEmpty()) return
    item(key = "episodes") {
        var selected by remember { mutableIntStateOf(d.seasons.keys.min()) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(d.seasons.keys.sorted()) { season ->
                    if (season == selected) Button(onClick = {}) { Text("Season $season") }
                    else OutlinedButton(onClick = { selected = season }) { Text("Season $season") }
                }
            }
            d.seasons[selected].orEmpty().forEach { ep ->
                OutlinedButton(onClick = { onPlay(ep) }, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("${ep.episode}. ${ep.title}", style = MaterialTheme.typography.titleSmall)
                        ep.plot?.let { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.tmdbSections(d: ContentDetails.Tmdb) {
    if (d.libraryMatches.isNotEmpty()) item(key = "lib") {
        val player = LocalPlayerViewModel.current
        Column {
            RailTitle("In your library")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(d.libraryMatches, key = { it.id }) { StreamCard(it, onClick = { e ->
                    val iptv = e as com.sahraflix.domain.model.CatalogEntry.Iptv
                    player.play(iptv)
                }) }
            }
        }
    }
    item(key = "where") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RailTitle("Where to watch")
            val wp = d.watchProviders
            if (wp == null || wp.isEmpty) {
                Text("No licensed streaming options found for your region.", style = MaterialTheme.typography.bodyMedium)
            } else {
                ProviderRow("Stream", wp.stream)
                ProviderRow("Rent", wp.rent)
                ProviderRow("Buy", wp.buy)
                Text("Availability data from JustWatch via TMDB (${wp.region}).", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (d.cast.isNotEmpty()) item(key = "cast") {
        Column {
            RailTitle("Cast")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(d.cast, key = { it.id }) { c ->
                    Column(Modifier.width(110.dp)) {
                        AsyncImage(c.profileUrl, c.name, Modifier.size(110.dp, 150.dp), contentScale = ContentScale.Crop)
                        Text(c.name, maxLines = 1, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        c.character?.let { Text(it, maxLines = 1, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
    if (d.seasons.isNotEmpty()) item(key = "seasons") {
        Text(d.seasons.joinToString("  ·  ") { "${it.name} (${it.episodeCount} ep)" }, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ProviderRow(label: String, providers: List<WatchProvider>) {
    if (providers.isEmpty()) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, modifier = Modifier.width(70.dp), style = MaterialTheme.typography.titleSmall)
        providers.take(8).forEach { p ->
            AsyncImage(p.logoUrl, p.name, Modifier.size(44.dp))
        }
    }
}
