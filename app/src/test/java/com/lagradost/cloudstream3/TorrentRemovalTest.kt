package com.lagradost.cloudstream3

import com.lagradost.cloudstream3.ui.player.LOADTYPE_CHROMECAST
import com.lagradost.cloudstream3.ui.player.LOADTYPE_INAPP
import com.lagradost.cloudstream3.ui.player.LOADTYPE_INAPP_DOWNLOAD
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Torrent/P2P removal regression guard.
 *
 * - In-app playback must never request TORRENT/MAGNET (filtered in RepoLinkGenerator).
 * - Download + Chromecast sets were already torrent-free and must stay so.
 * - Library enums (ExtractorLinkType.TORRENT/MAGNET, TvType.Torrent) are KEPT for
 *   extension ABI compat; only the in-app engine/wiring is removed.
 */
class TorrentRemovalTest {

    @Test
    fun `inapp load type excludes torrent and magnet`() {
        assertFalse(LOADTYPE_INAPP.contains(com.lagradost.cloudstream3.utils.ExtractorLinkType.TORRENT))
        assertFalse(LOADTYPE_INAPP.contains(com.lagradost.cloudstream3.utils.ExtractorLinkType.MAGNET))
    }

    @Test
    fun `inapp load type keeps normal playback types`() {
        assertTrue(LOADTYPE_INAPP.contains(com.lagradost.cloudstream3.utils.ExtractorLinkType.VIDEO))
        assertTrue(LOADTYPE_INAPP.contains(com.lagradost.cloudstream3.utils.ExtractorLinkType.M3U8))
        assertTrue(LOADTYPE_INAPP.contains(com.lagradost.cloudstream3.utils.ExtractorLinkType.DASH))
    }

    @Test
    fun `download and chromecast types exclude torrent`() {
        assertFalse(LOADTYPE_INAPP_DOWNLOAD.contains(com.lagradost.cloudstream3.utils.ExtractorLinkType.TORRENT))
        assertFalse(LOADTYPE_INAPP_DOWNLOAD.contains(com.lagradost.cloudstream3.utils.ExtractorLinkType.MAGNET))
        assertFalse(LOADTYPE_CHROMECAST.contains(com.lagradost.cloudstream3.utils.ExtractorLinkType.TORRENT))
        assertFalse(LOADTYPE_CHROMECAST.contains(com.lagradost.cloudstream3.utils.ExtractorLinkType.MAGNET))
    }

    @Test
    fun `extension enums kept for compatibility`() {
        // Deleting these would break existing extensions; they must still exist.
        assertTrue(
            com.lagradost.cloudstream3.utils.ExtractorLinkType.entries.contains(
                com.lagradost.cloudstream3.utils.ExtractorLinkType.TORRENT
            )
        )
        assertTrue(
            com.lagradost.cloudstream3.utils.ExtractorLinkType.entries.contains(
                com.lagradost.cloudstream3.utils.ExtractorLinkType.MAGNET
            )
        )
        assertTrue(TvType.entries.contains(TvType.Torrent))
    }
}
