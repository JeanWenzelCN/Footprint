package com.footprint.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    @RequiresApi(Build.VERSION_CODES.O)
    fun captureWindow(activity: Activity, onBitmapCaptured: (Bitmap) -> Unit) {
        val window = activity.window
        val bitmap = Bitmap.createBitmap(
            window.decorView.width,
            window.decorView.height,
            Bitmap.Config.ARGB_8888
        )
        
        val location = IntArray(2)
        window.decorView.getLocationInWindow(location)
        
        try {
            PixelCopy.request(
                window,
                android.graphics.Rect(
                    location[0],
                    location[1],
                    location[0] + window.decorView.width,
                    location[1] + window.decorView.height
                ),
                bitmap,
                { result ->
                    if (result == PixelCopy.SUCCESS) {
                        onBitmapCaptured(bitmap)
                    } else {
                        // Handle error?
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap): String? {
        val filename = "Footprint_Art_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.png"
        val directory = java.io.File(
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES),
            "FootprintArt"
        )
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, filename)
        
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            // Notify gallery scanner
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.toString()),
                null,
                null
            )
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
