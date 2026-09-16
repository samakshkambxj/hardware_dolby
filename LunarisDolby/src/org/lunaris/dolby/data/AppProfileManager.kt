/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val assignedProfile: Int = -1,
    /** Scene id assigned to this app, or null. Mutually exclusive with [assignedProfile]. */
    val assignedSceneId: String? = null
)

class AppProfileManager(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("app_profiles", Context.MODE_PRIVATE)
    private val packageManager = context.packageManager
    
    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            
            val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
            val assignedProfiles = getAppsWithProfiles()
            val assignedScenes = getAppsWithScenes()

            resolveInfos
                .distinctBy { it.activityInfo.packageName }
                .map { resolveInfo ->
                    val packageName = resolveInfo.activityInfo.packageName
                    AppInfo(
                        packageName = packageName,
                        appName = resolveInfo.loadLabel(packageManager).toString(),
                        icon = resolveInfo.loadIcon(packageManager),
                        assignedProfile = assignedProfiles[packageName] ?: -1,
                        assignedSceneId = assignedScenes[packageName]
                    )
                }
                .sortedBy { it.appName }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun getAppProfile(packageName: String): Int {
        return prefs.getInt(packageName, -1)
    }
    
    fun setAppProfile(packageName: String, profile: Int) {
        prefs.edit().putInt(packageName, profile).apply()
    }
    
    fun removeAppProfile(packageName: String) {
        prefs.edit().remove(packageName).apply()
    }
    
    fun getAppsWithProfiles(): Map<String, Int> {
        return prefs.all.mapNotNull { (key, value) ->
            if (value is Int) key to value else null
        }.toMap()
    }

    /**
     * Scene assignments share the same prefs file: a String value marks a
     * scene id, an Int value marks a profile. One key holds one kind, so
     * assigning a scene overwrites a profile and vice versa.
     */
    fun getAppScene(packageName: String): String? {
        return (prefs.all[packageName] as? String)
            ?.takeIf { it.startsWith(SCENE_PREFIX) }
            ?.removePrefix(SCENE_PREFIX)
            ?.takeIf { it.isNotEmpty() }
    }

    fun setAppScene(packageName: String, sceneId: String) {
        prefs.edit().putString(packageName, SCENE_PREFIX + sceneId).apply()
    }

    fun getAppsWithScenes(): Map<String, String> {
        return prefs.all.mapNotNull { (key, value) ->
            val id = (value as? String)
                ?.takeIf { it.startsWith(SCENE_PREFIX) }
                ?.removePrefix(SCENE_PREFIX)
                ?.takeIf { it.isNotEmpty() }
            if (id != null) key to id else null
        }.toMap()
    }
    
    fun clearAllAppProfiles() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val SCENE_PREFIX = "scene:"
    }
}
