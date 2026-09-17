package com.sahraflix.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text

@Composable
fun SearchScreen(detailId: String? = null) {
    var query by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(if (detailId == null) "Search" else "Details")
        if (detailId == null) {
            BasicTextField(value = query, onValueChange = { query = it }, singleLine = true)
            Text(if (query.isBlank()) "Search live TV, movies, and series" else "Searching for $query")
        } else {
            Text("Catalog entry: $detailId")
        }
    }
}
