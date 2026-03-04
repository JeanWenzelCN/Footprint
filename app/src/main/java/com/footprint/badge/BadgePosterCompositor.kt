package com.footprint.badge

import android.content.Context
import android.content.Intent
import android.graphics.*
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * 勋章海报合成器 — 将 Flutter 端抓拍的 3D 勋章 PNG 烘焙到拍立得海报上。
 *
 * 合成管线:
 *   1. 创建画布 (1080 × 1440 px)
 *   2. 绘制拍立得底板（圆角白色 + 内部暗色地图区域）
 *   3. 接触阴影 (Contact Shadow)  → 窄模糊、高透明
 *   4. 全局投影 (Drop Shadow)     → 宽模糊、低透明
 *   5. 材质特效 (Bloom / Distortion)
 *   6. 破框合成勋章 PNG
 *   7. 底部文字排版
 */
object BadgePosterCompositor {

    private const val POSTER_W = 1080
    private const val POSTER_H = 1440
    private const val FRAME_PAD = 48       // 拍立得白边
    private const val BOTTOM_PAD = 220     // 拍立得底部留白
    private const val CORNER_R = 24f
    private const val BADGE_SIZE = 320     // 勋章绘制尺寸
    private const val SHADOW_OFFSET = 12f  // 投影偏移

