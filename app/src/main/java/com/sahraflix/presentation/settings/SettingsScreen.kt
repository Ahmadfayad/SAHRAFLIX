package com.sahraflix.presentation.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text

@Composable
fun SettingsScreen() {
    Text("Settings", modifier = Modifier.fillMaxSize().padding(40.dp))
}
