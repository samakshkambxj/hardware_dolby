/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.domain.models

/**
 * A Scene is a named snapshot of the most relevant Dolby settings that can be
 * applied in one tap. Scenes intentionally capture enhancer-level settings
 * (profile, IEQ, bass/mid/treble, leveler, dialogue, virtualizers) and not raw
 * GEQ band gains, because the bass/mid/treble setters deterministically derive
 * GEQ deltas from the stored levels.
 */
data class Scene(
    val id: String,
    val name: String,
    val isBuiltIn: Boolean = false,
    val enabled: Boolean = true,
    val profile: Int = 0,
    val ieqPreset: Int = 0,
    val bassLevel: Int = 0,
    val bassCurve: Int = 0,
    val midLevel: Int = 0,
    val trebleLevel: Int = 0,
    val volumeLeveler: Boolean = false,
    val dialogueEnabled: Boolean = false,
    val dialogueAmount: Int = 6,
    val hpVirtualizer: Boolean = false,
    val spkVirtualizer: Boolean = false,
    val stereoWidening: Int = 32
)
