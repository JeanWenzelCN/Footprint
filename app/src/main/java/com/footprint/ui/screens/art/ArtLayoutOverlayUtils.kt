package com.footprint.ui.screens.art

import android.content.Context
import android.graphics.*
import com.footprint.ui.state.FootprintUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ArtLayoutOverlayUtils {

        /** Draws the art layout overlay onto a native Canvas for high-res export. */
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
                val textCValue =
                        when (uiState.artTextColor) {
                                "Black" -> Color.BLACK
                                "Gold" -> Color.parseColor("#FFCC00")
                                "Deep Blue" -> Color.parseColor("#007AFF")
                                else -> Color.WHITE
                        }

                val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
                val dateRangeStr =
                        "${startDate.format(dateFormatter)} - ${endDate.format(dateFormatter)}"

                val subtitleStr =
                        when (layout) {
                                ArtLayout.GEEK_STATS ->
                                        "DATE: $dateRangeStr • ${String.format("%.2f", totalDistanceKm)} KM"
                                else ->
                                        "$dateRangeStr • ${String.format("%.2f", totalDistanceKm)} KM"
                        }

                val textPaint =
                        Paint().apply {
                                color = textCValue
                                isAntiAlias = true
                                typeface = customTypeface
                                setShadowLayer(
                                        8f * scale.toFloat(),
                                        0f,
                                        6f * scale.toFloat(),
                                        Color.BLACK
                                )
                        }

                val subtitlePaint =
                        Paint().apply {
                                color = textCValue
                                isAntiAlias = true
                                typeface = customTypeface
                                setShadowLayer(
                                        4f * scale.toFloat(),
                                        0f,
                                        3f * scale.toFloat(),
                                        Color.BLACK
                                )
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
                                        val borderPaint =
                                                Paint(textPaint).apply {
                                                        style = Paint.Style.STROKE
                                                        strokeWidth = 6f * scale.toFloat()
                                                        color = Color.argb(204, 0, 0, 0)
                                                }
                                        canvas.drawText(titleStr, x_title, y_base, borderPaint)
                                }
                                canvas.drawText(titleStr, x_title, y_base, textPaint)
                                canvas.drawText(
                                        subtitleStr,
                                        x_sub,
                                        y_base + (100f * scale.toFloat()),
                                        subtitlePaint
                                )
                        }
                        ArtLayout.POLAROID -> {
                                val woodBaseColor =
                                        when (uiState.woodType) {
                                                com.footprint.ui.screens.art.WoodType.ASH ->
                                                        Color.parseColor("#E5D3B3")
                                                com.footprint.ui.screens.art.WoodType.WALNUT ->
                                                        Color.parseColor("#5D4037")
                                                com.footprint.ui.screens.art.WoodType.VINTAGE_OAK ->
                                                        Color.parseColor("#D2B48C")
                                        }
                                val frameColor =
                                        when (uiState.polaroidFrameStyle) {
                                                "CLASSIC_BLACK" -> Color.parseColor("#1A1A1A")
                                                "LIQUID_GLASS" -> Color.argb(120, 255, 255, 255)
                                                "ACOUSTIC_WOOD" -> woodBaseColor
                                                "HEAVY_MECHANICAL" -> Color.parseColor("#455A64")
                                                "CYBER_GLITCH" -> Color.parseColor("#0D0D0D")
                                                else -> Color.parseColor("#FAFAFA")
                                        }
                                val framePaint = Paint().apply { color = frameColor }

                                val mapWidth = bitmap.width * 0.9f
                                val mapHeight = mapWidth
                                val mapLeft = (bitmap.width - mapWidth) / 2f
                                val mapTop = bitmap.height * 0.15f
                                val mapBottom = mapTop + mapHeight

                                // For LIQUID_GLASS, we draw a blurred version of the map in the
                                // frame area
                                if (uiState.polaroidFrameStyle == "LIQUID_GLASS") {
                                        val blurRadius = 40f * scale.toFloat()
                                        val blurredMap = blurBitmap(bitmap, blurRadius)
                                        if (blurredMap != null) {
                                                // Draw blurred background only in frame area
                                                canvas.save()
                                                val framePath =
                                                        Path().apply {
                                                                addRect(
                                                                        0f,
                                                                        0f,
                                                                        bitmap.width.toFloat(),
                                                                        bitmap.height.toFloat(),
                                                                        Path.Direction.CW
                                                                )
                                                                addRect(
                                                                        mapLeft,
                                                                        mapTop,
                                                                        mapLeft + mapWidth,
                                                                        mapTop + mapHeight,
                                                                        Path.Direction.CCW
                                                                )
                                                        }
                                                canvas.clipPath(framePath)
                                                canvas.drawBitmap(blurredMap, 0f, 0f, null)
                                                canvas.restore()
                                                blurredMap.recycle()
                                        }
                                }

                                // Draw Frame
                                canvas.drawRect(0f, 0f, bitmap.width.toFloat(), mapTop, framePaint)
                                canvas.drawRect(
                                        0f,
                                        mapBottom,
                                        bitmap.width.toFloat(),
                                        bitmap.height.toFloat(),
                                        framePaint
                                )
                                canvas.drawRect(0f, mapTop, mapLeft, mapBottom, framePaint)
                                canvas.drawRect(
                                        mapLeft + mapWidth,
                                        mapTop,
                                        bitmap.width.toFloat(),
                                        mapBottom,
                                        framePaint
                                )

                                // Material Textures
                                when (uiState.polaroidFrameStyle) {
                                        "ACOUSTIC_WOOD" -> {
                                                val grainPaint =
                                                        Paint().apply {
                                                                color = Color.BLACK
                                                                alpha = 15
                                                                strokeWidth = 2f * scale.toFloat()
                                                        }

                                                // 1. Miter Joints & Directional Grain
                                                val pathTop =
                                                        Path().apply {
                                                                moveTo(0f, 0f)
                                                                lineTo(bitmap.width.toFloat(), 0f)
                                                                lineTo(mapLeft + mapWidth, mapTop)
                                                                lineTo(mapLeft, mapTop)
                                                                close()
                                                        }
                                                val pathBottom =
                                                        Path().apply {
                                                                moveTo(0f, bitmap.height.toFloat())
                                                                lineTo(
                                                                        bitmap.width.toFloat(),
                                                                        bitmap.height.toFloat()
                                                                )
                                                                lineTo(
                                                                        mapLeft + mapWidth,
                                                                        mapBottom
                                                                )
                                                                lineTo(mapLeft, mapBottom)
                                                                close()
                                                        }
                                                val pathLeft =
                                                        Path().apply {
                                                                moveTo(0f, 0f)
                                                                lineTo(0f, bitmap.height.toFloat())
                                                                lineTo(mapLeft, mapBottom)
                                                                lineTo(mapLeft, mapTop)
                                                                close()
                                                        }
                                                val pathRight =
                                                        Path().apply {
                                                                moveTo(bitmap.width.toFloat(), 0f)
                                                                lineTo(
                                                                        bitmap.width.toFloat(),
                                                                        bitmap.height.toFloat()
                                                                )
                                                                lineTo(
                                                                        mapLeft + mapWidth,
                                                                        mapBottom
                                                                )
                                                                lineTo(mapLeft + mapWidth, mapTop)
                                                                close()
                                                        }

                                                fun drawSectionGrain(
                                                        path: Path,
                                                        isVertical: Boolean
                                                ) {
                                                        canvas.save()
                                                        canvas.clipPath(path)
                                                        val density =
                                                                if (uiState.woodType ==
                                                                                com.footprint.ui
                                                                                        .screens.art
                                                                                        .WoodType
                                                                                        .VINTAGE_OAK
                                                                )
                                                                        80
                                                                else 50
                                                        for (i in 0 until density) {
                                                                val offset =
                                                                        (i.toFloat() / density) *
                                                                                (if (isVertical)
                                                                                        bitmap.width
                                                                                                .toFloat()
                                                                                else
                                                                                        bitmap.height
                                                                                                .toFloat())
                                                                if (isVertical) {
                                                                        canvas.drawLine(
                                                                                offset,
                                                                                0f,
                                                                                offset +
                                                                                        bitmap.width *
                                                                                                0.02f,
                                                                                bitmap.height
                                                                                        .toFloat(),
                                                                                grainPaint
                                                                        )
                                                                } else {
                                                                        canvas.drawLine(
                                                                                0f,
                                                                                offset,
                                                                                bitmap.width
                                                                                        .toFloat(),
                                                                                offset +
                                                                                        bitmap.height *
                                                                                                0.02f,
                                                                                grainPaint
                                                                        )
                                                                }
                                                        }
                                                        canvas.restore()
                                                }

                                                drawSectionGrain(pathTop, false)
                                                drawSectionGrain(pathBottom, false)
                                                drawSectionGrain(pathLeft, true)
                                                drawSectionGrain(pathRight, true)

                                                // 2. Bevel & Emboss Highlights
                                                val bevelPaint =
                                                        Paint().apply {
                                                                strokeWidth = 3f * scale.toFloat()
                                                        }
                                                bevelPaint.color = Color.argb(60, 255, 255, 255)
                                                canvas.drawLine(
                                                        0f,
                                                        0f,
                                                        bitmap.width.toFloat(),
                                                        0f,
                                                        bevelPaint
                                                )
                                                canvas.drawLine(
                                                        0f,
                                                        0f,
                                                        0f,
                                                        bitmap.height.toFloat(),
                                                        bevelPaint
                                                )
                                                bevelPaint.color = Color.argb(60, 0, 0, 0)
                                                canvas.drawLine(
                                                        bitmap.width.toFloat(),
                                                        0f,
                                                        bitmap.width.toFloat(),
                                                        bitmap.height.toFloat(),
                                                        bevelPaint
                                                )
                                                canvas.drawLine(
                                                        0f,
                                                        bitmap.height.toFloat(),
                                                        bitmap.width.toFloat(),
                                                        bitmap.height.toFloat(),
                                                        bevelPaint
                                                )
                                        }
                                        "HEAVY_MECHANICAL" -> {
                                                val armorColor = when (uiState.armorType) {
                                                        ArmorType.GUNMETAL -> Color.parseColor("#2C2C2E")
                                                        ArmorType.CARBON_FIBER -> Color.parseColor("#1C1C1E")
                                                        ArmorType.WORN_OLIVE -> Color.parseColor("#4B5320")
                                                }
                                                val framePaint = Paint().apply {
                                                        color = armorColor
                                                        isAntiAlias = true
                                                }
                                                
                                                // 1. Chamfered Frame Path
                                                val chamfer = 60f * scale.toFloat()
                                                val framePath = Path().apply {
                                                        moveTo(chamfer, 0f)
                                                        lineTo(bitmap.width - chamfer, 0f)
                                                        lineTo(bitmap.width.toFloat(), chamfer)
                                                        lineTo(bitmap.width.toFloat(), bitmap.height - chamfer)
                                                        lineTo(bitmap.width - chamfer, bitmap.height.toFloat())
                                                        lineTo(chamfer, bitmap.height.toFloat())
                                                        lineTo(0f, bitmap.height - chamfer)
                                                        lineTo(0f, chamfer)
                                                        close()
                                                        // Hole for map
                                                        addRect(mapLeft, mapTop, mapLeft + mapWidth, mapTop + mapHeight, Path.Direction.CCW)
                                                        fillType = Path.FillType.EVEN_ODD
                                                }
                                                canvas.drawPath(framePath, framePaint)

                                                // 2. Armor Texture (Simulated)
                                                if (uiState.canvasGrain > 0f) {
                                                        val noisePaint = Paint().apply {
                                                                color = Color.BLACK
                                                                alpha = (25 * uiState.canvasGrain).toInt()
                                                                xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
                                                        }
                                                        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), noisePaint)
                                                }

                                                // 3. Rivets
                                                val rivetPaint = Paint().apply {
                                                        color = Color.BLACK
                                                        alpha = 100
                                                        isAntiAlias = true
                                                }
                                                val rivetHighlight = Paint().apply {
                                                        color = Color.WHITE
                                                        alpha = 50
                                                        isAntiAlias = true
                                                }
                                                val rRadius = (6f + (uiState.mechanicalSeams * 8f)) * scale.toFloat()
                                                val rSpacing = (150f + (1.0f - uiState.mechanicalSeams) * 250f) * scale.toFloat()

                                                fun drawRivet(x: Float, y: Float) {
                                                        canvas.drawCircle(x, y, rRadius, rivetPaint)
                                                        canvas.drawCircle(x - 2f * scale.toFloat(), y - 2f * scale.toFloat(), rRadius * 0.5f, rivetHighlight)
                                                }

                                                // Frame Rivets
                                                drawRivet(mapLeft / 2, mapTop / 2)
                                                drawRivet(bitmap.width - mapLeft / 2, mapTop / 2)
                                                drawRivet(mapLeft / 2, bitmap.height - (60f * scale.toFloat()))
                                                drawRivet(bitmap.width - mapLeft / 2, bitmap.height - (60f * scale.toFloat()))

                                                var rx = mapLeft + rSpacing
                                                while (rx < mapLeft + mapWidth - rSpacing) {
                                                        drawRivet(rx, mapTop / 2)
                                                        drawRivet(rx, bitmap.height - (60f * scale.toFloat()))
                                                        rx += rSpacing
                                                }

                                                // 4. Hazard Striping
                                                if (uiState.hasHazardStriping) {
                                                        val sWidth = 40f * scale.toFloat()
                                                        val sHeight = 70f * scale.toFloat()
                                                        val sSpacing = 80f * scale.toFloat()
                                                        val sPaint = Paint().apply {
                                                                color = Color.parseColor("#FBC02D")
                                                                alpha = 200
                                                                isAntiAlias = true
                                                        }
                                                        val sStartX = (mapLeft + mapWidth) - (300f * scale.toFloat())
                                                        val sY = mapTop + mapHeight + (40f * scale.toFloat())

                                                        for (i in 0 until 6) {
                                                                val x = sStartX + (i * sSpacing)
                                                                val sPath = Path().apply {
                                                                        moveTo(x, sY)
                                                                        lineTo(x + sWidth, sY)
                                                                        lineTo(x + sWidth - (20f * scale.toFloat()), sY + sHeight)
                                                                        lineTo(x - (20f * scale.toFloat()), sY + sHeight)
                                                                        close()
                                                                }
                                                                canvas.drawPath(sPath, sPaint)
                                                        }
                                                }

                                                // 5. Tactical HUD (Post-processing on map area)
                                                val hudColor = Color.parseColor(uiState.artColorStyle)
                                                val gridPaint = Paint().apply {
                                                        color = hudColor
                                                        alpha = 40
                                                        strokeWidth = 1f * scale.toFloat()
                                                }
                                                val gridCount = 10
                                                for (i in 1 until gridCount) {
                                                        val gx = mapLeft + (mapWidth / gridCount) * i
                                                        val gy = mapTop + (mapHeight / gridCount) * i
                                                        canvas.drawLine(gx, mapTop, gx, mapTop + mapHeight, gridPaint)
                                                        canvas.drawLine(mapLeft, gy, mapLeft + mapWidth, gy, gridPaint)
                                                }
                                                
                                                val retPaint = Paint().apply {
                                                        color = hudColor
                                                        alpha = 180
                                                        style = Paint.Style.STROKE
                                                        strokeWidth = 3f * scale.toFloat()
                                                }
                                                val rs = 60f * scale.toFloat()
                                                // Corner Reticles
                                                canvas.drawLines(floatArrayOf(
                                                        mapLeft, mapTop, mapLeft + rs, mapTop,
                                                        mapLeft, mapTop, mapLeft, mapTop + rs,
                                                        mapLeft + mapWidth, mapTop + mapHeight, mapLeft + mapWidth - rs, mapTop + mapHeight,
                                                        mapLeft + mapWidth, mapTop + mapHeight, mapLeft + mapWidth, mapTop + mapHeight - rs
                                                ), retPaint)
                                        }
                                        "CYBER_GLITCH" -> {
                                                val neonPaint =
                                                        Paint().apply {
                                                                color = Color.parseColor("#00F2FF")
                                                                alpha = 150
                                                                style = Paint.Style.STROKE
                                                                strokeWidth = 2f * scale.toFloat()
                                                        }
                                                canvas.drawRect(
                                                        mapLeft - (4f * scale.toFloat()),
                                                        mapTop - (4f * scale.toFloat()),
                                                        mapLeft + mapWidth + (4f * scale.toFloat()),
                                                        mapBottom + (4f * scale.toFloat()),
                                                        neonPaint
                                                )
                                        }
                                }

                                // Inner Border / Ambient Occlusion
                                val borderColor =
                                        when (uiState.polaroidFrameStyle) {
                                                "CLASSIC_BLACK", "CYBER_GLITCH" ->
                                                        Color.argb(51, 255, 255, 255)
                                                "LIQUID_GLASS" -> Color.argb(128, 255, 255, 255)
                                                "HEAVY_MECHANICAL" -> Color.parseColor("#B0BEC5")
                                                "ACOUSTIC_WOOD" -> Color.argb(102, 141, 110, 99)
                                                else -> Color.argb(38, 0, 0, 0)
                                        }
                                val borderPaint =
                                        Paint().apply {
                                                color = borderColor
                                                style = Paint.Style.STROKE
                                                strokeWidth =
                                                        uiState.polaroidInnerBorder *
                                                                scale.toFloat()
                                        }
                                canvas.drawRect(
                                        mapLeft,
                                        mapTop,
                                        mapLeft + mapWidth,
                                        mapTop + mapHeight,
                                        borderPaint
                                )

                                // High-res Ambient Occlusion (Inner Shadow)
                                val shadowPaint =
                                        Paint().apply {
                                                val colors =
                                                        intArrayOf(
                                                                Color.argb(40, 0, 0, 0),
                                                                Color.TRANSPARENT
                                                        )
                                                shader =
                                                        LinearGradient(
                                                                0f,
                                                                mapTop,
                                                                0f,
                                                                mapTop + (20f * scale.toFloat()),
                                                                colors,
                                                                null,
                                                                Shader.TileMode.CLAMP
                                                        )
                                        }
                                canvas.drawRect(
                                        mapLeft,
                                        mapTop,
                                        mapLeft + mapWidth,
                                        mapTop + (20f * scale.toFloat()),
                                        shadowPaint
                                )

                                // Global Color Tint
                                val tintColor =
                                        when (uiState.polaroidFrameStyle) {
                                                "ACOUSTIC_WOOD" -> Color.argb(13, 255, 152, 0)
                                                "CYBER_GLITCH" -> Color.argb(8, 0, 242, 255)
                                                "HEAVY_MECHANICAL" -> Color.argb(13, 144, 164, 174)
                                                else -> Color.TRANSPARENT
                                        }
                                if (tintColor != Color.TRANSPARENT) {
                                        canvas.drawColor(tintColor)
                                }

                                // 5. Canvas Grain Overlay (Noise on map)
                                if (uiState.canvasGrain > 0f) {
                                        val grainAlpha = (40 * uiState.canvasGrain).toInt()
                                        val noisePaint =
                                                Paint().apply {
                                                        color = Color.BLACK
                                                        alpha = grainAlpha
                                                        xfermode =
                                                                PorterDuffXfermode(
                                                                        PorterDuff.Mode.DARKEN
                                                                )
                                                }
                                        canvas.drawRect(
                                                mapLeft,
                                                mapTop,
                                                mapLeft + mapWidth,
                                                mapBottom,
                                                noisePaint
                                        )
                                }

                                val titleColor =
                                        when (uiState.polaroidFrameStyle) {
                                                "CLASSIC_BLACK", "HEAVY_MECHANICAL" -> Color.WHITE
                                                "CYBER_GLITCH" -> Color.parseColor("#00F2FF")
                                                "ACOUSTIC_WOOD" -> Color.parseColor("#3E2723")
                                                else -> Color.BLACK
                                        }
                                textPaint.color = titleColor
                                textPaint.textSize = 240f * scale.toFloat()
                                textPaint.letterSpacing = 0.02f

                                val subColor =
                                        when (uiState.polaroidFrameStyle) {
                                                "CLASSIC_BLACK",
                                                "HEAVY_MECHANICAL",
                                                "CYBER_GLITCH" -> Color.argb(180, 255, 255, 255)
                                                "ACOUSTIC_WOOD" -> Color.argb(180, 62, 39, 35)
                                                else -> Color.GRAY
                                        }
                                subtitlePaint.color = subColor
                                subtitlePaint.textSize = 48f * scale.toFloat()

                                val titleStrRaw = uiState.artAuthorName.ifBlank { "My Journey" }
                                val titleStr = if (uiState.polaroidFrameStyle == "HEAVY_MECHANICAL") "[ $titleStrRaw ]" else titleStrRaw
                                val textWidth = textPaint.measureText(titleStr)
                                
                                val exportDateRange = if (uiState.polaroidFrameStyle == "HEAVY_MECHANICAL") dateRangeStr.replace(".", "-") else dateRangeStr
                                val exportSubtitleStr = if (uiState.polaroidFrameStyle == "HEAVY_MECHANICAL") {
                                        "$exportDateRange · ${String.format("%.2f", totalDistanceKm)} KM"
                                } else subtitleStr
                                
                                val subWidth = subtitlePaint.measureText(exportSubtitleStr)

                                val x_title = (bitmap.width - textWidth) / 2f
                                val x_sub = (bitmap.width - subWidth) / 2f
                                val y_base = bitmap.height - (180f * scale.toFloat())

                                if (uiState.artTextBorder) {
                                        val borderPaint =
                                                Paint(textPaint).apply {
                                                        style = Paint.Style.STROKE
                                                        strokeWidth = 6f * scale.toFloat()
                                                        color = Color.argb(50, 0, 0, 0)
                                                }
                                        canvas.drawText(titleStr, x_title, y_base, borderPaint)
                                }

                                // Laser Engraving Effect for Acoustic Wood
                                if (uiState.polaroidFrameStyle == "ACOUSTIC_WOOD") {
                                        val engravingPaint =
                                                Paint(textPaint).apply {
                                                        alpha =
                                                                (255 *
                                                                                0.95f *
                                                                                uiState.engravingDepth)
                                                                        .toInt()
                                                        xfermode =
                                                                PorterDuffXfermode(
                                                                        PorterDuff.Mode.MULTIPLY
                                                                )
                                                }
                                        canvas.drawText(titleStr, x_title, y_base, engravingPaint)
                                } else {
                                        if (uiState.polaroidFrameStyle == "HEAVY_MECHANICAL") {
                                                val borderPaint = Paint().apply {
                                                        color = titleColor
                                                        alpha = 80
                                                        style = Paint.Style.STROKE
                                                        strokeWidth = 2f * scale.toFloat()
                                                        isAntiAlias = true
                                                }
                                                val rectPadding = 40f * scale.toFloat()
                                                canvas.drawRoundRect(
                                                        x_title - rectPadding,
                                                        y_base - textPaint.textSize * 0.8f,
                                                        x_title + textWidth + rectPadding,
                                                        y_base + 10f * scale.toFloat(),
                                                        40f * scale.toFloat(),
                                                        40f * scale.toFloat(),
                                                        borderPaint
                                                )
                                        }
                                        canvas.drawText(titleStr, x_title, y_base, textPaint)
                                }

                                // Laser Engraving Effect for subtitle
                                if (uiState.polaroidFrameStyle == "ACOUSTIC_WOOD") {
                                        val subEngravingPaint =
                                                Paint(subtitlePaint).apply {
                                                        alpha =
                                                                (180 * uiState.engravingDepth)
                                                                        .toInt()
                                                        xfermode =
                                                                PorterDuffXfermode(
                                                                        PorterDuff.Mode.MULTIPLY
                                                                )
                                                }
                                        canvas.drawText(
                                                exportSubtitleStr,
                                                x_sub,
                                                y_base + (100f * scale.toFloat()),
                                                subEngravingPaint
                                        )
                                } else {
                                        canvas.drawText(
                                                exportSubtitleStr,
                                                x_sub,
                                                y_base + (100f * scale.toFloat()),
                                                subtitlePaint
                                        )
                                }
                        }
                        ArtLayout.GEEK_STATS -> {
                                val density = context.resources.displayMetrics.density
                                val margin = 16f * density * scale.toFloat()
                                val padding = 16f * density * scale.toFloat()

                                textPaint.textSize = 32f * scale.toFloat()
                                subtitlePaint.textSize = 32f * scale.toFloat()

                                val titleStr =
                                        uiState.artAuthorName.uppercase().ifBlank {
                                                "DATA VISUALIZATION"
                                        }
                                val modeStr = "MODE: TRACKING"

                                val titleWidth = textPaint.measureText(titleStr)
                                val subWidth = subtitlePaint.measureText(subtitleStr)
                                val modeWidth = subtitlePaint.measureText(modeStr)

                                val boxWidth = maxOf(titleWidth, subWidth, modeWidth) + padding * 2
                                val boxHeight =
                                        textPaint.textSize +
                                                subtitlePaint.textSize * 2 +
                                                padding * 2 +
                                                (8f * scale.toFloat()) * 2

                                val boxLeft = bitmap.width.toFloat() - margin - boxWidth
                                val boxTop = margin

                                val boxPaint = Paint().apply { color = Color.argb(178, 0, 0, 0) }
                                val cornerRadius = 8f * density * scale.toFloat()
                                canvas.drawRoundRect(
                                        boxLeft,
                                        boxTop,
                                        boxLeft + boxWidth,
                                        boxTop + boxHeight,
                                        cornerRadius,
                                        cornerRadius,
                                        boxPaint
                                )

                                val tx = boxLeft + padding
                                var ty = boxTop + padding + textPaint.textSize * 0.8f

                                if (uiState.artTextBorder) {
                                        val borderPaint =
                                                Paint(textPaint).apply {
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
                                val bracketPaint =
                                        Paint().apply {
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
                                canvas.drawLine(
                                        bitmap.width.toFloat(),
                                        bitmap.height.toFloat(),
                                        bitmap.width.toFloat() - bLen,
                                        bitmap.height.toFloat(),
                                        bracketPaint
                                )
                                canvas.drawLine(
                                        bitmap.width.toFloat(),
                                        bitmap.height.toFloat(),
                                        bitmap.width.toFloat(),
                                        bitmap.height.toFloat() - bLen,
                                        bracketPaint
                                )
                        }
                }
        }

        /**
         * Efficient blur for high-res bitmaps using downscaling and upscale. This simulates a
         * Gaussian blur while being much faster for large images.
         */
        private fun blurBitmap(src: Bitmap, radius: Float): Bitmap? {
                if (radius <= 0) return src.copy(src.config, true)

                // Downscale for performance
                val scaleFactor = 0.25f
                val width = Math.round(src.width * scaleFactor)
                val height = Math.round(src.height * scaleFactor)

                if (width <= 0 || height <= 0) return null

                val overlay = Bitmap.createScaledBitmap(src, width, height, true)

                // Apply a simple blur if needed, or just let scaling do the work
                // For a better effect, we'll draw it back with some alpha/offset or just use
                // multiple scales
                val result = Bitmap.createScaledBitmap(overlay, src.width, src.height, true)
                overlay.recycle()

                return result
        }
}
