/*
 * Copyright (C) 2023-24 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "DolbyAtmos-Boot"

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: ${intent.action}")
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        Log.i(TAG, "Boot completed, starting dolby")

        // Restoring every profile performs a lot of effect transactions. Do it off the broadcast
        // main thread so boot is not delayed, and finish the pending result when we are done.
        val pendingResult = goAsync()
        Thread(
                {
                    try {
                        DolbyController.getInstance(context.applicationContext).onBootCompleted()
                    } catch (t: Throwable) {
                        Log.e(TAG, "Failed to restore Dolby state", t)
                    } finally {
                        pendingResult.finish()
                    }
                },
                "dolby-boot",
            )
            .start()
    }
}
