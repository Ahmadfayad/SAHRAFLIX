package com.sahraflix.presentation.epg

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.compose.foundation.lazy.LazyColumn
import androidx.tv.material3.Card
import androidx.tv.material3.Text
import com.sahraflix.domain.model.EpgProgram
import com.sahraflix.domain.model.StreamItem
import java.util.concurrent.TimeUnit
import kotlin.math.max

@Composable
fun EpgTimeline(
    channels: LazyPagingItems<StreamItem>,
    viewModel: EpgViewModel,
    windowStart: Long,
    windowEnd: Long,
    onProgramClick: (EpgProgram) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalState = rememberScrollState()
    val pixelsPerMinute = 6.dp
    val windowMinutes = max(
        1L,
        TimeUnit.MILLISECONDS.toMinutes(windowEnd - windowStart)
    )

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(CHANNEL_WIDTH))
            Row(
                modifier = Modifier
                    .horizontalScroll(horizontalState)
                    .width(pixelsPerMinute * windowMinutes.toFloat())
            ) {
                for (minute in 0..windowMinutes step 60) {
                    Text(
                        text = formatHour(windowStart + TimeUnit.MINUTES.toMillis(minute)),
                        modifier = Modifier.width(pixelsPerMinute * 60f)
                    )
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(channels.itemCount) { index ->
                channels[index]?.let { channel ->
                    val programs by remember(channel.id, windowStart, windowEnd) {
                        viewModel.programs(channel.id, windowStart, windowEnd)
                    }.collectAsState(initial = emptyList())
                    EpgChannelRow(
                        channel = channel,
                        programs = programs,
                        windowStart = windowStart,
                        windowEnd = windowEnd,
                        pixelsPerMinute = pixelsPerMinute,
                        horizontalState = horizontalState,
                        onProgramClick = onProgramClick
                    )
                }
            }
        }
    }
}

@Composable
private fun EpgChannelRow(
    channel: StreamItem,
    programs: List<EpgProgram>,
    windowStart: Long,
    windowEnd: Long,
    pixelsPerMinute: androidx.compose.ui.unit.Dp,
    horizontalState: androidx.compose.foundation.ScrollState,
    onProgramClick: (EpgProgram) -> Unit
) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(
            text = channel.name,
            modifier = Modifier.width(CHANNEL_WIDTH)
        )
        Row(
            modifier = Modifier
                .horizontalScroll(horizontalState)
                .width(pixelsPerMinute * TimeUnit.MILLISECONDS.toMinutes(windowEnd - windowStart).toFloat())
                .height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            programs.forEach { program ->
                val visibleStart = max(program.startTime, windowStart)
                val visibleEnd = minOf(program.endTime, windowEnd)
                val calculatedWidth = pixelsPerMinute *
                    TimeUnit.MILLISECONDS.toMinutes(visibleEnd - visibleStart).toFloat()
                val width = if (calculatedWidth > 72.dp) calculatedWidth else 72.dp
                Card(
                    onClick = { onProgramClick(program) },
                    modifier = Modifier.width(width)
                ) {
                    Text(
                        text = program.title,
                        modifier = Modifier.padding(8.dp),
                        maxLines = 2
                    )
                }
            }
        }
    }
}

private fun formatHour(time: Long): String =
    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(time))

private val CHANNEL_WIDTH = 180.dp
