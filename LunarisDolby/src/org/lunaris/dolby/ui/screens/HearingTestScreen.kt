/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import org.lunaris.dolby.R
import org.lunaris.dolby.audio.HearingTonePlayer
import org.lunaris.dolby.audio.TestEar
import org.lunaris.dolby.data.DolbyRepository
import org.lunaris.dolby.domain.models.BandGain
import org.lunaris.dolby.ui.components.BouncyPopIn
import org.lunaris.dolby.ui.viewmodel.EqualizerViewModel
import org.lunaris.dolby.utils.ToastHelper
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.roundToInt

private val TEST_FREQS = listOf(250, 500, 1000, 2000, 4000, 8000)
private const val START_DB = -12f
private const val STEP_DB = 6f
private const val MIN_DB = -60f
private const val TICK_MS = 2200L
private const val MAX_COMP_DB = 6f

private enum class TestPhase { INTRO, TEST, RESULT }

/**
 * Personalized hearing test: pulsed tones per ear at descending levels,
 * thresholds mapped to a compensation curve saved as the "Personal" preset.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HearingTestScreen(
    viewModel: EqualizerViewModel,
    navController: NavController,
) {
    val context = LocalContext.current
    val player = remember(context.applicationContext) {
        HearingTonePlayer(context.applicationContext)
    }
    DisposableEffect(player) {
        onDispose { player.stop() }
    }

    var phase by remember { mutableStateOf(TestPhase.INTRO) }
    var earIdx by remember { mutableIntStateOf(0) }
    var freqIdx by remember { mutableIntStateOf(0) }
    var levelDb by remember { mutableFloatStateOf(START_DB) }
    var thresholds by remember {
        mutableStateOf(List(2) { MutableList<Float?>(TEST_FREQS.size) { null } })
    }
    var result by remember { mutableStateOf<List<BandGain>?>(null) }

    val ears = listOf(TestEar.LEFT, TestEar.RIGHT)
    val totalTrials = TEST_FREQS.size * 2
    val doneTrials = earIdx * TEST_FREQS.size + freqIdx

    fun advance() {
        var e = earIdx
        var f = freqIdx + 1
        if (f >= TEST_FREQS.size) {
            f = 0
            e += 1
        }
        if (e >= 2) {
            player.stop()
            result = buildCompensation(thresholds)
            phase = TestPhase.RESULT
        } else {
            earIdx = e
            freqIdx = f
        }
    }

    // (Re)start the tone on every new trial.
    LaunchedEffect(phase, earIdx, freqIdx) {
        if (phase == TestPhase.TEST) {
            levelDb = START_DB
            player.start(TEST_FREQS[freqIdx], ears[earIdx], START_DB)
        }
    }
    // Descending staircase until the floor.
    LaunchedEffect(phase, earIdx, freqIdx, levelDb) {
        if (phase == TestPhase.TEST && levelDb > MIN_DB) {
            delay(TICK_MS)
            val next = (levelDb - STEP_DB).coerceAtLeast(MIN_DB)
            levelDb = next
            player.setLevel(next)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hearing_test_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        player.stop()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (phase) {
                TestPhase.INTRO -> IntroBody(
                    onStart = { phase = TestPhase.TEST },
                )
                TestPhase.TEST -> TestBody(
                    ear = ears[earIdx],
                    freqHz = TEST_FREQS[freqIdx],
                    doneTrials = doneTrials,
                    totalTrials = totalTrials,
                    onLost = {
                        thresholds[earIdx][freqIdx] = levelDb
                        advance()
                    },
                    onReplay = {
                        levelDb = START_DB
                        player.setLevel(START_DB)
                    },
                    onSkip = {
                        thresholds[earIdx][freqIdx] = null
                        advance()
                    },
                )
                TestPhase.RESULT -> ResultBody(
                    gains = result ?: emptyList(),
                    onApply = {
                        result?.let {
                            viewModel.applyHearingGains(it)
                            ToastHelper.showToast(
                                context,
                                context.getString(R.string.hearing_applied),
                            )
                        }
                        navController.popBackStack()
                    },
                    onRetake = {
                        earIdx = 0
                        freqIdx = 0
                        thresholds = List(2) { MutableList<Float?>(TEST_FREQS.size) { null } }
                        result = null
                        phase = TestPhase.TEST
                    },
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun IntroBody(onStart: () -> Unit) {
    BouncyPopIn(key = "hearing_intro") {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.hearing_intro_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.hearing_intro_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.hearing_start))
            }
        }
    }
}

@Composable
private fun TestBody(
    ear: TestEar,
    freqHz: Int,
    doneTrials: Int,
    totalTrials: Int,
    onLost: () -> Unit,
    onReplay: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        LinearProgressIndicator(
            progress = doneTrials.toFloat() / totalTrials.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.onboarding_page_counter,
                doneTrials + 1,
                totalTrials,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (ear == TestEar.LEFT) "L" else "R",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (ear == TestEar.LEFT) {
                stringResource(R.string.hearing_left_ear)
            } else {
                stringResource(R.string.hearing_right_ear)
            },
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.hearing_freq, freqHz),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.hearing_listen_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onLost,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.hearing_lost))
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            OutlinedButton(onClick = onReplay) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.hearing_replay))
            }
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.hearing_skip))
            }
        }
    }
}

@Composable
private fun ResultBody(
    gains: List<BandGain>,
    onApply: () -> Unit,
    onRetake: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.hearing_result_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.hearing_result_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                val peak = gains.maxOfOrNull { abs(it.gain / 10f) }?.coerceAtLeast(0.5f) ?: 1f
                gains.forEach { band ->
                    val db = band.gain / 10f
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp),
                    ) {
                        Text(
                            text = if (band.frequency >= 1000) {
                                "${band.frequency / 1000}k"
                            } else {
                                "${band.frequency}"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.width(44.dp),
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((db / peak).coerceIn(0f, 1f))
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "+%.1f".format(db),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(52.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.hearing_apply))
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            OutlinedButton(onClick = onRetake) {
                Text(stringResource(R.string.hearing_retake))
            }
            TextButton(onClick = onDone) {
                Text(stringResource(R.string.hearing_done))
            }
        }
    }
}

/**
 * Worse-ear thresholds -> compensation curve on the 10 Lunaris bands.
 * Skipped trials contribute no boost.
 */
