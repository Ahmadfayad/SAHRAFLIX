package com.sahraflix.presentation.epg

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text

@Composable
fun EpgGuideScreen() {
    Text("EPG Guide", modifier = Modifier.fillMaxSize().padding(40.dp))
}
