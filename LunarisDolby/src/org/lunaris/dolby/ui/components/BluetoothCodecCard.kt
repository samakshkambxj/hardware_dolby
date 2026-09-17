/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothCodecConfig
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape

/**
 * Read-only Bluetooth audio card, mirroring [CodecInfoCard]: the active
 * A2DP device, negotiated codec, sample rate and HD status. Pure
 * observation — no DSP, no routing changes, zero conflict risk.
 *
 * Codec reads need BLUETOOTH_CONNECT (runtime on API 31+): without it
 * the card offers a one-tap grant instead of failing silently.
 */
data class BtAudioInfo(
    val deviceName: String,
    val codecLabel: String,
    val isHd: Boolean,
    val sampleRateHz: Int,
    val bitsPerSample: Int,
    val channelLabel: String
)

@Composable
fun BluetoothCodecCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(hasBtConnectPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }
    var info by remember { mutableStateOf<BtAudioInfo?>(null) }
    var queried by remember { mutableStateOf(false) }

    DisposableEffect(hasPermission) {
        if (!hasPermission) {
            info = null
            queried = false
            return@DisposableEffect onDispose {}
        }
        val adapter = runCatching { BluetoothAdapter.getDefaultAdapter() }.getOrNull()
        if (adapter == null) {
            queried = true
            return@DisposableEffect onDispose {}
        }
        var proxy: BluetoothA2dp? = null
        val listener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxyObject: BluetoothProfile) {
                val a2dp = proxyObject as BluetoothA2dp
                proxy = a2dp
                info = readBtAudioInfo(a2dp)
                queried = true
            }

            override fun onServiceDisconnected(profile: Int) {
                proxy = null
                info = null
                queried = true
            }
        }
        runCatching { adapter.getProfileProxy(context, listener, BluetoothProfile.A2DP) }
        onDispose {
            runCatching {
                proxy?.let { adapter.closeProfileProxy(BluetoothProfile.A2DP, it) }
            }
        }
    }

    ModernSettingsCard(
        title = "Bluetooth audio",
        icon = Icons.Default.Bluetooth,
        modifier = modifier
    ) {
        when {
            !hasPermission -> {
                Text(
                    text = "Allow nearby-device access to read the active Bluetooth codec.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    } else {
                        hasPermission = true
                    }
                }) {
                    Text("Grant access")
                }
            }
            !queried -> {
                SpinningDolbyLogo(size = 40.dp)
            }
            info == null -> {
                Text(
                    text = "No Bluetooth audio device connected.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                val current = info!!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = current.deviceName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = buildString {
                                append(current.codecLabel)
                                if (current.sampleRateHz > 0) {
                                    append(" · ${formatBtRate(current.sampleRateHz)}")
                                }
                                if (current.bitsPerSample > 0) {
                                    append(" · ${current.bitsPerSample}-bit")
                                }
                                append(" · ${current.channelLabel}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (current.isHd) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "HD",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun hasBtConnectPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    return runCatching {
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)
}

private fun readBtAudioInfo(a2dp: BluetoothA2dp): BtAudioInfo? {
    return runCatching {
        val device = a2dp.connectedDevices.firstOrNull() ?: return null
        val status = a2dp.getCodecStatus(device) ?: return null
        val config = status.codecConfig ?: return null
        val codecType = config.codecType
        BtAudioInfo(
            deviceName = runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() }
                ?: "Bluetooth device",
            codecLabel = btCodecLabel(codecType),
            isHd = codecType == BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD ||
                codecType == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC ||
                codecType == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LC3,
            sampleRateHz = runCatching { config.sampleRate }.getOrDefault(-1),
            bitsPerSample = runCatching { config.bitsPerSample }.getOrDefault(-1),
            channelLabel = btChannelLabel(runCatching { config.channelMode }.getOrDefault(-1))
        )
    }.getOrNull()
}

private fun btCodecLabel(codecType: Int): String {
    return when (codecType) {
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC -> "SBC"
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC -> "AAC"
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX -> "aptX"
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD -> "aptX HD"
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC -> "LDAC"
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_LC3 -> "LC3"
        else -> "Codec $codecType"
    }
}

private fun btChannelLabel(channelMode: Int): String {
    return when (channelMode) {
        BluetoothCodecConfig.CHANNEL_MODE_MONO -> "Mono"
        BluetoothCodecConfig.CHANNEL_MODE_STEREO -> "Stereo"
        else -> "Audio"
    }
}

private fun formatBtRate(hz: Int): String {
    return if (hz >= 1000 && hz % 1000 == 0) "${hz / 1000} kHz" else "$hz Hz"
}
