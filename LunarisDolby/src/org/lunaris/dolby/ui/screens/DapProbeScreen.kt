/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.lunaris.dolby.domain.models.DolbyUiState
import org.lunaris.dolby.ui.viewmodel.DolbyViewModel

private sealed interface ProbeResult {
    data object Idle : ProbeResult
    data object Running : ProbeResult
    data class Value(val v: Int) : ProbeResult
    data class Error(val msg: String) : ProbeResult
}

/** Known IDs as references plus every gap 100-130 as a candidate sweep.
 * Read-only: green value = HAL answered (still needs a listening test —
 * some HALs answer 0 for unknown IDs); red = unsupported. */
private val KNOWN_HINTS = mapOf(
    101 to "headphone virtualizer — reference",
    102 to "speaker virtualizer — reference",
    103 to "volume leveler — reference",
    104 to "IEQ preset — reference",
    105 to "dialogue enable — reference",
    108 to "dialogue amount — reference",
    110 to "GEQ gains — reference (shows first band only)",
    111 to "bass enhancer — reference",
    113 to "widening — reference, must read",
    116 to "leveler amount — reference, must read"
)

private val PROBE_TARGETS: List<Pair<Int, String>> = (100..130).map { id ->
    id to (KNOWN_HINTS[id] ?: "gap — candidate (reverb/height/bass-width?)")
}

/**
 * Hidden debug screen (5 taps on the Advanced title opens it).
 * Read-only sweep of candidate DAP parameter IDs on the current profile.
 * A returned value does NOT prove the param does anything — some HALs
 * answer 0 for unknown IDs — but a failure proves it is unsupported.
 * Results also go to logcat (tag Dolby-DapProbe) for copy-paste.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DapProbeScreen(
    viewModel: DolbyViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = (uiState as? DolbyUiState.Success)?.settings?.currentProfile ?: 0
    val scope = rememberCoroutineScope()
    var results by remember { mutableStateOf<Map<Int, ProbeResult>>(emptyMap()) }
    var probingAll by remember { mutableStateOf(false) }

    fun probe(id: Int) {
        scope.launch {
            results = results + (id to ProbeResult.Running)
            val r = viewModel.probeDapParam(id, profile)
            results = results + (id to r.fold(
                onSuccess = { ProbeResult.Value(it) },
                onFailure = { ProbeResult.Error(it.message ?: it.toString()) }
            ))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                // Keep action icons off the screen edge (M3 only insets 4.dp).
                modifier = Modifier.padding(end = 8.dp),
                title = {
                    Text(
                        "DAP probe (debug)",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "intro") {
                Text(
                    text = "Reads raw DAP IDs 100–130 on profile $profile. Green value = HAL answered " +
                        "(still needs a listening test); red = unsupported. " +
                        "Known IDs are references that must read.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item(key = "probe_all") {
                Button(
                    onClick = {
                        scope.launch {
                            probingAll = true
                            for ((id, _) in PROBE_TARGETS) {
                                results = results + (id to ProbeResult.Running)
                                val r = viewModel.probeDapParam(id, profile)
                                results = results + (id to r.fold(
                                    onSuccess = { ProbeResult.Value(it) },
                                    onFailure = { ProbeResult.Error(it.message ?: it.toString()) }
                                ))
                            }
                            probingAll = false
                        }
                    },
                    enabled = !probingAll,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (probingAll) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .width(18.dp)
                                .height(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (probingAll) "Probing…" else "Probe all")
                }
            }
            items(PROBE_TARGETS, key = { it.first }) { (id, hint) ->
                val result = results[id] ?: ProbeResult.Idle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Param $id",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        when (result) {
                            is ProbeResult.Idle -> Unit
                            is ProbeResult.Running -> {
                                Text(
                                    text = "reading…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            is ProbeResult.Value -> {
                                Text(
                                    text = "= ${result.v}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            is ProbeResult.Error -> {
                                Text(
                                    text = "unsupported (${result.msg.take(80)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    OutlinedButton(onClick = { probe(id) }) {
                        Text("Probe")
                    }
                }
            }
            item(key = "spacer") {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }
}
