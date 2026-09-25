package com.sahraflix.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.sahraflix.data.local.entity.PlaylistEntity
import com.sahraflix.data.local.entity.PlaylistType
import com.sahraflix.data.local.entity.SyncState
import com.sahraflix.presentation.theme.CinematicCharcoal
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsScreen(
    onPair: () -> Unit,
    onSubtitles: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlistList.collectAsState()
    val refresh by viewModel.refreshEnabled.collectAsState()
    val interval by viewModel.updateIntervalHours.collectAsState()
    val uiMode by viewModel.uiMode.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val message by viewModel.message.collectAsState()
    var showForm by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineLarge) }
        message?.let { m -> item { Text(m, style = MaterialTheme.typography.titleMedium) } }

        item { Text("Playlists", style = MaterialTheme.typography.titleLarge) }
        if (playlists.isEmpty()) item { Text("No playlists yet.", style = MaterialTheme.typography.bodyLarge) }
        items(playlists, key = { it.id }) { p -> PlaylistRow(p, viewModel::refresh, viewModel::remove) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { showForm = !showForm }) { Text(if (showForm) "Close form" else "Add on this TV") }
                OutlinedButton(onClick = onPair) { Text("Pair from phone (QR)") }
                OutlinedButton(onClick = viewModel::addDemoServer, enabled = !busy) { Text("Add demo server") }
            }
        }
        item {
            Text("Demo server: run tools/test-server/server.py on your computer; the app connects to ${viewModel.demoServerUrl}.",
                style = MaterialTheme.typography.bodySmall)
        }
        if (showForm) item { AddPlaylistForm(busy) { t, n, u, us, pw, mac, epg, ua -> viewModel.addPlaylist(t, n, u, us, pw, mac, epg, ua) } }

        item { Text("Updates", style = MaterialTheme.typography.titleLarge) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { viewModel.setRefreshEnabled(!refresh) }) {
                    Text("Automatic refresh: ${if (refresh) "On (Wi-Fi/Ethernet)" else "Off"}")
                }
                OutlinedButton(onClick = viewModel::cycleUpdateInterval, enabled = refresh) { Text("Every $interval h") }
            }
        }

        item { Text("Display & playback", style = MaterialTheme.typography.titleLarge) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                UiMode.entries.forEach { m ->
                    if (m == uiMode) Button(onClick = {}) { Text("Layout: ${m.name}") }
                    else OutlinedButton(onClick = { viewModel.setUiMode(m) }) { Text(m.name) }
                }
                OutlinedButton(onClick = onSubtitles) { Text("Subtitle style") }
                OutlinedButton(onClick = viewModel::clearCache) { Text("Clear image cache") }
            }
        }

        item { Text("About", style = MaterialTheme.typography.titleLarge) }
        item {
            Text(
                "SahraFlix plays IPTV sources you provide. Movie & series info, artwork and \"where to watch\" come from TMDB " +
                    (if (viewModel.tmdbConfigured) "(configured)." else "(not configured: add TMDB_API_KEY to local.properties).") +
                    " This product uses the TMDB API but is not endorsed or certified by TMDB.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PlaylistRow(p: PlaylistEntity, onRefresh: (PlaylistEntity) -> Unit, onRemove: (PlaylistEntity) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(CinematicCharcoal, MaterialTheme.shapes.small).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text("${p.name}  ·  ${p.type.name}", style = MaterialTheme.typography.titleMedium)
            val status = when (p.syncState) {
                SyncState.NEVER -> "Waiting to sync"
                SyncState.RUNNING -> p.syncMessage ?: "Syncing…"
                SyncState.OK -> "${p.itemCount} items · updated ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(p.lastUpdated))}" +
                    (p.syncMessage?.let { " · $it" } ?: "")
                SyncState.FAILED -> "Failed: ${p.syncMessage}"
            }
            Text(status, style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick = { onRefresh(p) }, enabled = p.syncState != SyncState.RUNNING) { Text("Refresh") }
        OutlinedButton(onClick = { onRemove(p) }) { Text("Remove") }
    }
}

@Composable
private fun AddPlaylistForm(
    busy: Boolean,
    onSubmit: (PlaylistType, String, String, String, String, String, String, String) -> Unit
) {
    var type by rememberSaveable { mutableStateOf(PlaylistType.M3U) }
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var user by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var mac by rememberSaveable { mutableStateOf("") }
    var epg by rememberSaveable { mutableStateOf("") }
    var ua by rememberSaveable { mutableStateOf("") }
    Column(Modifier.widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlaylistType.entries.forEach { t ->
                if (t == type) Button(onClick = {}) { Text(t.name) } else OutlinedButton(onClick = { type = t }) { Text(t.name) }
            }
        }
        Field(name, { name = it }, "Name (optional)")
        Field(url, { url = it }, when (type) {
            PlaylistType.M3U -> "Playlist URL (http://…/playlist.m3u)"
            PlaylistType.XTREAM -> "Server (http://host:port)"
            PlaylistType.STALKER -> "Portal URL (http://host:port/c/)"
        }, KeyboardType.Uri)
        if (type == PlaylistType.XTREAM) {
            Field(user, { user = it }, "Username")
            Field(pass, { pass = it }, "Password", password = true)
        }
        if (type == PlaylistType.STALKER) Field(mac, { mac = it }, "MAC address (00:1A:79:…)")
        if (type != PlaylistType.XTREAM) Field(epg, { epg = it }, "EPG / XMLTV URL (optional)", KeyboardType.Uri)
        Field(ua, { ua = it }, "Custom User-Agent (optional)")
        Button(onClick = { onSubmit(type, name, url, user, pass, mac, epg, ua) }, enabled = !busy && url.isNotBlank()) {
            Text(if (busy) "Checking…" else "Save & sync")
        }
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, keyboard: KeyboardType = KeyboardType.Text, password: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onChange, singleLine = true,
        label = { androidx.compose.material3.Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth()
    )
}
