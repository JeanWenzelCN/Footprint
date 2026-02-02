package com.footprint.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.*

private const val METABALL_THRESHOLD = 0.6f
private const val METABALL_RADIUS = 40f
private const val METABALL_INFLUENCE = 0.8f

@Composable
fun MetaballAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "Metaball")

    val anim1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val anim2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = min(size.width, size.height) / 4f

        val ball1 = Offset(
            center.x + cos(anim1) * radius,
            center.y + sin(anim1) * radius
        )
        val ball2 = Offset(
            center.x + cos(anim2) * radius * 0.7f,
            center.y + sin(anim2) * radius * 0.7f
        )
        
        val brush = Brush.horizontalGradient(listOf(Color(0xFF93C5FD), Color(0xFF581C87)))

        drawMetaball(brush, ball1, ball2)
    }
}

private fun DrawScope.drawMetaball(
    brush: Brush,
    ball1: Offset,
    ball2: Offset,
    radius: Float = METABALL_RADIUS
) {
    val d = (ball1 - ball2).getDistance()
    if (d > (radius * 2) + radius * METABALL_INFLUENCE) {
        drawCircle(brush, radius, ball1)
        drawCircle(brush, radius, ball2)
        return
    }

    val angle = atan2(ball2.y - ball1.y, ball2.x - ball1.x)
    val path = Path()

    // Points on circle 1
    val p1 = ball1 + Offset(cos(angle + PI / 2).toFloat() * radius, sin(angle + PI / 2).toFloat() * radius)
    val p2 = ball1 + Offset(cos(angle - PI / 2).toFloat() * radius, sin(angle - PI / 2).toFloat() * radius)

    // Points on circle 2
    val p3 = ball2 + Offset(cos(angle + PI / 2).toFloat() * radius, sin(angle + PI / 2).toFloat() * radius)
    val p4 = ball2 + Offset(cos(angle - PI / 2).toFloat() * radius, sin(angle - PI / 2).toFloat() * radius)
    
    // Control points for bezier curve
    val handleDist = min(d / 2f, radius * METABALL_THRESHOLD)
    val control1 = p1 + Offset(cos(angle).toFloat() * handleDist, sin(angle).toFloat() * handleDist)
    val control2 = p3 + Offset(cos(angle + PI).toFloat() * handleDist, sin(angle + PI).toFloat() * handleDist)
    val control3 = p4 + Offset(cos(angle + PI).toFloat() * handleDist, sin(angle + PI).toFloat() * handleDist)
    val control4 = p2 + Offset(cos(angle).toFloat() * handleDist, sin(angle).toFloat() * handleDist)

    path.moveTo(p1.x, p1.y)
    path.cubicTo(control1.x, control1.y, control2.x, control2.y, p3.x, p3.y)
    path.lineTo(p4.x, p4.y)
    path.cubicTo(control3.x, control3.y, control4.x, control4.y, p2.x, p2.y)
    path.close()

    drawPath(path, brush)
    drawCircle(brush, radius, ball1)
    drawCircle(brush, radius, ball2)
}

@Preview
@Composable
fun MetaballPreview() {
    Box(modifier = Modifier.size(200.dp)) {
        MetaballAnimation()
    }
}
