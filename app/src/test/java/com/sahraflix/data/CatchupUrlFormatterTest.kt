package com.sahraflix.data

import com.sahraflix.data.local.entity.StreamItemEntity
import com.sahraflix.data.repository.CatchupUrlFormatter
import com.sahraflix.domain.model.StreamType
import org.junit.Assert.assertEquals
import org.junit.Test

class CatchupUrlFormatterTest {
    @Test
    fun replacesEpochCatchupTokens() {
        val stream = StreamItemEntity(
            id = "channel", name = "Channel", streamUrl = "https://live.example/channel.m3u8",
            logoUrl = null, streamType = StreamType.LIVE, categoryId = "live", playlistId = "p",
            catchupSource = "https://replay.example/{start}/{end}"
        )
        val result = CatchupUrlFormatter.format(stream, 1_700_000_000_000, 1_700_003_600_000)
        assertEquals("https://replay.example/1700000000/1700003600", result)
    }
}
