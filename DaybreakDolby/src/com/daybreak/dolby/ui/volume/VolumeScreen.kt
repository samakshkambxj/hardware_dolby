/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.ui.volume

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.daybreak.dolby.R
import com.daybreak.dolby.ui.components.DayCard
import com.daybreak.dolby.ui.components.DayScreen
import com.daybreak.dolby.ui.components.DaySectionHeader
import com.daybreak.dolby.ui.components.DaySlider

@Composable
fun VolumeScreen(viewModel: VolumeViewModel) {
    val streams by viewModel.streams.collectAsState()

    DayScreen {
        item { DaySectionHeader(text = stringResource(R.string.tab_volume)) }

        streams.forEach { stream ->
            item(key = stream.type) {
                DayCard {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(stream.labelRes),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        StreamSlider(
                            stream = stream,
                            onCommit = { viewModel.setVolume(stream.type, it) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamSlider(
    stream: StreamState,
    onCommit: (Int) -> Unit,
) {
    var pos by remember(stream.current) {
        mutableFloatStateOf(stream.current.toFloat())
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        DaySlider(
            value = pos,
            valueRange = stream.min.toFloat()..stream.max.toFloat(),
            steps = (stream.max - stream.min - 1).coerceAtLeast(0),
            onLiveChange = { pos = it },
            onCommit = { onCommit(pos.toInt()) },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "${pos.toInt()}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(40.dp),
        )
    }
}
