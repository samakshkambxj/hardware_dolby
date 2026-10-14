/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.daybreak.dolby.audio.DolbyController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DolbyTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            val controller = DolbyController.get(applicationContext)
            val on = controller.isEnabled()
            qsTile?.apply {
                state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                subtitle = profileLabel(controller)
                updateTile()
            }
        }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val controller = DolbyController.get(applicationContext)
            val on = controller.isEnabled()
            controller.setEnabled(!on)
            qsTile?.apply {
                state = if (!on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                subtitle = profileLabel(controller)
                updateTile()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun profileLabel(controller: DolbyController): CharSequence {
        val profile = controller.currentProfile().toString()
        val values = resources.getStringArray(R.array.profile_values)
        val index = values.indexOf(profile)
        if (index == -1) return getString(R.string.level_off)
        return resources.getStringArray(R.array.profile_entries)[index]
    }
}
