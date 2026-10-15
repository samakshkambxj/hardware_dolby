/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.dolbyatmos.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.dolbyatmos.DolbyConstants
import org.dolbyatmos.R
import org.dolbyatmos.data.DolbyRepository
import org.dolbyatmos.data.SleepTimerManager
import org.dolbyatmos.utils.ToastHelper

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
