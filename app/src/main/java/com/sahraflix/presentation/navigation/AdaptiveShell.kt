package com.sahraflix.presentation.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AdaptiveShell(isTvMode: Boolean, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.weight(1f)) {
            if (isTvMode) {
                NavigationRail {
                    NavigationRailItem(selected = true, onClick = {}, icon = { Text("H") })
                    NavigationRailItem(selected = false, onClick = {}, icon = { Text("S") })
                }
            }
            content()
        }
        if (!isTvMode) {
            NavigationBar {
                NavigationBarItem(selected = true, onClick = {}, icon = { Text("H") }, label = { Text("Home") })
                NavigationBarItem(selected = false, onClick = {}, icon = { Text("S") }, label = { Text("Search") })
            }
        }
    }
}
