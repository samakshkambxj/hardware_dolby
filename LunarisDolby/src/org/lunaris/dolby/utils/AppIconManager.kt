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
        /**
         * Background color for the in-app preview. Null = dynamic
         * (follows the theme primary). Never point the preview at the
         * mipmap adaptive-icon: Compose's painterResource cannot inflate
         * <adaptive-icon> XML and crashes the Customization screen.
         */
        val backgroundRes: Int?
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
        IconOption(ID_DYNAMIC, "Dynamic", ".ui.DolbyActivityIconDynamic", null),
        IconOption(ID_INDIGO, "Indigo", ".ui.DolbyActivityIconIndigo", R.color.icon_bg_indigo),
        IconOption(ID_MIDNIGHT, "Midnight", ".ui.DolbyActivityIconMidnight", R.color.icon_bg_midnight),
        IconOption(ID_GOLD, "Gold", ".ui.DolbyActivityIconGold", R.color.icon_bg_gold),
        IconOption(ID_CRIMSON, "Crimson", ".ui.DolbyActivityIconCrimson", R.color.icon_bg_crimson),
        IconOption(ID_TEAL, "Teal", ".ui.DolbyActivityIconTeal", R.color.icon_bg_teal),
        IconOption(ID_VIOLET, "Violet", ".ui.DolbyActivityIconViolet", R.color.icon_bg_violet),
        IconOption(ID_OCEAN, "Ocean", ".ui.DolbyActivityIconOcean", R.color.icon_bg_ocean)
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
