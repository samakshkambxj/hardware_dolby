/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
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

data class PageStyle(
    val headerTitle: String = AppBrand.NAME,
    val subtitleText: String = "Experience immersive audio",
    val showTitle: Boolean = true,
    val showSubtitle: Boolean = true,
    val iconStyle: PageIconStyle = PageIconStyle.ACCENT,
    val cornerStyle: PageCornerStyle = PageCornerStyle.ROUNDED
) {
    val cardShape: RoundedCornerShape get() = RoundedCornerShape(cornerStyle.radius)
}

private const val PREFS_NAME = "dolby_page_style"
private const val KEY_TITLE = "header_title"
private const val KEY_SUBTITLE = "subtitle_text"
private const val KEY_SHOW_TITLE = "show_title"
private const val KEY_SHOW_SUBTITLE = "show_subtitle"
private const val KEY_ICON_STYLE = "icon_style"
private const val KEY_CORNER_STYLE = "corner_style"

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
        iconStyle = runCatching {
            PageIconStyle.valueOf(prefs.getString(KEY_ICON_STYLE, PageIconStyle.ACCENT.name)!!)
        }.getOrDefault(PageIconStyle.ACCENT),
        cornerStyle = runCatching {
            PageCornerStyle.valueOf(prefs.getString(KEY_CORNER_STYLE, PageCornerStyle.ROUNDED.name)!!)
        }.getOrDefault(PageCornerStyle.ROUNDED)
    )

    fun update(transform: (PageStyle) -> PageStyle) {
        val next = transform(_style.value)
        prefs.edit()
            .putString(KEY_TITLE, next.headerTitle)
            .putString(KEY_SUBTITLE, next.subtitleText)
            .putBoolean(KEY_SHOW_TITLE, next.showTitle)
            .putBoolean(KEY_SHOW_SUBTITLE, next.showSubtitle)
            .putString(KEY_ICON_STYLE, next.iconStyle.name)
            .putString(KEY_CORNER_STYLE, next.cornerStyle.name)
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
