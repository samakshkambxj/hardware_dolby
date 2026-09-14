/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.lunaris.dolby.DolbyConstants
import org.lunaris.dolby.R
import org.lunaris.dolby.data.DolbyRepository
import org.lunaris.dolby.data.SleepTimerManager
import org.lunaris.dolby.utils.ToastHelper

/**
 * Fired by the [SleepTimerManager] alarm when the sleep timer elapses.
 * Disables Dolby and clears the stored deadline. Not exported; the alarm
 * uses an explicit intent.
 */
class SleepTimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != SleepTimerManager.ACTION_SLEEP_TIMER) return
        try {
            SleepTimerManager.clearDeadline(context)
            DolbyRepository(context.applicationContext).use { repository ->
                repository.setDolbyEnabled(false)
            }
            ToastHelper.showLongToast(
                context.applicationContext,
                context.getString(R.string.sleep_timer_ended)
            )
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Sleep timer failed: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "SleepTimerReceiver"
    }
}
