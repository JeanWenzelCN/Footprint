package com.footprint.ui.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

/**
 * A Modifier that adds a "bouncy" click effect to any composable.
 * When pressed, the composable scales down, and when released, it springs back.
 *
 * @param strength The stiffness of the spring animation.
 */
fun Modifier.bouncyClick(strength: Float = 200f): Modifier = composed {
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    if (isPressed) {
        scope.launch {
            scale.animateTo(
                targetValue = 0.9f,
                animationSpec = spring(stiffness = strength)
            )
        }
    } else {
        scope.launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(stiffness = strength)
            )
        }
    }

    this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
