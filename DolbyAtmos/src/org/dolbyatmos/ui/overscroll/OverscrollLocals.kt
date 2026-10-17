/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.dolbyatmos.ui.overscroll

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Publishes the vendored platform overscroll [StretchOverscrollCompensationState]
 * so consumers (e.g. the liquid-glass backdrop) can compensate for an in-flight
 * stretch. Mirrors OriginSU's `LocalStretchOverscrollCompensationState`.
 *
 * Null when no custom overscroll factory is installed.
 */
val LocalStretchOverscrollCompensationState =
    staticCompositionLocalOf<StretchOverscrollCompensationState?> { null }
