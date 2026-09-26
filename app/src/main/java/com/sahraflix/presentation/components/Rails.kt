package com.sahraflix.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.sahraflix.domain.model.CatalogEntry

@Composable
fun RailTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
}

private val railPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)

@Composable
fun EntryRail(
    title: String,
    entries: List<CatalogEntry>,
    onClick: (CatalogEntry) -> Unit,
    onLongClick: ((CatalogEntry) -> Unit)? = null,
    subtitle: (CatalogEntry) -> String? = { null },
    progress: (CatalogEntry) -> Float? = { null }
) {
    if (entries.isEmpty()) return
    Column {
        RailTitle(title)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = railPadding) {
            items(entries, key = { it.id }) { entry ->
                StreamCard(entry, onClick, subtitle = subtitle(entry), progress = progress(entry), onLongClick = onLongClick)
            }
        }
    }
}

@Composable
fun PagedRail(
    title: String,
    items: LazyPagingItems<CatalogEntry>,
    onClick: (CatalogEntry) -> Unit,
    onLongClick: ((CatalogEntry) -> Unit)? = null
) {
    if (items.itemCount == 0) return
    Column {
        RailTitle(title)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = railPadding) {
            items(count = items.itemCount, key = items.itemKey { it.id }) { index ->
                items[index]?.let { StreamCard(it, onClick, onLongClick = onLongClick) }
            }
        }
    }
}
