package com.lagradost.cloudstream3.utils

import com.lagradost.cloudstream3.utils.SubtitleHelper.fromCodeToLangTagIETF
import com.lagradost.cloudstream3.utils.SubtitleHelper.fromLanguageToTagIETF

/**
 * Preferred source-language matching (e.g., Telugu).
 *
 * This is a *preference, not a restriction*: callers reorder matching sources
 * first and keep all others as fallback. When no preference is set or nothing
 * matches, the original order/behavior is preserved.
 *
 * Matching is language-aware via [SubtitleHelper] so English names ("Telugu"),
 * native names ("తెలుగు"), and codes ("te", "tel", "TE", "te-IN") all resolve
 * to the same IETF base tag ("te").
 */
object SourceLanguageHelper {

    /** Curated single-choice options for the preference UI (TV-remote + small-screen friendly). */
    val COMMON_SOURCE_TAGS: List<String> = listOf(
        "en", "hi", "te", "ta", "ml", "kn", "bn", "mr", "pa", "gu",
        "es", "fr", "de", "ar", "zh", "ja", "ko", "ur", "or", "as"
    ).distinct()

    fun isPreferenceSet(preferredTag: String?): Boolean = !preferredTag.isNullOrBlank()

    /** "te-IN" -> "te", "tel" stays "tel" (further normalized via SubtitleHelper by callers). */
    fun baseTag(tag: String?): String? {
        if (tag.isNullOrBlank()) return null
        val base = tag.trim().replace('_', '-').substringBefore('-').lowercase()
        return base.ifBlank { null }
    }

    /**
     * Normalize any language input (name, native name, IETF/ISO/OpenSubtitles code,
     * or free-text like "HD Telugu 1080p") to its IETF base tag ("te").
     * Returns null when the input does not resolve to a known language.
     */
    fun normalizeToBaseTag(input: String?): String? {
        if (input.isNullOrBlank()) return null
        val trimmed = input.trim()
        // Direct code path first (handles "te", "tel", "te-IN").
        val viaCode = fromCodeToLangTagIETF(trimmed)?.let { baseTag(it) }
        if (viaCode != null) return viaCode
        // Name / phrase path with halfMatch (handles "Telugu", "తెలుగు", "HD Telugu 1080p").
        val viaName = fromLanguageToTagIETF(trimmed, true)?.let { baseTag(it) }
        if (viaName != null) return viaName
        return null
    }

    fun normalizedPreference(preferredTag: String?): String? {
        if (preferredTag.isNullOrBlank()) return null
        return normalizeToBaseTag(preferredTag.trim()) ?: baseTag(preferredTag)
    }

    /**
     * True when either [source] or [name] indicates [preferredTag].
     * Blank preference or unresolvable inputs return false (caller falls back).
     */
    fun matchesSource(source: String?, name: String?, preferredTag: String?): Boolean {
        val pref = normalizedPreference(preferredTag) ?: return false
        val candidates = listOf(source, name).filter { !it.isNullOrBlank() }.map { it!!.trim() }
        if (candidates.isEmpty()) return false
        for (candidate in candidates) {
            val tag = normalizeToBaseTag(candidate) ?: continue
            if (tag.equals(pref, ignoreCase = true)) return true
        }
        return false
    }

    /**
     * Stable reorder: matching items first (original relative order kept), others after.
     * Returns the original list instance when no preference or no matches (fallback).
     */
    fun <T> rankByLanguage(
        items: List<T>,
        preferredTag: String?,
        textOf: (T) -> Pair<String?, String?>
    ): List<T> {
        val pref = normalizedPreference(preferredTag) ?: return items
        if (items.isEmpty()) return items
        var anyMatch = false
        for (item in items) {
            val (source, name) = textOf(item)
            if (matchesSource(source, name, pref)) {
                anyMatch = true
                break
            }
        }
        if (!anyMatch) return items
        val matching = ArrayList<T>(items.size)
        val rest = ArrayList<T>(items.size)
        for (item in items) {
            val (source, name) = textOf(item)
            if (matchesSource(source, name, pref)) matching.add(item) else rest.add(item)
        }
        return matching + rest
    }

    /**
     * Pick the best audio track for [preferredTag] from runtime track metadata.
     * Tries `language` then `label` per track, language-aware.
     * Returns null when no preference or no match (caller keeps current behavior).
     */
    fun <T> findBestAudioTrack(
        tracks: List<T>,
        preferredTag: String?,
        langOf: (T) -> String?,
        labelOf: (T) -> String? = { null }
    ): T? {
        val pref = normalizedPreference(preferredTag) ?: return null
        if (tracks.isEmpty()) return null
        for (track in tracks) {
            val langTag = normalizeToBaseTag(langOf(track)?.trim())
            if (langTag != null && langTag.equals(pref, ignoreCase = true)) return track
        }
        for (track in tracks) {
            val labelTag = normalizeToBaseTag(labelOf(track)?.trim())
            if (labelTag != null && labelTag.equals(pref, ignoreCase = true)) return track
        }
        return null
    }
}
