/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import org.lunaris.dolby.R
import org.lunaris.dolby.data.SpatializerManager

/**
 * Toggles the framework spatializer. Shows as unavailable when the device
 * has no spatializer or the current output can't be spatialized.
 * Long-press opens the app via QS_TILE_PREFERENCES.
 */
class SpatialTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        var manager: SpatializerManager? = null
        try {
            manager = SpatializerManager(applicationContext)
            manager.refresh()
            val enabled = manager.isEnabled.value
            if (manager.setEnabled(!enabled)) {
                manager.refresh()
            }
        } catch (_: Exception) {
        } finally {
            try {
                manager?.destroy()
            } catch (_: Exception) {
            }
        }
        updateTile()
    }

    private fun updateTile() {
        qsTile?.apply {
            var supported = false
            var available = false
            var enabled = false
            var manager: SpatializerManager? = null
            try {
                manager = SpatializerManager(applicationContext)
                manager.refresh()
                supported = manager.isSupported.value
                available = manager.isAvailable.value
                enabled = manager.isEnabled.value
            } catch (_: Exception) {
            } finally {
                try {
                    manager?.destroy()
                } catch (_: Exception) {
                }
            }
            state = when {
                !supported || !available -> Tile.STATE_UNAVAILABLE
                enabled -> Tile.STATE_ACTIVE
                else -> Tile.STATE_INACTIVE
            }
            subtitle = if (enabled) getString(R.string.dolby_on)
                       else getString(R.string.dolby_off)
            updateTile()
        }
    }
}
