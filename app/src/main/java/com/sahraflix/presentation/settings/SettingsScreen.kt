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
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Settings")
        Text("Profile security is managed from the profile gate.")
        androidx.tv.material3.Button(onClick = { viewModel.setRefreshEnabled(!refreshEnabled) }) {
            Text(if (refreshEnabled) "Background refresh: On" else "Background refresh: Off")
        }
        Text("TMDB catalog refresh runs automatically when network access is available.")
    }
}
