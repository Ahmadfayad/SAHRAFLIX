package com.sahraflix.data.parser

import java.text.Normalizer

/**
 * Matches TMDB titles against IPTV VOD names, which are typically decorated:
 * "EN - The Matrix (1999) [4K]", "|FR| Matrix 1999 FHD", "4K-The.Matrix.1999".
 */
object TitleMatcher {
    private val PREFIX = Regex("""^\s*(\|[^|]{1,6}\||\[[^]]{1,6}]|[A-Z]{2,3}\s*[-:|]|4K\s*[-:|]|VOD\s*[-:|])\s*""", RegexOption.IGNORE_CASE)
    private val YEAR = Regex("""\b(19[0-9]{2}|20[0-9]{2})\b""")
    private val NOISE = Regex(
        """\b(4k|uhd|fhd|hd|sd|hevc|h265|x265|x264|1080p|720p|2160p|multi|sub|subs|vostfr|dual|audio|web[- ]?dl|bluray|hdr|dv|imax|extended|remastered)\b""",
        RegexOption.IGNORE_CASE
    )

    data class Parsed(val key: String, val year: Int?)

    fun parse(raw: String): Parsed {
        var s = raw
        repeat(2) { s = PREFIX.replace(s, "") }
        val year = YEAR.findAll(s).lastOrNull()?.value?.toIntOrNull()
        s = s.replace(Regex("""[\[(][^\])]*[\])]"""), " ")   // (1999) [4K] etc.
        s = YEAR.replace(s, " ")
        s = NOISE.replace(s, " ")
        return Parsed(normalize(s), year)
    }

    fun normalize(value: String): String {
        val ascii = Normalizer.normalize(value, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        return ascii.lowercase()
            .replace("&", " and ")
            .replace(Regex("""^(the|a|an)\s+"""), "")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("""^(the|a|an)\s+"""), "")
    }

    /** Score 0..100; ≥ 80 is considered the same title. */
    fun score(tmdbTitle: String, tmdbYear: Int?, candidate: String): Int {
        val want = normalize(tmdbTitle)
        val got = parse(candidate)
        if (want.isEmpty() || got.key.isEmpty()) return 0
        var score = when {
            got.key == want -> 90
            got.key.startsWith("$want ") || got.key.endsWith(" $want") -> 70
            else -> return 0
        }
        if (tmdbYear != null && got.year != null) score += if (tmdbYear == got.year) 10 else -40
        return score.coerceIn(0, 100)
    }

    /** A short fragment for the SQL LIKE pre-filter. */
    fun searchFragment(title: String): String =
        normalize(title).split(' ').maxByOrNull { it.length }?.takeIf { it.length >= 3 } ?: normalize(title)
}
