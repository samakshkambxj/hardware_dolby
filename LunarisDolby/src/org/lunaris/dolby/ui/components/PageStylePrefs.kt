/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fixed app branding. NOTHING in PageStyleRepository/PageStyleScreen reads
 * from or writes to this object — it is not user-editable, by design.
 */
object AppBrand {
    const val NAME = "Dolby Atmos"
    const val CREDIT = "samakshkambxj"
}

enum class PageIconStyle { ACCENT, PLAIN, FILLED }

enum class PageCornerStyle(val radius: Dp) {
    SOFT(8.dp), ROUNDED(16.dp), PILL(28.dp)
}

enum class PageCardStyle { FILLED, OUTLINED, ELEVATED }

enum class PageIconShape { CIRCLE, SQUIRCLE, ROUNDED }

enum class PageIconSize(val box: Dp, val icon: Dp) {
    S(32.dp, 20.dp), M(40.dp, 24.dp), L(48.dp, 28.dp)
}

enum class ParticleDensity(val count: Int) {
    OFF(0), SUBTLE(35), DENSE(90)
}

enum class AccentChoice { DEFAULT, AZURE, VIOLET, TEAL, JADE, GOLD, ROSE, RUBY }

data class PageStyle(
    val headerTitle: String = AppBrand.NAME,
    val subtitleText: String = "Experience immersive audio",
    val showTitle: Boolean = true,
    val showSubtitle: Boolean = true,
    val iconStyle: PageIconStyle = PageIconStyle.ACCENT,
    val cornerStyle: PageCornerStyle = PageCornerStyle.ROUNDED,
    val cardStyle: PageCardStyle = PageCardStyle.FILLED,
    val iconShape: PageIconShape = PageIconShape.ROUNDED,
    val iconSize: PageIconSize = PageIconSize.M,
    val showBannerWaveform: Boolean = true,
    val bannerGradient: Boolean = true,
    val headerCentered: Boolean = false,
    val particleDensity: ParticleDensity = ParticleDensity.DENSE,
    val navBlur: Boolean = true,
    val dynamicColor: Boolean = true,
    val amoledDark: Boolean = false,
    val accent: AccentChoice = AccentChoice.DEFAULT
) {
    val cardShape: RoundedCornerShape get() = RoundedCornerShape(cornerStyle.radius)
}

/** Shape of the leading icon badge in settings cards. */
@Composable
fun PageIconShape.shape(): Shape = when (this) {
    PageIconShape.CIRCLE -> CircleShape
    PageIconShape.SQUIRCLE -> RoundedCornerShape(percent = 30)
    PageIconShape.ROUNDED -> MaterialTheme.shapes.medium
}

private const val PREFS_NAME = "dolby_page_style"
private const val KEY_TITLE = "header_title"
private const val KEY_SUBTITLE = "subtitle_text"
private const val KEY_SHOW_TITLE = "show_title"
private const val KEY_SHOW_SUBTITLE = "show_subtitle"
private const val KEY_ICON_STYLE = "icon_style"
private const val KEY_CORNER_STYLE = "corner_style"
private const val KEY_CARD_STYLE = "card_style"
private const val KEY_ICON_SHAPE = "icon_shape"
private const val KEY_ICON_SIZE = "icon_size"
private const val KEY_SHOW_BANNER_WAVEFORM = "show_banner_waveform"
private const val KEY_BANNER_GRADIENT = "banner_gradient"
private const val KEY_HEADER_CENTERED = "header_centered"
private const val KEY_PARTICLE_DENSITY = "particle_density"
private const val KEY_NAV_BLUR = "nav_blur"
private const val KEY_DYNAMIC_COLOR = "dynamic_color"
private const val KEY_AMOLED_DARK = "amoled_dark"
private const val KEY_ACCENT = "accent"

class PageStyleRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _style = MutableStateFlow(readStyle())
    val style: StateFlow<PageStyle> = _style.asStateFlow()

    private fun readStyle(): PageStyle = PageStyle(
        headerTitle = prefs.getString(KEY_TITLE, null) ?: AppBrand.NAME,
        subtitleText = prefs.getString(KEY_SUBTITLE, null) ?: "Experience immersive audio",
        showTitle = prefs.getBoolean(KEY_SHOW_TITLE, true),
        showSubtitle = prefs.getBoolean(KEY_SHOW_SUBTITLE, true),
        iconStyle = readEnum(KEY_ICON_STYLE, PageIconStyle.ACCENT),
        cornerStyle = readEnum(KEY_CORNER_STYLE, PageCornerStyle.ROUNDED),
        cardStyle = readEnum(KEY_CARD_STYLE, PageCardStyle.FILLED),
        iconShape = readEnum(KEY_ICON_SHAPE, PageIconShape.ROUNDED),
        iconSize = readEnum(KEY_ICON_SIZE, PageIconSize.M),
        showBannerWaveform = prefs.getBoolean(KEY_SHOW_BANNER_WAVEFORM, true),
        bannerGradient = prefs.getBoolean(KEY_BANNER_GRADIENT, true),
        headerCentered = prefs.getBoolean(KEY_HEADER_CENTERED, false),
        particleDensity = readEnum(KEY_PARTICLE_DENSITY, ParticleDensity.DENSE),
        navBlur = prefs.getBoolean(KEY_NAV_BLUR, true),
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
        amoledDark = prefs.getBoolean(KEY_AMOLED_DARK, false),
        accent = readEnum(KEY_ACCENT, AccentChoice.DEFAULT)
    )

    private inline fun <reified T : Enum<T>> readEnum(key: String, fallback: T): T =
        runCatching { java.lang.Enum.valueOf(T::class.java, prefs.getString(key, fallback.name)) }
            .getOrDefault(fallback)

    fun update(transform: (PageStyle) -> PageStyle) {
        val next = transform(_style.value)
        prefs.edit()
            .putString(KEY_TITLE, next.headerTitle)
            .putString(KEY_SUBTITLE, next.subtitleText)
            .putBoolean(KEY_SHOW_TITLE, next.showTitle)
            .putBoolean(KEY_SHOW_SUBTITLE, next.showSubtitle)
            .putString(KEY_ICON_STYLE, next.iconStyle.name)
            .putString(KEY_CORNER_STYLE, next.cornerStyle.name)
            .putString(KEY_CARD_STYLE, next.cardStyle.name)
            .putString(KEY_ICON_SHAPE, next.iconShape.name)
            .putString(KEY_ICON_SIZE, next.iconSize.name)
            .putBoolean(KEY_SHOW_BANNER_WAVEFORM, next.showBannerWaveform)
            .putBoolean(KEY_BANNER_GRADIENT, next.bannerGradient)
            .putBoolean(KEY_HEADER_CENTERED, next.headerCentered)
            .putString(KEY_PARTICLE_DENSITY, next.particleDensity.name)
            .putBoolean(KEY_NAV_BLUR, next.navBlur)
            .putBoolean(KEY_DYNAMIC_COLOR, next.dynamicColor)
            .putBoolean(KEY_AMOLED_DARK, next.amoledDark)
            .putString(KEY_ACCENT, next.accent.name)
            .apply()
        _style.value = next
    }

    fun resetAll() = update { PageStyle() }

    companion object {
        @Volatile private var instance: PageStyleRepository? = null
        fun get(context: Context): PageStyleRepository =
            instance ?: synchronized(this) {
                instance ?: PageStyleRepository(context).also { instance = it }
            }
    }
}

@Composable
fun rememberPageStyleRepository(): PageStyleRepository {
    val context = LocalContext.current
    return remember(context) { PageStyleRepository.get(context) }
}

@Composable
fun rememberPageStyle(): State<PageStyle> {
    val repo = rememberPageStyleRepository()
    return repo.style.collectAsState()
}
