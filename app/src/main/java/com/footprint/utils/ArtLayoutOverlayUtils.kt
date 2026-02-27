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
                            "LIQUID_GLASS" -> {
                                when (uiState.glassTint) {
                                    "Cyan" -> android.graphics.Color.argb(40, 0, 255, 255)
                                    "Gold" -> android.graphics.Color.argb(40, 255, 215, 0)
                                    "Rose" -> android.graphics.Color.argb(40, 255, 182, 193)
                                    else -> android.graphics.Color.argb(30, 255, 255, 255)
                                }
                            }
                            else -> android.graphics.Color.parseColor("#FAFAFA")
                        }

                val paperPaint = Paint().apply { color = frameColor }

                // 2. Draw Frame
                if (uiState.polaroidFrameStyle == "LIQUID_GLASS") {
                    val blurR = (uiState.frostRadius * scale).toInt().coerceIn(1, 100)
                    val blurredMap = blurBitmap(bitmap, blurR)

                    // Draw blurred map in the frame area (Top, Bottom, Left, Right)
                    // We use the same RectF for both src and dst (relative to coordinate system)
                    // since blurredMap is same size as original map bitmap.

                    // Top segment
                    canvas.drawBitmap(
                            blurredMap,
                            Rect(0, 0, bitmap.width, mapTop.toInt()),
                            RectF(0f, 0f, width, mapTop),
                            null
                    )

                    // Bottom segment
                    canvas.drawBitmap(
                            blurredMap,
                            Rect(0, mapBottom.toInt(), bitmap.width, bitmap.height),
                            RectF(0f, mapBottom, width, height),
                            null
                    )

                    // Left segment (middle part)
                    canvas.drawBitmap(
                            blurredMap,
                            Rect(0, mapTop.toInt(), mapLeft.toInt(), mapBottom.toInt()),
                            RectF(0f, mapTop, mapLeft, mapBottom),
                            null
                    )

                    // Right segment (middle part)
                    canvas.drawBitmap(
                            blurredMap,
                            Rect(mapRight.toInt(), mapTop.toInt(), bitmap.width, mapBottom.toInt()),
                            RectF(mapRight, mapTop, width, mapBottom),
                            null
                    )
                }

                canvas.drawRect(0f, 0f, width, mapTop, paperPaint) // Top Tint
                canvas.drawRect(0f, mapBottom, width, height, paperPaint) // Bottom Tint
                canvas.drawRect(0f, mapTop, mapLeft, mapBottom, paperPaint) // Left Tint
                canvas.drawRect(mapRight, mapTop, width, mapBottom, paperPaint) // Right Tint

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
                                            val tintColor =
                                                    when (uiState.glassTint) {
                                                        "Cyan" ->
                                                                android.graphics.Color.argb(
                                                                        180,
                                                                        0,
                                                                        255,
                                                                        255
                                                                )
                                                        "Gold" ->
                                                                android.graphics.Color.argb(
                                                                        180,
                                                                        255,
                                                                        215,
                                                                        0
                                                                )
                                                        "Rose" ->
                                                                android.graphics.Color.argb(
                                                                        180,
                                                                        255,
                                                                        182,
                                                                        193
                                                                )
                                                        else ->
                                                                android.graphics.Color.argb(
                                                                        150,
                                                                        255,
                                                                        255,
                                                                        255
                                                                )
                                                    }
                                            tintColor
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
                            color =
                                    if (uiState.polaroidFrameStyle == "LIQUID_GLASS")
                                            android.graphics.Color.BLACK
                                    else secondaryTextColor
                            alpha = if (uiState.polaroidFrameStyle == "LIQUID_GLASS") 150 else 255
                            textAlign = Paint.Align.LEFT
                            typeface = customTypeface
                        }
                canvas.drawText(dateRange, mapLeft, metaY, metaPaint)

                if (uiState.polaroidFrameStyle == "LIQUID_GLASS") {
                    val now = java.time.LocalTime.now()
                    val timeStr =
                            now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                    metaPaint.apply {
                        textSize = (8f * scale).toFloat()
                        typeface = Typeface.MONOSPACE
                        alpha = 100
                    }
                    canvas.drawText(
                            "TIMESTAMP: $timeStr",
                            mapLeft,
                            metaY + (12f * scale).toFloat(),
                            metaPaint
                    )
                }

                metaPaint.apply {
                    textSize = (8f * scale).toFloat()
                    typeface = Typeface.MONOSPACE
                    alpha = if (uiState.polaroidFrameStyle == "LIQUID_GLASS") 180 else 150
                }
                canvas.drawText(
                        "TOTAL DISTANCE: %.2f KM".format(totalDistanceKm),
                        mapLeft,
                        metaY +
                                (if (uiState.polaroidFrameStyle == "LIQUID_GLASS") 24f else 15f) *
                                        scale.toFloat(),
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

    private fun blurBitmap(bitmap: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return bitmap
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val w = result.width
        val h = result.height
        val pix = IntArray(w * h)
        result.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(Math.max(w, h))
        val divsum = (div + 1) shr 1 * ((div + 1) shr 1)
        val dv = IntArray(256 * divsum)
        for (i in 0 until 256 * divsum) dv[i] = i / divsum

        yw = 0
        yi = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        for (y in 0 until h) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            for (i in -radius..radius) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))]
                sir = stack[i + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
            }
            stackpointer = radius

            for (x in 0 until w) {

                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]

                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
            }
            yw += w
        }
        for (x in 0 until w) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            yp = -radius * w
            for (i in -radius..radius) {
                yi = Math.max(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - Math.abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) {
                    yp += w
                }
            }
            yi = x
            stackpointer = radius
            for (y in 0 until h) {
                // Preserve original alpha if possible, or force opaque
                pix[yi] = (-0x1000000 or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum])
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w
                }
                p = x + vmin[y]
                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi += w
            }
        }
        result.setPixels(pix, 0, w, 0, 0, w, h)
        return result
    }
}
