/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.ui.customize

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.daybreak.dolby.ui.theme.AccentChoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CornerStyle(val radius: Dp) {
    SOFT(8.dp), ROUNDED(16.dp), PILL(28.dp)
}

enum class CardStyle { FILLED, OUTLINED, ELEVATED }

enum class BarDesign { STOCK, GLASS }

enum class BarStyle { TRANSPARENT, FROSTED, FILLED, TONAL, OUTLINED }

data class Customize(
    val dynamicColor: Boolean = true,
    val amoled: Boolean = false,
    val accent: AccentChoice = AccentChoice.DEFAULT,
    val cornerStyle: CornerStyle = CornerStyle.PILL,
    val cardStyle: CardStyle = CardStyle.FILLED,
    val barDesign: BarDesign = BarDesign.GLASS,
    val barStyle: BarStyle = BarStyle.TRANSPARENT,
    val rainbow: Boolean = true,
    val glassIntensity: Float = 0.6f,
    val navTint: Float = 0.55f,
    val navBlurRadius: Float = 20f,
)

class CustomizeRepository private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _customize = MutableStateFlow(readCustomize())
    val customize: StateFlow<Customize> = _customize.asStateFlow()

    private fun readCustomize(): Customize = Customize(
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
        amoled = prefs.getBoolean(KEY_AMOLED, false),
        accent = readEnum(KEY_ACCENT, AccentChoice.DEFAULT),
        cornerStyle = readEnum(KEY_CORNER_STYLE, CornerStyle.PILL),
        cardStyle = readEnum(KEY_CARD_STYLE, CardStyle.FILLED),
        barDesign = readEnum(KEY_BAR_DESIGN, BarDesign.GLASS),
        barStyle = readEnum(KEY_BAR_STYLE, BarStyle.TRANSPARENT),
        rainbow = prefs.getBoolean(KEY_RAINBOW, true),
        glassIntensity = prefs.getFloat(KEY_GLASS_INTENSITY, 0.6f).coerceIn(0f, 1f),
        navTint = prefs.getFloat(KEY_NAV_TINT, 0.55f).coerceIn(0f, 1f),
        navBlurRadius = prefs.getFloat(KEY_NAV_BLUR_RADIUS, 20f).coerceIn(0f, 64f),
    )

    private inline fun <reified T : Enum<T>> readEnum(key: String, fallback: T): T =
        runCatching { java.lang.Enum.valueOf(T::class.java, prefs.getString(key, fallback.name)) }
            .getOrDefault(fallback)

    fun update(transform: (Customize) -> Customize) {
        val next = transform(_customize.value)
        prefs.edit()
            .putBoolean(KEY_DYNAMIC_COLOR, next.dynamicColor)
            .putBoolean(KEY_AMOLED, next.amoled)
            .putString(KEY_ACCENT, next.accent.name)
            .putString(KEY_CORNER_STYLE, next.cornerStyle.name)
            .putString(KEY_CARD_STYLE, next.cardStyle.name)
            .putString(KEY_BAR_DESIGN, next.barDesign.name)
            .putString(KEY_BAR_STYLE, next.barStyle.name)
            .putBoolean(KEY_RAINBOW, next.rainbow)
            .putFloat(KEY_GLASS_INTENSITY, next.glassIntensity.coerceIn(0f, 1f))
            .putFloat(KEY_NAV_TINT, next.navTint.coerceIn(0f, 1f))
            .putFloat(KEY_NAV_BLUR_RADIUS, next.navBlurRadius.coerceIn(0f, 64f))
            .apply()
        _customize.value = next
    }

    fun resetAll() = update { Customize() }

    companion object {
        private const val PREFS_NAME = "daybreak_customize"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_AMOLED = "amoled"
        private const val KEY_ACCENT = "accent"
        private const val KEY_CORNER_STYLE = "corner_style"
        private const val KEY_CARD_STYLE = "card_style"
        private const val KEY_BAR_DESIGN = "bar_design"
        private const val KEY_BAR_STYLE = "bar_style"
        private const val KEY_RAINBOW = "rainbow"
        private const val KEY_GLASS_INTENSITY = "glass_intensity"
        private const val KEY_NAV_TINT = "nav_tint"
        private const val KEY_NAV_BLUR_RADIUS = "nav_blur_radius"

        @Volatile private var instance: CustomizeRepository? = null

        fun get(context: Context): CustomizeRepository =
            instance ?: synchronized(this) {
                instance ?: CustomizeRepository(context).also { instance = it }
            }
    }
}

@Composable
fun rememberCustomize(): State<Customize> {
    val context = LocalContext.current
    val repo = remember(context.applicationContext) {
        CustomizeRepository.get(context.applicationContext)
    }
    return repo.customize.collectAsState()
}
