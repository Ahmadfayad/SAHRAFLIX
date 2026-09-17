package com.sahraflix.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.Text
import com.sahraflix.presentation.components.StreamCard
import com.sahraflix.presentation.player.PlayerViewModel

@Composable
fun SearchScreen(
    detailId: String? = null,
    onOpenDetail: (String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val results = viewModel.results.collectAsLazyPagingItems()
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(if (detailId == null) "Search" else "Details")
        if (detailId == null) {
            BasicTextField(value = query, onValueChange = viewModel::setQuery, singleLine = true)
            Text(if (query.isBlank()) "Search live TV, movies, and series" else "Results")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(results.itemCount) { index ->
                    results[index]?.let { entry ->
                        StreamCard(entry, onClick = {
                            onOpenDetail(it.id.removePrefix("streaming:"))
                        })
                    }
                }
            }
        } else {
            Text("Catalog entry: $detailId")
        }
    }
}
