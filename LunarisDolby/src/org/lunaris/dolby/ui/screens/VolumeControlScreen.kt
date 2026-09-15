/*
 * Copyright (C) 2026 Anshuman _X (maxxcodebug)
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.lunaris.dolby.R
import org.lunaris.dolby.ui.components.FloatingParticles
import org.lunaris.dolby.ui.components.ModernSettingSlider
import org.lunaris.dolby.ui.components.ModernSettingsCard
import org.lunaris.dolby.ui.components.verticalBouncyEdge

private data class VolumeStreamInfo(
    val streamType: Int,
    val title: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VolumeControlScreen() {
    val context = LocalContext.current

    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
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

    fun readVolumes(): Map<Int, Int> =
        streams.associate { stream ->
            stream.streamType to try {
                audioManager.getStreamVolume(stream.streamType)
            } catch (_: Exception) {
                0
            }
        }

    var volumes by remember { mutableStateOf(readVolumes()) }

    fun refreshVolumes() {
        volumes = readVolumes()
    }

    // Reflect hardware volume keys / system volume panel changes while visible.
    DisposableEffect(context) {
        refreshVolumes()
        val filter = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction("android.media.STREAM_DEVICES_CHANGED_ACTION")
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                refreshVolumes()
            }
        }
        // VOLUME_CHANGED_ACTION is protected on some builds; fall back gracefully.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (_: Exception) {
            // No live sync; sliders still read fresh values on recompose.
        }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.volume),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
        FloatingParticles()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .verticalBouncyEdge()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = streams,
                key = { it.streamType }
            ) { stream ->
                val max = remember(stream.streamType) {
                    try {
                        audioManager.getStreamMaxVolume(stream.streamType)
                    } catch (_: Exception) {
                        15
                    }
                }
                val min = remember(stream.streamType) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        try {
                            audioManager.getStreamMinVolume(stream.streamType)
                        } catch (_: Exception) {
                            0
                        }
                    } else {
                        0
                    }
                }
                val current = (volumes[stream.streamType]
                    ?: try {
                        audioManager.getStreamVolume(stream.streamType)
                    } catch (_: Exception) {
                        min
                    }).coerceIn(min, max)

                ModernSettingsCard(
                    title = stream.title,
                    icon = stream.icon
                ) {
                    ModernSettingSlider(
                        title = "Level",
                        value = current,
                        valueRange = min.toFloat()..max.toFloat(),
                        steps = (max - min - 1).coerceAtLeast(0),
                        valueLabel = { "$it / $max" },
                        onValueChange = { newValue ->
                            val target = newValue.toInt().coerceIn(min, max)
                            // Optimistic UI update; receiver will correct it if clamped.
                            volumes = volumes.toMutableMap().apply {
                                put(stream.streamType, target)
                            }
                            try {
                                audioManager.setStreamVolume(
                                    stream.streamType,
                                    target,
                                    0
                                )
                            } catch (_: SecurityException) {
                                // DND / policy restricted stream; re-read actual value.
                                refreshVolumes()
                            } catch (_: Exception) {
                                refreshVolumes()
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
        }
    }
}
