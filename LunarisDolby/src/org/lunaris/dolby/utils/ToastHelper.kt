/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ToastHelper {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentToast: Toast? = null

    data class ToastEvent(val message: String, val long: Boolean = false)

    /**
     * Foreground messages surface as Material snackbars (see DolbyActivity).
     * Only used when the UI is collecting; background callers (services,
     * receivers, app closed) still get a plain system toast.
     */
    private val _events = MutableSharedFlow<ToastEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ToastEvent> = _events.asSharedFlow()

    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        val delivered = if (_events.subscriptionCount.value > 0) {
            _events.tryEmit(ToastEvent(message, duration == Toast.LENGTH_LONG))
        } else {
            false
        }
        if (!delivered) {
            mainHandler.post {
                currentToast?.cancel()
                currentToast = Toast.makeText(context.applicationContext, message, duration)
                currentToast?.show()
            }
        }
    }

    fun showLongToast(context: Context, message: String) {
        showToast(context, message, Toast.LENGTH_LONG)
    }
}
