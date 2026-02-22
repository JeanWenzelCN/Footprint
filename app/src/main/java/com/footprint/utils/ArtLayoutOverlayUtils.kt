package com.footprint.utils

import android.content.Context
import android.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.footprint.ui.screens.art.ArtLayout
import com.footprint.FootprintViewModel
import java.time.LocalDate

object ArtLayoutOverlayUtils {

    fun drawOverlay(
        context: Context,
        canvas: Canvas,
        bitmap: Bitmap,
        layout: ArtLayout,
        uiState: com.footprint.ui.state.FootprintUiState,
        totalDistanceKm: Double,
        startDate: LocalDate,
        endDate: LocalDate,
        customTypeface: Typeface,
        scale: Double
    ) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()

        val actualTextColor = when (uiState.artTextColor) {
            "Black" -> android.graphics.Color.BLACK
            "Gold" -> android.graphics.Color.parseColor("#FFCC00")
            "Deep Blue" -> android.graphics.Color.parseColor("#007AFF")
            "White" -> android.graphics.Color.WHITE
            else -> android.graphics.Color.WHITE
        }

        val artName = uiState.artAuthorName.ifBlank { 
            if (layout == ArtLayout.POLAROID) "My Journey" else if (layout == ArtLayout.GEEK_STATS) "DATA VISUALIZATION" else "漂泊的灵魂" 
        }
        
        val dateRange = "${startDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"))} - ${endDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"))}"
        val metadata = "$dateRange • %.2f KM".format(totalDistanceKm)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = customTypeface
            textAlign = Paint.Align.CENTER
        }

        when (layout) {
            ArtLayout.FULLCREEN_A24 -> {
                val fontSize = (40f * scale).toFloat()
                textPaint.textSize = fontSize
                
                val x = width / 2
                val y = height - (150f * scale).toFloat()

                // Draw Border
                if (uiState.artTextBorder) {
                    val borderPaint = Paint(textPaint).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = (6f * scale).toFloat()
                        color = Color.Black.copy(alpha = 0.8f).toArgb()
                        strokeJoin = Paint.Join.ROUND
                    }
                    canvas.drawText(artName, x, y, borderPaint)
                }

                // Draw Text
                textPaint.color = actualTextColor
                canvas.drawText(artName, x, y, textPaint)

                // Draw Metadata
                val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = (14f * scale).toFloat()
                    color = Color.White.copy(alpha = 0.7f).toArgb()
                    textAlign = Paint.Align.CENTER
                    typeface = customTypeface
                }
                canvas.drawText(metadata, x, y + (30f * scale).toFloat(), metaPaint)
            }
            ArtLayout.POLAROID -> {
                // Polaroid Paper
                val paperPaint = Paint().apply { color = android.graphics.Color.parseColor("#FAFAFA") }
                
                val mapWidth = width * 0.9f
                val mapHeight = mapWidth // Square
                val mapLeft = (width - mapWidth) / 2
                val mapTop = height * 0.15f
                val mapBottom = mapTop + mapHeight

                // Draw Frame
                canvas.drawRect(0f, 0f, width, mapTop, paperPaint) // Top
                canvas.drawRect(0f, mapBottom, width, height, paperPaint) // Bottom
                canvas.drawRect(0f, mapTop, mapLeft, mapBottom, paperPaint) // Left
                canvas.drawRect(width - mapLeft, mapTop, width, mapBottom, paperPaint) // Right

                // Inner Border
                val strokePaint = Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = (2f * scale).toFloat()
                    color = Color.Black.copy(alpha = 0.1f).toArgb()
                }
                canvas.drawRect(mapLeft, mapTop, mapLeft + mapWidth, mapBottom, strokePaint)

                // Text
                val fontSize = (36f * scale).toFloat()
                textPaint.textSize = fontSize
                val x = width / 2
                val y = height - (160f * scale).toFloat()

                if (uiState.artTextBorder) {
                    val borderPaint = Paint(textPaint).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = (6f * scale).toFloat()
                        color = Color.Black.copy(alpha = 0.8f).toArgb()
                        strokeJoin = Paint.Join.ROUND
                    }
                    canvas.drawText(artName, x, y, borderPaint)
                }

                textPaint.color = actualTextColor
                canvas.drawText(artName, x, y, textPaint)

                val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = (14f * scale).toFloat()
                    color = android.graphics.Color.GRAY
                    textAlign = Paint.Align.CENTER
                    typeface = customTypeface
                }
                canvas.drawText(metadata, x, y + (30f * scale).toFloat(), metaPaint)
            }
            ArtLayout.GEEK_STATS -> {
                val padding = (32f * scale).toFloat()
                val bgPaint = Paint().apply { color = Color.Black.copy(alpha = 0.7f).toArgb() }
                
                val rect = RectF(width - (400f * scale).toFloat(), padding, width - padding, padding + (150f * scale).toFloat())
                canvas.drawRoundRect(rect, 8f * scale.toFloat(), 8f * scale.toFloat(), bgPaint)

                textPaint.textAlign = Paint.Align.LEFT
                val x = rect.left + (16f * scale).toFloat()
                val y = rect.top + (40f * scale).toFloat()

                textPaint.textSize = (18f * scale).toFloat()
                val artNameUpper = artName.uppercase()

                if (uiState.artTextBorder) {
                    val borderPaint = Paint(textPaint).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = (4f * scale).toFloat()
                        color = Color.Black.copy(alpha = 0.8f).toArgb()
                        strokeJoin = Paint.Join.ROUND
                    }
                    canvas.drawText(artNameUpper, x, y, borderPaint)
                }

                textPaint.color = actualTextColor
                canvas.drawText(artNameUpper, x, y, textPaint)

                val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = (12f * scale).toFloat()
                    color = android.graphics.Color.WHITE
                    typeface = customTypeface
                }
                canvas.drawText("DATE: $metadata", x, y + (30f * scale).toFloat(), metaPaint)
                canvas.drawText("MODE: TRACKING", x, y + (55f * scale).toFloat(), metaPaint)

                // Corner Brackets
                val color = Color.White.copy(alpha = 0.5f).toArgb()
                val bracketPaint = Paint().apply {
                    this.color = color
                    strokeWidth = (2f * scale).toFloat()
                }
                val len = (40f * scale).toFloat()
                
                // Top Left
                canvas.drawLine(0f, 0f, len, 0f, bracketPaint)
                canvas.drawLine(0f, 0f, 0f, len, bracketPaint)

                // Bottom Right
                canvas.drawLine(width, height, width - len, height, bracketPaint)
                canvas.drawLine(width, height, width, height - len, bracketPaint)
            }
        }
    }
}