    /**
     * @return 合成后海报的绝对路径，失败返回 null
     */
    fun compose(
        context: Context,
        badgePngBytes: ByteArray,
        badgeTitle: String,
        badgeColorHex: String,
        materialType: String,
    ): String? {
        return try {
            val poster = Bitmap.createBitmap(POSTER_W, POSTER_H, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(poster)

            // ── 1. 纯黑背景 ──
            canvas.drawColor(Color.BLACK)

            // ── 2. 拍立得底板 ──
            drawPolaroidFrame(canvas)

            // ── 3. 解码勋章 PNG ──
            val badgeBitmap = BitmapFactory.decodeByteArray(badgePngBytes, 0, badgePngBytes.size)
                ?: return null
            val scaledBadge = Bitmap.createScaledBitmap(badgeBitmap, BADGE_SIZE, BADGE_SIZE, true)

            // 勋章右下角破框位置
            val badgeLeft = POSTER_W - FRAME_PAD - BADGE_SIZE + 40   // 右侧压边 40px
            val badgeTop  = POSTER_H - BOTTOM_PAD - BADGE_SIZE / 2   // 跨越底部留白线

            // ── 4. 接触阴影 (Contact Shadow) ──
            drawContactShadow(canvas, badgeLeft, badgeTop, scaledBadge)

            // ── 5. 全局投影 (Drop Shadow) ──
            drawDropShadow(canvas, badgeLeft, badgeTop, scaledBadge)

            // ── 6. 材质特效 ──
            val accentColor = parseColorSafe(badgeColorHex)
            when (materialType) {
                "Cyber" -> drawCyberBloom(canvas, badgeLeft, badgeTop, accentColor)
                "Liquid" -> drawGlassDistortion(canvas, poster, badgeLeft, badgeTop)
            }

            // ── 7. 绘制勋章本体 ──
            canvas.drawBitmap(scaledBadge, badgeLeft.toFloat(), badgeTop.toFloat(), null)

            // ── 8. 底部文字排版 ──
            drawBottomText(canvas, badgeTitle, accentColor)

            // ── 9. 输出到缓存 ──
            val outFile = File(context.cacheDir, "badge_poster_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { fos ->
                poster.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }

            // 回收
            badgeBitmap.recycle()
            scaledBadge.recycle()
            poster.recycle()

            outFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 分享海报到外部应用
     */
    fun share(context: Context, posterPath: String) {
        val file = File(posterPath)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享勋章海报"))
    }

    // ─────────────────────────── 内部绘制方法 ───────────────────────────

    private fun drawPolaroidFrame(canvas: Canvas) {
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val frameRect = RectF(
            FRAME_PAD.toFloat(), FRAME_PAD.toFloat(),
            (POSTER_W - FRAME_PAD).toFloat(),
            (POSTER_H - FRAME_PAD).toFloat()
        )
        canvas.drawRoundRect(frameRect, CORNER_R, CORNER_R, framePaint)

        // 内部暗色"地图区域"
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(25, 25, 30)
            style = Paint.Style.FILL
        }
        val innerRect = RectF(
            (FRAME_PAD + 16).toFloat(), (FRAME_PAD + 16).toFloat(),
            (POSTER_W - FRAME_PAD - 16).toFloat(),
            (POSTER_H - FRAME_PAD - BOTTOM_PAD).toFloat()
        )
        canvas.drawRoundRect(innerRect, CORNER_R / 2, CORNER_R / 2, innerPaint)
    }

    private fun drawContactShadow(canvas: Canvas, x: Int, y: Int, badge: Bitmap) {
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
            alpha = 200  // 80% 不透明
            maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawBitmap(badge, x.toFloat(), (y + 6).toFloat(), shadowPaint)
    }

    private fun drawDropShadow(canvas: Canvas, x: Int, y: Int, badge: Bitmap) {
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
            alpha = 76   // 30%
            maskFilter = BlurMaskFilter(24f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawBitmap(
            badge,
            x + SHADOW_OFFSET,
            y + SHADOW_OFFSET,
            shadowPaint
        )
    }

    private fun drawCyberBloom(canvas: Canvas, x: Int, y: Int, accentColor: Int) {
        val bloomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                (x + BADGE_SIZE / 2).toFloat(),
                (y + BADGE_SIZE / 2).toFloat(),
                BADGE_SIZE * 0.8f,
                Color.argb(60, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        }
        canvas.drawCircle(
            (x + BADGE_SIZE / 2).toFloat(),
            (y + BADGE_SIZE / 2).toFloat(),
            BADGE_SIZE * 0.8f,
            bloomPaint
        )
    }

    private fun drawGlassDistortion(canvas: Canvas, poster: Bitmap, x: Int, y: Int) {
        // 提取勋章覆盖区域的底层像素，轻微模糊以模拟折射
        val safeX = x.coerceIn(0, poster.width - 1)
        val safeY = y.coerceIn(0, poster.height - 1)
        val cropW = BADGE_SIZE.coerceAtMost(poster.width - safeX)
        val cropH = BADGE_SIZE.coerceAtMost(poster.height - safeY)
        if (cropW <= 0 || cropH <= 0) return

        val region = Bitmap.createBitmap(poster, safeX, safeY, cropW, cropH)
        val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = 120
            maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
        }
        // 略微放大再绘制回去，模拟凸透镜畸变
        val scaleMatrix = Matrix().apply {
            val scale = 1.08f
            val dx = -(cropW * (scale - 1f)) / 2f
            val dy = -(cropH * (scale - 1f)) / 2f
            setScale(scale, scale)
            postTranslate(safeX + dx, safeY + dy)
        }
        canvas.drawBitmap(region, scaleMatrix, blurPaint)
        region.recycle()
    }

    private fun drawBottomText(canvas: Canvas, title: String, accentColor: Int) {
        val textY = (POSTER_H - FRAME_PAD - BOTTOM_PAD + 60).toFloat()

        // 成就标题
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(40, 40, 40)
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(title, (FRAME_PAD + 32).toFloat(), textY, titlePaint)

        // 副标题
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(130, 130, 130)
            textSize = 28f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        canvas.drawText("FOOTPRINT · ACHIEVEMENT", (FRAME_PAD + 32).toFloat(), textY + 50, subPaint)

        // 装饰色带
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
        canvas.drawRoundRect(
            RectF(
                (FRAME_PAD + 32).toFloat(), textY + 70,
                (FRAME_PAD + 132).toFloat(), textY + 76
            ),
            3f, 3f, barPaint
        )
    }

    private fun parseColorSafe(hex: String?): Int {
        return try {
            val clean = hex?.removePrefix("#") ?: "FFFFFF"
            Color.parseColor("#$clean")
        } catch (_: Exception) {
            Color.WHITE
        }
    }
}
