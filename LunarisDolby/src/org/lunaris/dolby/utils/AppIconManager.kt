/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import org.lunaris.dolby.R

/**
 * Alternate launcher icons declared as activity-aliases in the manifest.
 *
 * Exactly one alias is enabled at a time so the launcher never shows
 * duplicates. Switching is instant from the app's side; the launcher
 * itself may take a few seconds to refresh its icon cache.
 */
object AppIconManager {

    data class IconOption(
        val id: String,
        val label: String,
        val alias: String,
        /** Mipmap adaptive-icon used for the in-app preview. */
        val iconRes: Int
    )

    const val ID_DYNAMIC = "dynamic"
    const val ID_INDIGO = "indigo"
    const val ID_MIDNIGHT = "midnight"
    const val ID_GOLD = "gold"
    const val ID_CRIMSON = "crimson"
    const val ID_TEAL = "teal"
    const val ID_VIOLET = "violet"
    const val ID_OCEAN = "ocean"

    val OPTIONS = listOf(
        IconOption(ID_DYNAMIC, "Dynamic", ".ui.DolbyActivityIconDynamic", R.mipmap.ic_launcher),
        IconOption(ID_INDIGO, "Indigo", ".ui.DolbyActivityIconIndigo", R.mipmap.ic_launcher_indigo),
        IconOption(ID_MIDNIGHT, "Midnight", ".ui.DolbyActivityIconMidnight", R.mipmap.ic_launcher_midnight),
        IconOption(ID_GOLD, "Gold", ".ui.DolbyActivityIconGold", R.mipmap.ic_launcher_gold),
        IconOption(ID_CRIMSON, "Crimson", ".ui.DolbyActivityIconCrimson", R.mipmap.ic_launcher_crimson),
        IconOption(ID_TEAL, "Teal", ".ui.DolbyActivityIconTeal", R.mipmap.ic_launcher_teal),
        IconOption(ID_VIOLET, "Violet", ".ui.DolbyActivityIconViolet", R.mipmap.ic_launcher_violet),
        IconOption(ID_OCEAN, "Ocean", ".ui.DolbyActivityIconOcean", R.mipmap.ic_launcher_ocean)
    )

    /** Id of the currently enabled alias; falls back to dynamic. */
    fun current(context: Context): String {
        val pm = context.packageManager
        for (option in OPTIONS) {
            val state = runCatching {
                pm.getComponentEnabledSetting(
                    ComponentName(context.packageName, context.packageName + option.alias)
                )
            }.getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) return option.id
        }
        // Nothing explicitly enabled: the manifest default (dynamic,
        // android:enabled="true") is the visible icon.
        return ID_DYNAMIC
    }

    /** Enables [id] and disables every other launcher alias. */
    fun apply(context: Context, id: String) {
        val pm = context.packageManager
        for (option in OPTIONS) {
            val component = ComponentName(context.packageName, context.packageName + option.alias)
            val state = if (option.id == id) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            runCatching {
                pm.setComponentEnabledSetting(
                    component,
                    state,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}