private fun buildCompensation(thresholds: List<List<Float?>>): List<BandGain> {
    val freqs = DolbyRepository.BAND_FREQUENCIES_10
    // Per spot frequency: worse ear wins; fully skipped -> no boost.
    val spotWorst: List<Float?> = TEST_FREQS.map { hz ->
        val idx = TEST_FREQS.indexOf(hz)
        listOfNotNull(thresholds[0][idx], thresholds[1][idx]).maxOrNull()
    }
    val best = spotWorst.filterNotNull().minOrNull() ?: MIN_DB
    val spotComp = spotWorst.map { worst ->
        if (worst == null) 0f else ((worst - best) * 0.6f).coerceIn(0f, MAX_COMP_DB)
    }
    return freqs.map { bandHz ->
        val comp = interpLog(bandHz.toFloat(), TEST_FREQS, spotComp)
        BandGain(frequency = bandHz, gain = (comp * 10f).roundToInt())
    }
}

private fun interpLog(x: Float, xs: List<Int>, ys: List<Float>): Float {
    val lx = log10(x)
    val lxs = xs.map { log10(it.toFloat()) }
    if (lx <= lxs.first()) return ys.first()
    if (lx >= lxs.last()) return ys.last()
    for (i in 0 until lxs.size - 1) {
        if (lx in lxs[i]..lxs[i + 1]) {
            val t = (lx - lxs[i]) / (lxs[i + 1] - lxs[i])
            return ys[i] * (1 - t) + ys[i + 1] * t
        }
    }
    return 0f
}
