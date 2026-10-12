package com.lagradost.cloudstream3.utils

import android.app.ActivityManager
import android.content.Context

/**
 * Central low-memory policy for constrained devices (~500MB RAM phones / low-end TVs).
 *
 * Pure functions are unit-tested (see LowMemoryHelperTest); the only Android
 * dependency is [isLowRamDevice], which wraps ActivityManager with safe fallbacks.
 *
 * Rationale (verifiable sources):
 * - Android Manage-your-app-memory: only TRIM_MEMORY_UI_HIDDEN / BACKGROUND are
 *   delivered on modern Android; clear image mem caches there.
 * - ActivityManager.isLowRamDevice() is the documented low-RAM signal.
 * - ExoPlayer DefaultLoadControl back buffer retains 30s by default; halving it
 *   materially lowers peak native/heap buffering on 512MB devices.
 */
object LowMemoryHelper {
    const val BACK_BUFFER_DEFAULT_MS = 30_000
    const val BACK_BUFFER_LOW_RAM_MS = 10_000

    const val IMAGE_DISK_DEFAULT_BYTES = 128L * 1024L * 1024L
    const val IMAGE_DISK_LOW_RAM_BYTES = 64L * 1024L * 1024L

    const val HTTP_CACHE_DEFAULT_BYTES = 25L * 1024L * 1024L
    const val HTTP_CACHE_LOW_RAM_BYTES = 10L * 1024L * 1024L

    /** Cap for Exo disk cache on low-RAM devices when user selects Auto or a large value. */
    const val EXO_DISK_CAP_LOW_RAM_BYTES = 16L * 1024L * 1024L

    /** Cap for max video buffer duration on low-RAM devices (ms). */
    const val EXO_BUFFER_CAP_LOW_RAM_MS = 60_000

    /** Below this usable space we treat storage as pressured and disable disk caches. */
    const val LOW_STORAGE_THRESHOLD_BYTES = 500L * 1024L * 1024L

    fun isLowRamDevice(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.isLowRamDevice == true
        } catch (_: Throwable) {
            false
        }
    }

    fun backBufferMs(isLowRam: Boolean): Int =
        if (isLowRam) BACK_BUFFER_LOW_RAM_MS else BACK_BUFFER_DEFAULT_MS

    fun imageDiskBytes(isLowRam: Boolean): Long =
        if (isLowRam) IMAGE_DISK_LOW_RAM_BYTES else IMAGE_DISK_DEFAULT_BYTES

    fun httpCacheBytes(isLowRam: Boolean): Long =
        if (isLowRam) HTTP_CACHE_LOW_RAM_BYTES else HTTP_CACHE_DEFAULT_BYTES

    /**
     * @param requestedBytes user setting in bytes, 0 = Auto (Media3 default).
     * @return bytes to actually use; 0 preserved as Auto on normal devices.
     */
    fun capExoCacheBytes(requestedBytes: Long, isLowRam: Boolean): Long {
        if (!isLowRam) return requestedBytes.coerceAtLeast(0L)
        if (requestedBytes <= 0L) return EXO_DISK_CAP_LOW_RAM_BYTES
        return minOf(requestedBytes, EXO_DISK_CAP_LOW_RAM_BYTES)
    }

    /**
     * @param requestedMs user setting in ms, 0 = Auto (Media3 default).
     */
    fun capExoBufferMs(requestedMs: Long, isLowRam: Boolean): Long {
        if (!isLowRam) return requestedMs.coerceAtLeast(0L)
        if (requestedMs <= 0L) return 0L // Auto; DefaultLoadControl default is already conservative
        return minOf(requestedMs, EXO_BUFFER_CAP_LOW_RAM_MS.toLong())
    }

    fun isStorageLow(usableBytes: Long): Boolean = usableBytes < LOW_STORAGE_THRESHOLD_BYTES

    /**
     * Resolve disk cache size under storage pressure.
     * @return 0 to disable disk caching when storage is critically low is handled by caller
     * via null cache; here we return 0 meaning "no disk cache".
     */
    fun resolveDiskCacheSize(requestedBytes: Long, usableBytes: Long): Long {
        if (isStorageLow(usableBytes)) return 0L
        return requestedBytes.coerceAtLeast(0L)
    }

    /** Previews (MediaMetadataRetriever thumbnails) are disabled on TV already; also disable on low-RAM. */
    fun shouldDisablePreview(isLowRam: Boolean, isTv: Boolean): Boolean = isLowRam || isTv
}
