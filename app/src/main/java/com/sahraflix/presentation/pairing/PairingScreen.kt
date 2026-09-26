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
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text

@Composable
fun PairingScreen(onDone: () -> Unit, viewModel: PairingViewModel = hiltViewModel()) {
    val source by viewModel.source.collectAsState()
    val pin by viewModel.pin.collectAsState()
    val url by viewModel.serverUrl.collectAsState()
    val status by viewModel.status.collectAsState()
    val qr = remember(url) { if (url.isBlank()) null else viewModel.qrBitmap(360) }

    LaunchedEffect(Unit) { viewModel.start() }

    Row(Modifier.fillMaxSize().padding(40.dp), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Pair from your phone", style = MaterialTheme.typography.headlineLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PairingSource.entries.forEach { item ->
                    if (item == source) Button(onClick = {}) { Text(item.name) }
                    else OutlinedButton(onClick = { viewModel.selectSource(item) }) { Text(item.name) }
                }
            }
            Text("1. Connect your phone to the same Wi-Fi network as this TV.", style = MaterialTheme.typography.bodyLarge)
            Text("2. Scan the QR code, or open:", style = MaterialTheme.typography.bodyLarge)
            Text(url, style = MaterialTheme.typography.titleMedium)
            Text("3. Enter this PIN on your phone:", style = MaterialTheme.typography.bodyLarge)
            Text(pin, style = MaterialTheme.typography.displaySmall)
            Text("The PIN is single-use and changes after 5 wrong attempts.", style = MaterialTheme.typography.bodySmall)
            when (val s = status) {
                PairingStatus.Waiting -> Text("Waiting for your phone…")
                PairingStatus.Saving -> Text("Checking your login…")
                is PairingStatus.Done -> {
                    Text("Added \"${s.name}\". Channels are downloading in the background.", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = onDone) { Text("Go to Home") }
                }
                is PairingStatus.Failed -> Text("Couldn't add it: ${s.message}", style = MaterialTheme.typography.titleMedium)
            }
        }
        qr?.let { Image(it.asImageBitmap(), "Pairing QR code", Modifier.size(360.dp)) }
    }
}
