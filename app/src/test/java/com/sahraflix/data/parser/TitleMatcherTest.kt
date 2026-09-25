package com.sahraflix.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TitleMatcherTest {
    @Test fun matchesDecoratedIptvNames() {
        assertTrue(TitleMatcher.score("The Matrix", 1999, "EN - The Matrix (1999) [4K]") >= 80)
        assertTrue(TitleMatcher.score("The Matrix", 1999, "|FR| Matrix 1999 FHD") >= 80)
        assertTrue(TitleMatcher.score("Amélie", 2001, "Amelie 2001 HD") >= 80)
        assertTrue(TitleMatcher.score("Fast & Furious", null, "Fast and Furious") >= 80)
    }

    @Test fun rejectsSequelsWrongYearsAndSubstrings() {
        assertTrue(TitleMatcher.score("The Matrix", 1999, "The Matrix Reloaded (2003)") < 80)
        assertTrue(TitleMatcher.score("Dune", 2021, "Dune (1984)") < 80)
        assertEquals(0, TitleMatcher.score("Up", 2009, "Upgrade (2018)"))
    }

    @Test fun searchFragmentIsLongestWord() = assertEquals("rings", TitleMatcher.searchFragment("The Lord of the Rings"))
}
