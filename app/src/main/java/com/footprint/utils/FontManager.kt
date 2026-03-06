package com.footprint.utils

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import java.io.File
import java.net.URL
import kotlin.concurrent.thread

object FontManager {
    private const val BASE_URL = "https://gitee.com/Ace314/src/raw/master/font"
    private val fontCache = mutableMapOf<String, FontFamily>()
    private val downloadingFonts = mutableSetOf<String>()

    private val fontMap =
            mapOf(
                    "MaShanZheng" to "ma_shan_zheng.ttf",
                    "ZhiMangXing" to "zhi_mang_xing.ttf",
                    "LongCang" to "long_cang.ttf",
                    "LiuJianMaoCao" to "liu_jian_mao_cao.ttf",
                    "ZCOOLXiaoWei" to "zcool_xiao_wei.ttf"
            )

    fun getFontFamily(context: Context, fontName: String): FontFamily {
        if (fontName == "Default") return FontFamily.Default

        fontCache[fontName]?.let {
            return it
        }

        val fileName = fontMap[fontName] ?: return FontFamily.Default
        val fontFile = File(context.filesDir, "fonts/$fileName")

        return if (fontFile.exists()) {
            val family =
                    FontFamily(
                            androidx.compose.ui.text.font.Font(
                                    file = fontFile,
                                    weight = FontWeight.Normal,
                                    style = FontStyle.Normal
                            )
                    )
            fontCache[fontName] = family
            family
        } else {
            downloadFont(context, fontName)
            FontFamily.Default
        }
    }

    fun isFontLoaded(context: Context, fontName: String): Boolean {
        val fileName = fontMap[fontName] ?: return true
        return File(context.filesDir, "fonts/$fileName").exists()
    }

    private fun downloadFont(context: Context, fontName: String) {
        if (downloadingFonts.contains(fontName)) return
        val fileName = fontMap[fontName] ?: return

        downloadingFonts.add(fontName)
        thread {
            try {
                val url = URL("$BASE_URL/$fileName")
                val fontsDir = File(context.filesDir, "fonts")
                if (!fontsDir.exists()) fontsDir.mkdirs()

                val tempFile = File(fontsDir, "$fileName.tmp")
                url.openStream().use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }

                val finalFile = File(fontsDir, fileName)
                tempFile.renameTo(finalFile)
                downloadingFonts.remove(fontName)
            } catch (e: Exception) {
                e.printStackTrace()
                downloadingFonts.remove(fontName)
            }
        }
    }
}
