package com.lagradost.cloudstream3

import com.lagradost.cloudstream3.utils.SourceLanguageHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Preferred source language is a preference with fallback, never a restriction. */
class SourceLanguageHelperTest {

    @Test
    fun `telugu names and codes normalize to te`() {
        assertEquals("te", SourceLanguageHelper.normalizeToBaseTag("Telugu"))
        assertEquals("te", SourceLanguageHelper.normalizeToBaseTag("తెలుగు"))
        assertEquals("te", SourceLanguageHelper.normalizeToBaseTag("te"))
        assertEquals("te", SourceLanguageHelper.normalizeToBaseTag("TE"))
        assertEquals("te", SourceLanguageHelper.normalizeToBaseTag("tel"))
        assertEquals("te", SourceLanguageHelper.normalizeToBaseTag("te-IN"))
        assertEquals("te", SourceLanguageHelper.normalizeToBaseTag("HD Telugu 1080p"))
    }

    @Test
    fun `other languages do not normalize to te`() {
        assertEquals("hi", SourceLanguageHelper.normalizeToBaseTag("Hindi"))
        assertEquals("en", SourceLanguageHelper.normalizeToBaseTag("English"))
        assertNull(SourceLanguageHelper.normalizeToBaseTag(null))
        assertNull(SourceLanguageHelper.normalizeToBaseTag(""))
        assertNull(SourceLanguageHelper.normalizeToBaseTag("Klingon"))
    }

    @Test
    fun `matches source or name with telugu preference`() {
        assertTrue(SourceLanguageHelper.matchesSource("Telugu HD", "1080p", "te"))
        assertTrue(SourceLanguageHelper.matchesSource("HD", "Telugu 1080p", "te"))
        assertTrue(SourceLanguageHelper.matchesSource("తెలుగు", "HD", "te"))
        assertTrue(SourceLanguageHelper.matchesSource("TE", "HD", "te"))
        assertFalse(SourceLanguageHelper.matchesSource("Hindi HD", "1080p", "te"))
        assertFalse(SourceLanguageHelper.matchesSource("English", "1080p", "te"))
    }

    @Test
    fun `blank preference never matches (fallback)`() {
        assertFalse(SourceLanguageHelper.matchesSource("Telugu HD", "1080p", null))
        assertFalse(SourceLanguageHelper.matchesSource("Telugu HD", "1080p", ""))
        assertFalse(SourceLanguageHelper.matchesSource("Telugu HD", "1080p", "   "))
        assertFalse(SourceLanguageHelper.matchesSource(null, null, "te"))
    }

    data class FakeLink(val source: String?, val name: String?)

    @Test
    fun `rank puts preferred language first and keeps fallback`() {
        val links = listOf(
            FakeLink("English HD", "1080p"),
            FakeLink("Hindi HD", "1080p"),
            FakeLink("Telugu HD", "1080p"),
            FakeLink("Tamil HD", "1080p"),
        )
        val ranked = SourceLanguageHelper.rankByLanguage(links, "te") { it.source to it.name }
        assertEquals("Telugu HD", ranked.first().source)
        // All others preserved (preference, not restriction).
        assertEquals(4, ranked.size)
        assertTrue(ranked.map { it.source }.containsAll(listOf("English HD", "Hindi HD", "Tamil HD")))
        // Relative order of non-matching is stable.
        assertEquals(listOf("English HD", "Hindi HD", "Tamil HD"), ranked.drop(1).map { it.source })
    }

    @Test
    fun `rank falls back to original order when no match`() {
        val links = listOf(
            FakeLink("English HD", "1080p"),
            FakeLink("Hindi HD", "1080p"),
        )
        val ranked = SourceLanguageHelper.rankByLanguage(links, "te") { it.source to it.name }
        assertEquals(links, ranked)
    }

    @Test
    fun `rank falls back when preference blank`() {
        val links = listOf(
            FakeLink("Telugu HD", "1080p"),
            FakeLink("English HD", "1080p"),
        )
        assertEquals(links, SourceLanguageHelper.rankByLanguage(links, null) { it.source to it.name })
        assertEquals(links, SourceLanguageHelper.rankByLanguage(links, "") { it.source to it.name })
    }

    data class FakeAudio(val language: String?, val label: String?)

    @Test
    fun `finds matching audio track for telugu`() {
        val tracks = listOf(
            FakeAudio("en", "English"),
            FakeAudio("hi", "Hindi"),
            FakeAudio("te", "Telugu"),
        )
        val best = SourceLanguageHelper.findBestAudioTrack(
            tracks, "te",
            langOf = { it.language },
            labelOf = { it.label }
        )
        assertEquals("te", best?.language)
    }

    @Test
    fun `audio matching handles region subtags`() {
        val tracks = listOf(
            FakeAudio("en-US", "English"),
            FakeAudio("te-IN", "Telugu"),
        )
        val best = SourceLanguageHelper.findBestAudioTrack(
            tracks, "te",
            langOf = { it.language },
            labelOf = { it.label }
        )
        assertEquals("te-IN", best?.language)
    }

    @Test
    fun `audio falls back to null when no match`() {
        val tracks = listOf(
            FakeAudio("en", "English"),
            FakeAudio("hi", "Hindi"),
        )
        assertNull(
            SourceLanguageHelper.findBestAudioTrack(
                tracks, "te",
                langOf = { it.language },
                labelOf = { it.label }
            )
        )
        assertNull(
            SourceLanguageHelper.findBestAudioTrack(
                tracks, null,
                langOf = { it.language },
                labelOf = { it.label }
            )
        )
    }
}
