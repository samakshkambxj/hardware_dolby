/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import org.lunaris.dolby.DolbyConstants
import org.lunaris.dolby.service.SleepTimerReceiver

data class SleepTimerState(
    val active: Boolean = false,
    val remainingMs: Long = 0L
)

/**
 * Turns Dolby off after a chosen delay. The deadline (elapsed-realtime based,
 * so it survives wall-clock changes) is stored in "dolby_prefs" and enforced
 * with an exact alarm delivered to [SleepTimerReceiver]. If exact alarms are
 * not permitted, falls back to an inexact one.
 */
class SleepTimerManager(private val context: Context) {

    fun start(minutes: Int) {
        if (minutes <= 0) {
            cancel()
            return
        }
        val deadline = SystemClock.elapsedRealtime() + minutes * 60_000L
        prefs().edit().putLong(DolbyConstants.PREF_SLEEP_TIMER_DEADLINE, deadline).apply()
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        try {
            if (alarm.canScheduleExactAlarms()) {
                alarm.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP, deadline, pendingIntent()
                )
            } else {
                alarm.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP, deadline, pendingIntent()
                )
            }
        } catch (e: SecurityException) {
            DolbyConstants.dlog(TAG, "Exact alarm denied, using inexact: ${e.message}")
            alarm.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP, deadline, pendingIntent()
            )
        }
    }

    fun cancel() {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent())
        clearDeadline(context)
    }

    private fun prefs() =
        context.getSharedPreferences("dolby_prefs", Context.MODE_PRIVATE)

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, SleepTimerReceiver::class.java)
            .setAction(ACTION_SLEEP_TIMER)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val TAG = "SleepTimerManager"
        private const val REQUEST_CODE = 0xD01B
        const val ACTION_SLEEP_TIMER = "org.lunaris.dolby.ACTION_SLEEP_TIMER"

        fun remainingMs(context: Context): Long {
            val deadline = context.getSharedPreferences("dolby_prefs", Context.MODE_PRIVATE)
                .getLong(DolbyConstants.PREF_SLEEP_TIMER_DEADLINE, 0L)
            if (deadline <= 0L) return 0L
            return (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        }

        fun isActive(context: Context): Boolean = remainingMs(context) > 0L

        fun clearDeadline(context: Context) {
            context.getSharedPreferences("dolby_prefs", Context.MODE_PRIVATE)
                .edit().remove(DolbyConstants.PREF_SLEEP_TIMER_DEADLINE).apply()
        }
    }
}
