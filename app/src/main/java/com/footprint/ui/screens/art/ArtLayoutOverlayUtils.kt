package com.footprint.ui.screens.art

import android.content.Context
import android.graphics.*
import com.footprint.ui.screens.art.ArtLayout
import com.footprint.ui.state.FootprintUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ArtLayoutOverlayUtils {

    /**
     * Draws the art layout overlay onto a native Canvas for high-res export.
     */
    fun drawOverlay(
        context: Context,
        canvas: Canvas,
        bitmap: Bitmap,
        layout: ArtLayout,
        uiState: FootprintUiState,
        totalDistanceKm: Double,
        startDate: LocalDate,
        endDate: LocalDate,
        customTypeface: Typeface,
        scale: Double
    ) {
        val textCValue = when (uiState.artTextColor) {
            "Black" -> Color.BLACK
            "Gold" -> Color.parseColor("#FFCC00")
            "Deep Blue" -> Color.parseColor("#007AFF")
            else -> Color.WHITE
        }

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        val dateRangeStr = "${startDate.format(dateFormatter)} - ${endDate.format(dateFormatter)}"
        
        val subtitleStr = when (layout) {
            ArtLayout.GEEK_STATS -> "DATE: $dateRangeStr • ${String.format("%.2f", totalDistanceKm)} KM"
            else -> "$dateRangeStr • ${String.format("%.2f", totalDistanceKm)} KM"
        }

        val textPaint = Paint().apply {
            color = textCValue
            isAntiAlias = true
            typeface = customTypeface
            setShadowLayer(8f * scale.toFloat(), 0f, 6f * scale.toFloat(), Color.BLACK)
        }

        val subtitlePaint = Paint().apply {
            color = textCValue
            isAntiAlias = true
            typeface = customTypeface
            setShadowLayer(4f * scale.toFloat(), 0f, 3f * scale.toFloat(), Color.BLACK)
        }

        when (layout) {
            ArtLayout.FULLCREEN_A24 -> {
                textPaint.textSize = 240f * scale.toFloat()
                textPaint.letterSpacing = 0.05f
                subtitlePaint.textSize = 48f * scale.toFloat()
                subtitlePaint.letterSpacing = 0.02f
                subtitlePaint.alpha = 180

                val titleStr = uiState.artAuthorName.ifBlank { "漂泊的灵魂" }
                val textWidth = textPaint.measureText(titleStr)
                val subWidth = subtitlePaint.measureText(subtitleStr)

                val y_base = bitmap.height - (180f * scale.toFloat())
                val x_title = (bitmap.width - textWidth) / 2f
                val x_sub = (bitmap.width - subWidth) / 2f

                if (uiState.artTextBorder) {
                    val borderPaint = Paint(textPaint).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 6f * scale.toFloat()
                        color = Color.argb(204, 0, 0, 0)
                    }
                    canvas.drawText(titleStr, x_title, y_base, borderPaint)
                }
                canvas.drawText(titleStr, x_title, y_base, textPaint)
                canvas.drawText(subtitleStr, x_sub, y_base + (100f * scale.toFloat()), subtitlePaint)
            }
            ArtLayout.POLAROID -> {
                val framePaint = Paint().apply { color = Color.parseColor("#FAFAFA") }
                val mapWidth = bitmap.width * 0.9f
                val mapHeight = mapWidth
                val mapLeft = (bitmap.width - mapWidth) / 2f
                val mapTop = bitmap.height * 0.15f
                val mapBottom = mapTop + mapHeight

                canvas.drawRect(0f, 0f, bitmap.width.toFloat(), mapTop, framePaint)
                canvas.drawRect(0f, mapBottom, bitmap.width.toFloat(), bitmap.height.toFloat(), framePaint)
                canvas.drawRect(0f, mapTop, mapLeft, mapBottom, framePaint)
                canvas.drawRect(mapLeft + mapWidth, mapTop, bitmap.width.toFloat(), mapBottom, framePaint)

                val shadowPaint = Paint().apply {
                    color = Color.argb(25, 0, 0, 0)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f * scale.toFloat()
                }
                canvas.drawRect(mapLeft, mapTop, mapLeft + mapWidth, mapTop + mapHeight, shadowPaint)

                textPaint.textSize = 240f * scale.toFloat()
                textPaint.color = Color.BLACK
                textPaint.letterSpacing = 0.02f
                subtitlePaint.textSize = 48f * scale.toFloat()
                subtitlePaint.color = Color.GRAY

                val titleStr = uiState.artAuthorName.ifBlank { "My Journey" }
                val textWidth = textPaint.measureText(titleStr)
                val subWidth = subtitlePaint.measureText(subtitleStr)

                val x_title = (bitmap.width - textWidth) / 2f
                val x_sub = (bitmap.width - subWidth) / 2f
                val y_base = bitmap.height - (180f * scale.toFloat())

                if (uiState.artTextBorder) {
                    val borderPaint = Paint(textPaint).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 6f * scale.toFloat()
                        color = Color.argb(50, 0, 0, 0)
                    }
                    canvas.drawText(titleStr, x_title, y_base, borderPaint)
                }
                canvas.drawText(titleStr, x_title, y_base, textPaint)
                canvas.drawText(subtitleStr, x_sub, y_base + (100f * scale.toFloat()), subtitlePaint)
            }
            ArtLayout.GEEK_STATS -> {
                val density = context.resources.displayMetrics.density
                val margin = 16f * density * scale.toFloat()
                val padding = 16f * density * scale.toFloat()

                textPaint.textSize = 32f * scale.toFloat()
                subtitlePaint.textSize = 32f * scale.toFloat()

                val titleStr = uiState.artAuthorName.uppercase().ifBlank { "DATA VISUALIZATION" }
                val modeStr = "MODE: TRACKING"

                val titleWidth = textPaint.measureText(titleStr)
                val subWidth = subtitlePaint.measureText(subtitleStr)
                val modeWidth = subtitlePaint.measureText(modeStr)

                val boxWidth = maxOf(titleWidth, subWidth, modeWidth) + padding * 2
                val boxHeight = textPaint.textSize + subtitlePaint.textSize * 2 + padding * 2 + (8f * scale.toFloat()) * 2

                val boxLeft = bitmap.width.toFloat() - margin - boxWidth
                val boxTop = margin

                val boxPaint = Paint().apply { color = Color.argb(178, 0, 0, 0) }
                val cornerRadius = 8f * density * scale.toFloat()
                canvas.drawRoundRect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight, cornerRadius, cornerRadius, boxPaint)

                val tx = boxLeft + padding
                var ty = boxTop + padding + textPaint.textSize * 0.8f

                if (uiState.artTextBorder) {
                    val borderPaint = Paint(textPaint).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 4f * scale.toFloat()
                        color = Color.argb(204, 0, 0, 0)
                    }
                    canvas.drawText(titleStr, tx, ty, borderPaint)
                }
                canvas.drawText(titleStr, tx, ty, textPaint)

                ty += (8f * scale.toFloat()) + subtitlePaint.textSize
                canvas.drawText(subtitleStr, tx, ty, subtitlePaint)

                ty += (8f * scale.toFloat()) + subtitlePaint.textSize
                canvas.drawText(modeStr, tx, ty, subtitlePaint)

                // Decorative corner brackets
                val bracketPaint = Paint().apply {
                    color = Color.WHITE
                    alpha = 128
                    style = Paint.Style.STROKE
                    strokeWidth = 2f * density * scale.toFloat()
                }
                val bLen = 40f * density * scale.toFloat()
                // Top Left
                canvas.drawLine(0f, 0f, bLen, 0f, bracketPaint)
                canvas.drawLine(0f, 0f, 0f, bLen, bracketPaint)
                // Bottom Right
                canvas.drawLine(bitmap.width.toFloat(), bitmap.height.toFloat(), bitmap.width.toFloat() - bLen, bitmap.height.toFloat(), bracketPaint)
                canvas.drawLine(bitmap.width.toFloat(), bitmap.height.toFloat(), bitmap.width.toFloat(), bitmap.height.toFloat() - bLen, bracketPaint)
            }
        }
    }
}
