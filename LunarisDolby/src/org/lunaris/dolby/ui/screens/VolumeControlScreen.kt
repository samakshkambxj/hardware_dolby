/*
 * Copyright (C) 2026 Anshuman _X (maxxcodebug)
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.screens

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.lunaris.dolby.R
import org.lunaris.dolby.ui.components.ModernSettingSlider
import org.lunaris.dolby.ui.components.ModernSettingsCard

private data class VolumeStreamInfo(
    val streamType: Int,
    val title: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VolumeControlScreen() {
    val context = LocalContext.current

    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    val notificationManager = remember {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    val streams = remember {
        listOf(
            VolumeStreamInfo(
                AudioManager.STREAM_MUSIC,
                "Media",
                Icons.Default.MusicNote
            ),
            VolumeStreamInfo(
                AudioManager.STREAM_RING,
                "Ringtone",
                Icons.Default.Notifications
            ),
            VolumeStreamInfo(
                AudioManager.STREAM_NOTIFICATION,
                "Notification",
                Icons.Default.NotificationsActive
            ),
            VolumeStreamInfo(
                AudioManager.STREAM_ALARM,
                "Alarm",
                Icons.Default.Alarm
            ),
            VolumeStreamInfo(
                AudioManager.STREAM_VOICE_CALL,
                "Call",
                Icons.Default.Call
            )
        )
    }

    var refreshTrigger by remember {
        mutableIntStateOf(0)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.volume),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(streams) { stream ->
            key(refreshTrigger) {
                val max = remember(stream.streamType) {
                    audioManager.getStreamMaxVolume(stream.streamType)
                }

                var current by remember(
                    stream.streamType,
                    refreshTrigger
                ) {
                    mutableIntStateOf(
                        audioManager.getStreamVolume(stream.streamType)
                    )
                }

                ModernSettingsCard(
                    title = stream.title,
                    icon = stream.icon
                ) {
                    ModernSettingSlider(
                        title = stream.title,
                        value = current,
                        valueRange = 0f..max.toFloat(),
                        steps = (max - 1).coerceAtLeast(0),
                        onValueChange = { newValue ->
                            val target = newValue.toInt()

                            current = target

                            try {
                                audioManager.setStreamVolume(
                                    stream.streamType,
                                    target,
                                    0
                                )
                            } catch (e: SecurityException) {
                                if (!notificationManager.isNotificationPolicyAccessGranted) {
                                    // DND access may be required on some Android versions.
                                }
                            }
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}
