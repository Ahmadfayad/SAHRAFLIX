package com.sahraflix.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class M3uParserTest {
    private val playlist = """
        ﻿#EXTM3U url-tvg="http://a/epg.xml.gz,http://b/epg.xml" x-tvg-url="http://a/epg.xml.gz"
        #EXTINF:-1 tvg-id="bbc1.uk" tvg-logo="http://l/bbc.png" group-title="UK, News",BBC One, HD
        #EXTVLCOPT:http-user-agent=MyAgent/1.0
        #EXTVLCOPT:http-referrer=http://ref/
        http://host/live/u/p/1.ts
        #EXTINF:-1 catchup="default" catchup-source="http://c/{utc}" tvg-shift=2,Sport 1
        #EXTGRP:Sports
        http://host/live/u/p/2.m3u8
        #EXTINF:0,The Matrix (1999)
        http://host/movie/u/p/33.mkv
        #EXTINF:-1 group-title="Series EN",Some Show S02E05
        http://host/series/u/p/44.mp4
        http://orphan/url
    """.trimIndent()

    private fun parse(): Pair<M3uParser.Header?, List<M3uParser.Entry>> {
        var header: M3uParser.Header? = null
        val entries = mutableListOf<M3uParser.Entry>()
        M3uParser().parse(playlist.reader().buffered(), { header = it }) { entries += it }
        return header to entries
    }

    @Test fun headerEpgUrlsAreSplitAndDeduplicated() =
        assertEquals(listOf("http://a/epg.xml.gz", "http://b/epg.xml"), parse().first!!.epgUrls)

    @Test fun orphanUrlsAreIgnored() = assertEquals(4, parse().second.size)

    @Test fun titlesAndAttributesMayContainCommas() {
        val bbc = parse().second[0]
        assertEquals("BBC One, HD", bbc.title)
        assertEquals("UK, News", bbc.group)
        assertEquals("bbc1.uk", bbc.tvgId)
        assertEquals("http://l/bbc.png", bbc.logo)
    }

    @Test fun vlcOptionsApplyToOneEntryOnly() {
        val (_, e) = parse()
        assertEquals("MyAgent/1.0", e[0].userAgent)
        assertEquals("http://ref/", e[0].referrer)
        assertNull(e[1].userAgent)
    }

    @Test fun groupAndCatchup() {
        val sport = parse().second[1]
        assertEquals("Sports", sport.group)
        assertEquals("http://c/{utc}", sport.catchupSource)
    }

    @Test fun kindDetection() {
        val (_, e) = parse()
        assertEquals(M3uParser.Kind.LIVE, e[0].kind)
        assertEquals(M3uParser.Kind.MOVIE, e[2].kind)
        assertEquals(M3uParser.Kind.SERIES, e[3].kind)
        assertEquals(M3uParser.Kind.LIVE, M3uParser().detectKind("http://x/stream?id=1.mp4", "Channel", null))
        assertEquals(M3uParser.Kind.SERIES, M3uParser().detectKind("http://x/1.ts", "Show S1E2", null))
    }
}
