/*
 * Copyright (C) 2026 samakshkambxj
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.aospa.dolby.R

const val PAGE_HOME = 0
const val PAGE_EQUALIZER = 1
const val PAGE_ADVANCED = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DolbyApp(
    viewModel: DolbyViewModel,
    initialPage: Int = PAGE_HOME,
    pageRequest: Int? = null,
    onPageRequestConsumed: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    var page by rememberSaveable { mutableStateOf(initialPage) }
    var menuOpen by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(pageRequest) {
        val request = pageRequest ?: return@LaunchedEffect
        page = request
        onPageRequestConsumed()
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHost.showSnackbar(message)
        viewModel.dismissMessage()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (page) {
                            PAGE_EQUALIZER -> stringResource(R.string.dolby_preset)
                            PAGE_ADVANCED -> stringResource(R.string.advanced_title)
                            else -> stringResource(R.string.dolby_title)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.action_more),
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.about_title)) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Info, contentDescription = null)
                                },
                                onClick = {
                                    menuOpen = false
                                    showAbout = true
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 0.dp,
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                ) {
                    NavigationBarItem(
                        selected = page == PAGE_HOME,
                        onClick = { page = PAGE_HOME },
                        icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_home)) },
                    )
                    NavigationBarItem(
                        selected = page == PAGE_EQUALIZER,
                        onClick = { page = PAGE_EQUALIZER },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (state.userPresetActive) Badge()
                                },
                            ) {
                                Icon(Icons.Rounded.GraphicEq, contentDescription = null)
                            }
                        },
                        label = { Text(stringResource(R.string.nav_equalizer)) },
                    )
                    NavigationBarItem(
                        selected = page == PAGE_ADVANCED,
                        onClick = { page = PAGE_ADVANCED },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (state.hasTuning) Badge()
                                },
                            ) {
                                Icon(Icons.Rounded.Tune, contentDescription = null)
                            }
                        },
                        label = { Text(stringResource(R.string.nav_advanced)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Crossfade(
            targetState = page,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "page",
        ) { current ->
            when (current) {
                PAGE_EQUALIZER -> EqualizerScreen(state = state, viewModel = viewModel)
                PAGE_ADVANCED -> AdvancedScreen(state = state, viewModel = viewModel)
                else -> HomeScreen(
                    state = state,
                    onToggleEnabled = viewModel::setEnabled,
                    onProfileSelected = viewModel::setProfile,
                    onBassToggle = viewModel::setBassEnabled,
                    onLevelerToggle = viewModel::setLevelerEnabled,
                    onDialogueToggle = viewModel::setDialogueEnabled,
                    onVirtualizerToggle = viewModel::setActiveVirtualizer,
                    onOpenEqualizer = { page = PAGE_EQUALIZER },
                    onOpenAdvanced = { page = PAGE_ADVANCED },
                )
            }
        }
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (_: Exception) {
            null
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_title)) },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(stringResource(R.string.about_body))
                if (!version.isNullOrBlank()) {
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier.padding(top = 10.dp)
                    )
                    Text(
                        text = stringResource(R.string.about_version, version),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        },
    )
}
