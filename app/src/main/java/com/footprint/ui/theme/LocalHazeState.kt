package com.footprint.ui.theme

import androidx.compose.runtime.compositionLocalOf
import dev.chrisbanes.haze.HazeState

/**
 * CompositionLocal to provide a [HazeState] throughout the component tree.
 * This avoids the need to pass the HazeState down through composable function parameters.
 */
val LocalHazeState = compositionLocalOf { HazeState() }