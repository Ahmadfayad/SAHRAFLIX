package com.sahraflix.presentation.pairing

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Text

@Composable
fun PairingScreen(viewModel: PairingViewModel = hiltViewModel()) {
    val source by viewModel.source.collectAsState()
    val pin by viewModel.pin.collectAsState()
    val url by viewModel.serverUrl.collectAsState()
    val paired by viewModel.paired.collectAsState()
    val qr = remember(url) { if (url.isBlank()) null else viewModel.qrBitmap(360) }

    LaunchedEffect(Unit) { viewModel.start() }
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Add playlist")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PairingSource.values().forEach { item ->
                Button(onClick = { viewModel.selectSource(item) }) {
                    Text(if (item == source) "[ ${item.name} ]" else item.name)
                }
            }
        }
        Text("Open this page on your phone")
        Text(url)
        Text("TV PIN: $pin")
        qr?.let { Image(it.asImageBitmap(), "Pairing QR", Modifier.size(360.dp)) }
        if (paired) Text("Playlist received. Sync will begin shortly.")
    }
}
