/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.data

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.lunaris.dolby.R

/**
 * Hidden unlockables. Persisted in SharedPreferences so badges survive
 * restarts. All entry points init lazily and are safe to call from any
 * thread; UI observes [badges], [logoTaps] and [appOpens].
 */
object EasterEggs {

    const val BADGE_PERSISTENT = "persistent"
    const val BADGE_MASHER = "masher"
    const val BADGE_WORDSMITH = "wordsmith"
    const val BADGE_LIZARD = "lizard"
    const val BADGE_MAINTAINER = "maintainer"

    val ALL_BADGES = listOf(
        BADGE_PERSISTENT, BADGE_MASHER, BADGE_WORDSMITH, BADGE_LIZARD, BADGE_MAINTAINER
    )

    const val LOGO_TAP_TARGET = 7
    const val LOGO_TAP_WINDOW_MS = 10_000L

    /** Volume-key Konami: up, up, down, down. */
    private val KONAMI_SEQUENCE = listOf(true, true, false, false)
    private const val KONAMI_WINDOW_MS = 2_000L

    /** Preset-name cheat codes (matched case-insensitively after trim). */
    val CHEAT_CODES = setOf(
        "konami", "iddqd", "lunaris", "xyzzy", "1337", "samaksh", "samakshhhh"
    )

    /** Cheat codes that invoke the maintainer by name. */
    fun isMaintainerCode(code: String): Boolean =
        code == "samaksh" || code == "samakshhhh"

    private const val PREFS = "easter_eggs"
    private const val KEY_BADGES = "badges"
    private const val KEY_LOGO_TAPS = "logo_taps"
    private const val KEY_APP_OPENS = "app_opens"

    @Volatile private var initialized = false
    private lateinit var prefs: SharedPreferences

    private val _badges = MutableStateFlow(setOf<String>())
    val badges: StateFlow<Set<String>> = _badges.asStateFlow()

    private val _logoTaps = MutableStateFlow(0)
    val logoTaps: StateFlow<Int> = _logoTaps.asStateFlow()

    private val _appOpens = MutableStateFlow(0)
    val appOpens: StateFlow<Int> = _appOpens.asStateFlow()

    // In-memory only: progress toward the Konami sequence.
    private var konamiProgress = 0
    private var konamiLastTime = 0L
    private var logoLastTap = 0L
    private var logoStreak = 0

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _badges.value = prefs.getStringSet(KEY_BADGES, emptySet())?.toSet() ?: emptySet()
        _logoTaps.value = prefs.getInt(KEY_LOGO_TAPS, 0)
        _appOpens.value = prefs.getInt(KEY_APP_OPENS, 0)
        initialized = true
    }

    fun isUnlocked(id: String): Boolean = _badges.value.contains(id)

    fun unlock(context: Context, id: String): Boolean {
        init(context)
        if (_badges.value.contains(id)) return false
        _badges.value = _badges.value + id
        prefs.edit().putStringSet(KEY_BADGES, _badges.value).apply()
        return true
    }

    fun recordAppOpen(context: Context): Int {
        init(context)
        _appOpens.value = _appOpens.value + 1
        prefs.edit().putInt(KEY_APP_OPENS, _appOpens.value).apply()
        return _appOpens.value
    }

    /**
     * Records a logo tap. Returns the current consecutive streak
     * (1..[LOGO_TAP_TARGET]). The streak keeps counting even after the badge
     * is owned so the celebration can replay on every completion.
     */
    fun recordLogoTap(context: Context, now: Long = SystemClock.uptimeMillis()): Int {
        init(context)
        _logoTaps.value = _logoTaps.value + 1
        prefs.edit().putInt(KEY_LOGO_TAPS, _logoTaps.value).apply()
        logoStreak = if (now - logoLastTap > LOGO_TAP_WINDOW_MS) 1 else logoStreak + 1
        logoLastTap = now
        val result = logoStreak.coerceAtMost(LOGO_TAP_TARGET)
        // Auto-reset so the next streak starts fresh and every 7-tap
        // run celebrates instead of firing on a single follow-up tap.
        if (result >= LOGO_TAP_TARGET) logoStreak = 0
        return result
    }

    /**
     * Feeds one volume-key press into the Konami detector. Returns true on
     * every completion (also unlocks the badge on the first one) so the
     * celebration replays instead of firing only once.
     */
    fun recordVolumeKey(context: Context, up: Boolean, now: Long = SystemClock.uptimeMillis()): Boolean {
        init(context)
        if (now - konamiLastTime > KONAMI_WINDOW_MS) konamiProgress = 0
        konamiLastTime = now
        konamiProgress = if (KONAMI_SEQUENCE.getOrNull(konamiProgress) == up) {
            konamiProgress + 1
        } else {
            // Overlapping restart: a lone UP can be the start of a new attempt.
            if (KONAMI_SEQUENCE.firstOrNull() == up) 1 else 0
        }
        if (konamiProgress >= KONAMI_SEQUENCE.size) {
            konamiProgress = 0
            unlock(context, BADGE_MASHER)
            return true
        }
        return false
    }

    /** Returns the matched cheat code, or null. Unlocking is left to the caller. */
    fun checkPresetName(name: String): String? {
        val normalized = name.trim().lowercase()
        return CHEAT_CODES.firstOrNull { it == normalized }
    }

    fun cheatMessageRes(code: String): Int = when (code) {
        "konami" -> R.string.egg_cheat_konami
        "iddqd" -> R.string.egg_cheat_iddqd
        "lunaris" -> R.string.egg_cheat_lunaris
        "xyzzy" -> R.string.egg_cheat_xyzzy
        "samaksh", "samakshhhh" -> R.string.egg_cheat_samaksh
        else -> R.string.egg_cheat_elite
    }

    /** Returns true if this visit freshly unlocked the badge. */
    fun visitLounge(context: Context): Boolean = unlock(context, BADGE_LIZARD)

    fun reset(context: Context) {
        init(context)
        _badges.value = emptySet()
        _logoTaps.value = 0
        _appOpens.value = 0
        logoStreak = 0
        konamiProgress = 0
        prefs.edit()
            .putStringSet(KEY_BADGES, emptySet())
            .putInt(KEY_LOGO_TAPS, 0)
            .putInt(KEY_APP_OPENS, 0)
            .apply()
    }
}
