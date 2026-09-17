package com.sahraflix.presentation.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Text

@Composable
fun SubtitleSettingsScreen(viewModel: SubtitleSettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Subtitle settings")
        Button(onClick = {
            viewModel.setFontSize(
                when (settings.fontSize) {
                    "Small" -> "Normal"
                    "Normal" -> "Large"
                    else -> "Small"
                }
            )
        }) { Text("Font size: ${settings.fontSize}") }
        Button(onClick = {
            viewModel.setBackgroundOpacity((settings.backgroundOpacity + 25) % 125)
        }) { Text("Background opacity: ${settings.backgroundOpacity}%") }
        Button(onClick = {
            viewModel.setTextColor(
                if (settings.textColor == android.graphics.Color.WHITE) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
            )
        }) { Text("Text color: ${if (settings.textColor == android.graphics.Color.YELLOW) "Yellow" else "White"}") }
        Button(onClick = {
            viewModel.setEdgeColor(
                if (settings.edgeColor == android.graphics.Color.BLACK) android.graphics.Color.DKGRAY
                else android.graphics.Color.BLACK
            )
        }) { Text("Edge color: ${if (settings.edgeColor == android.graphics.Color.DKGRAY) "Dark gray" else "Black"}") }
    }
}
