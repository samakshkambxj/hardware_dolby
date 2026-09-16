/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Alternate launcher icons declared as activity-aliases in the manifest.
 *
 * Exactly one alias is enabled at a time so the launcher never shows
 * duplicates. Switching is instant from the app's side; the launcher
 * itself may take a few seconds to refresh its icon cache.
 */
object AppIconManager {

    data class IconOption(val id: String, val label: String, val alias: String)

    const val ID_DYNAMIC = "dynamic"
    const val ID_INDIGO = "indigo"
    const val ID_MIDNIGHT = "midnight"
    const val ID_GOLD = "gold"

    val OPTIONS = listOf(
        IconOption(ID_DYNAMIC, "Dynamic", ".ui.DolbyActivityIconDynamic"),
        IconOption(ID_INDIGO, "Indigo", ".ui.DolbyActivityIconIndigo"),
        IconOption(ID_MIDNIGHT, "Midnight", ".ui.DolbyActivityIconMidnight"),
        IconOption(ID_GOLD, "Gold", ".ui.DolbyActivityIconGold")
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
