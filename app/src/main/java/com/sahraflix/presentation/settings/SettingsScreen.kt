package com.sahraflix.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val refreshEnabled by viewModel.refreshEnabled.collectAsState()
    val uiMode by viewModel.uiMode.collectAsState()
    val hardwareAcceleration by viewModel.hardwareAcceleration.collectAsState()
    val defaultPlayer by viewModel.defaultPlayer.collectAsState()
    val updateIntervalHours by viewModel.updateIntervalHours.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Settings")
        Text("Profile security is managed from the profile gate.")
        Text("UI mode: ${uiMode.name}")
        androidx.tv.material3.Button(onClick = {
            viewModel.setUiMode(
                when (uiMode) {
                    UiMode.AUTO -> UiMode.TV
                    UiMode.TV -> UiMode.MOBILE
                    UiMode.MOBILE -> UiMode.AUTO
                }
            )
        }) { Text("Cycle UI mode") }
        androidx.tv.material3.Button(onClick = { viewModel.setRefreshEnabled(!refreshEnabled) }) {
            Text(if (refreshEnabled) "Background refresh: On" else "Background refresh: Off")
        }
        androidx.tv.material3.Button(onClick = { viewModel.setHardwareAcceleration(!hardwareAcceleration) }) {
            Text("Hardware acceleration: ${if (hardwareAcceleration) "On" else "Off"}")
        }
        androidx.tv.material3.Button(onClick = viewModel::cycleDefaultPlayer) {
            Text("Default player: $defaultPlayer")
        }
        androidx.tv.material3.Button(onClick = viewModel::cycleUpdateInterval) {
            Text("Update interval: ${updateIntervalHours}h")
        }
        androidx.tv.material3.Button(onClick = viewModel::clearCache) {
            Text("Clear cache")
        }
        Text("TMDB catalog refresh runs automatically when network access is available.")
    }
}
