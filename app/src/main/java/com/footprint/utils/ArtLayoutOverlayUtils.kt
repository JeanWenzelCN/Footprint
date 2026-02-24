package com.footprint.utils

import android.content.Context
import android.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.footprint.ui.screens.art.ArtLayout
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

        val actualTextColor =
                when (uiState.artTextColor) {
                    "Black" -> android.graphics.Color.BLACK
                    "Gold" -> android.graphics.Color.parseColor("#FFCC00")
                    "Deep Blue" -> android.graphics.Color.parseColor("#007AFF")
                    "White" -> android.graphics.Color.WHITE
                    else -> android.graphics.Color.WHITE
                }

        val artName =
                uiState.artAuthorName.ifBlank {
                    if (layout == ArtLayout.POLAROID) "My Journey"
                    else if (layout == ArtLayout.GEEK_STATS) "DATA VISUALIZATION" else "漂泊的灵魂"
                }

        val dateRange =
                "${startDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"))} - ${endDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"))}"
        val metadata = "$dateRange • %.2f KM".format(totalDistanceKm)

        val textPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
                    val borderPaint =
                            Paint(textPaint).apply {
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
                val metaPaint =
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            textSize = (14f * scale).toFloat()
                            color = Color.White.copy(alpha = 0.7f).toArgb()
                            textAlign = Paint.Align.CENTER
                            typeface = customTypeface
                        }
                canvas.drawText(metadata, x, y + (30f * scale).toFloat(), metaPaint)
            }
            ArtLayout.POLAROID -> {
                // 1. Calculations & Materials
                val minP = width * 0.04f
                val maxP = width * 0.15f
                val sideP = minP + (maxP - minP) * uiState.polaroidFramePadding
                val topP = sideP
                val bottomP = height * (0.15f + 0.15f * uiState.polaroidFramePadding)

                val mapWidth = width - (sideP * 2)
                val mapHeight = height - topP - bottomP
                val mapLeft = sideP
                val mapTop = topP
                val mapRight = mapLeft + mapWidth
                val mapBottom = mapTop + mapHeight

                val frameColor =
                        when (uiState.polaroidFrameStyle) {
                            "CLASSIC_BLACK" -> android.graphics.Color.parseColor("#1A1A1A")
                            "LIQUID_GLASS" ->
                                    android.graphics.Color.argb(
                                            100,
                                            255,
                                            255,
                                            255
                                    ) // Semi-transparent white
                            else -> android.graphics.Color.parseColor("#FAFAFA")
                        }

                val paperPaint = Paint().apply { color = frameColor }

                // 2. Draw Frame
                canvas.drawRect(0f, 0f, width, mapTop, paperPaint) // Top
                canvas.drawRect(0f, mapBottom, width, height, paperPaint) // Bottom
                canvas.drawRect(0f, mapTop, mapLeft, mapBottom, paperPaint) // Left
                canvas.drawRect(mapRight, mapTop, width, mapBottom, paperPaint) // Right

                // 3. Inner Border
                if (uiState.polaroidInnerBorder > 0) {
                    val strokePaint =
                            Paint().apply {
                                style = Paint.Style.STROKE
                                strokeWidth = (uiState.polaroidInnerBorder * scale).toFloat()
                                color =
                                        if (uiState.polaroidFrameStyle == "CLASSIC_BLACK") {
                                            Color.White.copy(alpha = 0.2f).toArgb()
                                        } else if (uiState.polaroidFrameStyle == "LIQUID_GLASS") {
                                            Color.White.copy(alpha = 0.5f).toArgb()
                                        } else {
                                            Color.Black.copy(alpha = 0.15f).toArgb()
                                        }
                            }
                    canvas.drawRect(mapLeft, mapTop, mapRight, mapBottom, strokePaint)
                }

                // 4. Typography & Watermark Area
                val isBlack = uiState.polaroidFrameStyle == "CLASSIC_BLACK"
                val primaryTextColor =
                        if (isBlack) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                val secondaryTextColor = android.graphics.Color.GRAY

                // Main Title
                textPaint.apply {
                    textSize = (28f * scale).toFloat()
                    color = primaryTextColor
                    alpha = if (isBlack) 230 else 200
                }
                val centerX = width / 2
                val titleY = mapBottom + (60f * scale).toFloat()
                canvas.drawText(artName, centerX, titleY, textPaint)

                // Divider Line
                val linePaint =
                        Paint().apply {
                            color = primaryTextColor
                            alpha = 50
                            strokeWidth = (1f * scale).toFloat()
                        }
                val lineLen = (40f * scale).toFloat()
                val lineY = titleY + (20f * scale).toFloat()
                canvas.drawLine(
                        centerX - lineLen / 2,
                        lineY,
                        centerX + lineLen / 2,
                        lineY,
                        linePaint
                )

                // Bottom Metadata Row
                val metaY = lineY + (40f * scale).toFloat()

                // Left side: Date & Distance
                val metaPaint =
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            textSize = (11f * scale).toFloat()
                            color = secondaryTextColor
                            textAlign = Paint.Align.LEFT
                            typeface = customTypeface
                        }
                canvas.drawText(dateRange, mapLeft, metaY, metaPaint)
                metaPaint.apply {
                    textSize = (8f * scale).toFloat()
                    typeface = Typeface.MONOSPACE
                    alpha = 150
                }
                canvas.drawText(
                        "TOTAL DISTANCE: %.2f KM".format(totalDistanceKm),
                        mapLeft,
                        metaY + (15f * scale).toFloat(),
                        metaPaint
                )

                // Right side: Coords & Nickname Stamp
                val stampPaint =
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            textSize = (7f * scale).toFloat()
                            color = secondaryTextColor
                            textAlign = Paint.Align.RIGHT
                            typeface = Typeface.MONOSPACE
                            alpha = 150
                        }
                canvas.drawText("COORD: 31.23°N, 121.47°E", mapRight, metaY, stampPaint)

                // Red Stamp
                val stampText = uiState.userNickname.uppercase()
                val stampFontSize = (9f * scale).toFloat()
                stampPaint.apply {
                    textSize = stampFontSize
                    color = android.graphics.Color.parseColor("#FF453A")
                    alpha = 180
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val stampWidth = stampPaint.measureText(stampText)
                val stampRectPadding = (3f * scale).toFloat()
                val stampX = mapRight
                val stampY = metaY + (20f * scale).toFloat()

                // Stamp Border
                val borderPaint =
                        Paint(stampPaint).apply {
                            style = Paint.Style.STROKE
                            strokeWidth = (1f * scale).toFloat()
                        }
                val stampRect =
                        RectF(
                                stampX - stampWidth - stampRectPadding * 2,
                                stampY - stampFontSize,
                                stampX,
                                stampY + stampRectPadding
                        )
                canvas.drawRoundRect(
                        stampRect,
                        2f * scale.toFloat(),
                        2f * scale.toFloat(),
                        borderPaint
                )
                canvas.drawText(stampText, stampX - stampRectPadding, stampY, stampPaint)
            }
            ArtLayout.GEEK_STATS -> {
                val padding = (32f * scale).toFloat()
                val bgPaint = Paint().apply { color = Color.Black.copy(alpha = 0.7f).toArgb() }

                val rect =
                        RectF(
                                width - (400f * scale).toFloat(),
                                padding,
                                width - padding,
                                padding + (150f * scale).toFloat()
                        )
                canvas.drawRoundRect(rect, 8f * scale.toFloat(), 8f * scale.toFloat(), bgPaint)

                textPaint.textAlign = Paint.Align.LEFT
                val x = rect.left + (16f * scale).toFloat()
                val y = rect.top + (40f * scale).toFloat()

                textPaint.textSize = (18f * scale).toFloat()
                val artNameUpper = artName.uppercase()

                if (uiState.artTextBorder) {
                    val borderPaint =
                            Paint(textPaint).apply {
                                style = Paint.Style.STROKE
                                strokeWidth = (4f * scale).toFloat()
                                color = Color.Black.copy(alpha = 0.8f).toArgb()
                                strokeJoin = Paint.Join.ROUND
                            }
                    canvas.drawText(artNameUpper, x, y, borderPaint)
                }

                textPaint.color = actualTextColor
                canvas.drawText(artNameUpper, x, y, textPaint)

                val metaPaint =
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            textSize = (12f * scale).toFloat()
                            color = android.graphics.Color.WHITE
                            typeface = customTypeface
                        }
                canvas.drawText("DATE: $metadata", x, y + (30f * scale).toFloat(), metaPaint)
                canvas.drawText("MODE: TRACKING", x, y + (55f * scale).toFloat(), metaPaint)

                // Corner Brackets
                val color = Color.White.copy(alpha = 0.5f).toArgb()
                val bracketPaint =
                        Paint().apply {
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
