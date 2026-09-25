package com.sahraflix.presentation.epg

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.domain.model.EpgProgram
import com.sahraflix.presentation.theme.CinematicCharcoal
import com.sahraflix.presentation.theme.GoldWash
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val CHANNEL_WIDTH = 200.dp
private val PER_MINUTE = 5.dp

@Composable
fun EpgTimeline(
    channels: LazyPagingItems<CatalogEntry.Iptv>,
    viewModel: EpgViewModel,
    windowStart: Long,
    windowEnd: Long,
    onChannelClick: (CatalogEntry.Iptv) -> Unit,
    onProgramClick: (CatalogEntry.Iptv, EpgProgram) -> Unit
) {
    val scroll = rememberScrollState()
    val minutes = TimeUnit.MILLISECONDS.toMinutes(windowEnd - windowStart)
    val now = remember { System.currentTimeMillis() }
    val density = LocalDensity.current
    // Start scrolled to "now" (the window begins 2 h earlier for catch-up).
    LaunchedEffect(Unit) {
        scroll.scrollTo(with(density) { (PER_MINUTE * TimeUnit.MILLISECONDS.toMinutes(now - windowStart).toFloat() - 60.dp).roundToPx() }.coerceAtLeast(0))
    }
    Column {
        Row {
            Box(Modifier.width(CHANNEL_WIDTH))
            Row(Modifier.horizontalScroll(scroll).width(PER_MINUTE * minutes.toFloat())) {
                for (m in 0 until minutes step 30) {
                    Text(hhmm(windowStart + TimeUnit.MINUTES.toMillis(m)), Modifier.width(PER_MINUTE * 30f), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(count = channels.itemCount, key = channels.itemKey { it.id }) { index ->
                channels[index]?.let { channel ->
                    val programmes by remember(channel.id, windowStart) {
                        viewModel.programmes(channel, windowStart, windowEnd)
                    }.collectAsState(initial = emptyList())
                    ChannelRow(channel, programmes, windowStart, windowEnd, now, scroll, onChannelClick, onProgramClick)
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(
    channel: CatalogEntry.Iptv,
    programmes: List<EpgProgram>,
    windowStart: Long,
    windowEnd: Long,
    now: Long,
    scroll: ScrollState,
    onChannelClick: (CatalogEntry.Iptv) -> Unit,
    onProgramClick: (CatalogEntry.Iptv, EpgProgram) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Card(onClick = { onChannelClick(channel) }, modifier = Modifier.width(CHANNEL_WIDTH - 8.dp).height(60.dp)) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(channel.posterUrl, null, Modifier.width(48.dp).height(36.dp))
                Text(channel.title, Modifier.padding(start = 8.dp), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Box(Modifier.width(8.dp))
        Row(
            Modifier.horizontalScroll(scroll).width(PER_MINUTE * TimeUnit.MILLISECONDS.toMinutes(windowEnd - windowStart).toFloat()).height(60.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (programmes.isEmpty()) {
                Card(onClick = { onChannelClick(channel) }, modifier = Modifier.fillMaxWidth().height(60.dp)) {
                    Text("No guide data", Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
            var cursor = windowStart
            programmes.forEach { p ->
                val start = maxOf(p.startTime, windowStart)
                val end = minOf(p.endTime, windowEnd)
                if (end <= start) return@forEach
                if (start > cursor) Box(Modifier.width(width(start - cursor)))
                cursor = end
                val live = p.isLive(now)
                Card(
                    onClick = { onProgramClick(channel, p) },
                    modifier = Modifier.width(width(end - start) - 3.dp).height(60.dp),
                    colors = CardDefaults.colors(containerColor = if (live) GoldWash else CinematicCharcoal)
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(p.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                        Text("${hhmm(p.startTime)} – ${hhmm(p.endTime)}" + if (p.isPast(now) && channel.stream.catchupSource != null) "  ⟲" else "",
                            maxLines = 1, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun width(ms: Long): Dp = PER_MINUTE * TimeUnit.MILLISECONDS.toMinutes(ms).toFloat()
private fun hhmm(t: Long) = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(t))
