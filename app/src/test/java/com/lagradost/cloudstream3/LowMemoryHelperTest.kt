package com.lagradost.cloudstream3

import com.lagradost.cloudstream3.utils.LowMemoryHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Low-memory cache/buffer policy must stay bounded on 500MB-RAM devices. */
class LowMemoryHelperTest {

    @Test
    fun `back buffer is smaller on low ram`() {
        assertEquals(10_000, LowMemoryHelper.backBufferMs(true))
        assertEquals(30_000, LowMemoryHelper.backBufferMs(false))
    }

    @Test
    fun `image disk cache is bounded`() {
        assertEquals(64L * 1024L * 1024L, LowMemoryHelper.imageDiskBytes(true))
        assertEquals(128L * 1024L * 1024L, LowMemoryHelper.imageDiskBytes(false))
        // Regression: must never return the old 512MB default.
        assertTrue(LowMemoryHelper.imageDiskBytes(true) <= 128L * 1024L * 1024L)
        assertTrue(LowMemoryHelper.imageDiskBytes(false) <= 128L * 1024L * 1024L)
    }

    @Test
    fun `http cache is bounded`() {
        assertEquals(10L * 1024L * 1024L, LowMemoryHelper.httpCacheBytes(true))
        assertEquals(25L * 1024L * 1024L, LowMemoryHelper.httpCacheBytes(false))
    }

    @Test
    fun `exo disk cache capped on low ram`() {
        // Auto (0) becomes a small bounded cache on low-RAM instead of Media3 default.
        assertEquals(
            LowMemoryHelper.EXO_DISK_CAP_LOW_RAM_BYTES,
            LowMemoryHelper.capExoCacheBytes(0L, true)
        )
        // Large user values are capped.
        assertEquals(
            LowMemoryHelper.EXO_DISK_CAP_LOW_RAM_BYTES,
            LowMemoryHelper.capExoCacheBytes(500L * 1024L * 1024L, true)
        )
        // Small user values pass through.
        assertEquals(4L * 1024L * 1024L, LowMemoryHelper.capExoCacheBytes(4L * 1024L * 1024L, true))
        // Normal devices preserve Auto (0).
        assertEquals(0L, LowMemoryHelper.capExoCacheBytes(0L, false))
    }

    @Test
    fun `exo buffer duration capped on low ram`() {
        assertEquals(0L, LowMemoryHelper.capExoBufferMs(0L, true))
        assertEquals(60_000L, LowMemoryHelper.capExoBufferMs(300_000L, true))
        assertEquals(30_000L, LowMemoryHelper.capExoBufferMs(30_000L, true))
        assertEquals(300_000L, LowMemoryHelper.capExoBufferMs(300_000L, false))
    }

    @Test
    fun `storage pressure disables disk cache`() {
        assertTrue(LowMemoryHelper.isStorageLow(100L * 1024L * 1024L))
        assertFalse(LowMemoryHelper.isStorageLow(2L * 1024L * 1024L * 1024L))
        assertEquals(0L, LowMemoryHelper.resolveDiskCacheSize(16L * 1024L * 1024L, 100L))
        assertEquals(
            16L * 1024L * 1024L,
            LowMemoryHelper.resolveDiskCacheSize(16L * 1024L * 1024L, 2L * 1024L * 1024L * 1024L)
        )
    }
}
