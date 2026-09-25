package com.sahraflix.data

import com.sahraflix.data.repository.CatchupUrlFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatchupUrlFormatterTest {
    private val start = 1_700_000_000_000L          // 2023-11-14 22:13:20 UTC
    private val end = start + 3_600_000L
    private val now = start + 7_200_000L

    @Test fun epochTokens() = assertEquals(
        "https://r/1700000000/1700003600",
        CatchupUrlFormatter.format("https://r/{start}/{end}", null, "u", start, end, now)
    )

    @Test fun xtreamTimeshiftUsesServerTimezoneAndMinutes() = assertEquals(
        "http://h/timeshift/u/p/60/2023-11-14:22-13/42.ts",
        CatchupUrlFormatter.format("http://h/timeshift/u/p/{duration_min}/{xtream_start|Europe/London}/42.ts", "xtream", "u", start, end, now)
    )

    @Test fun shiftTypeAppendsUtcParams() = assertEquals(
        "http://h/ch.m3u8?utc=1700000000&lutc=1700007200",
        CatchupUrlFormatter.format(null, "shift", "http://h/ch.m3u8", start, end, now)
    )

    @Test fun appendTypeUsesOffset() = assertEquals(
        "http://h/ch?offset=7200",
        CatchupUrlFormatter.format("?offset={offset}", "append", "http://h/ch", start, end, now)
    )

    @Test fun customFormats() {
        assertEquals("http://h/2023-11-14-22", CatchupUrlFormatter.format("http://h/{utc:Y-m-d-H}", null, "u", start, end, now))
        assertEquals("http://h/20231114/3600", CatchupUrlFormatter.format("http://h/{Y}{m}{d}/{duration}", null, "u", start, end, now))
    }

    @Test fun noTemplateMeansNoCatchup() = assertNull(CatchupUrlFormatter.format(null, null, "u", start, end, now))
}
