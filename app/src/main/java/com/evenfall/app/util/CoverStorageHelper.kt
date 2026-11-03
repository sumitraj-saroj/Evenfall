package com.evenfall.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object CoverStorageHelper {
    private const val MAX_DIMENSION = 1024
    private const val JPEG_QUALITY = 85

    fun saveAndDownsampleCover(context: Context, sourceUri: Uri, bookId: Long): String? {
        return try {
            val contentResolver = context.contentResolver

            // 1. Decode bounds
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: return null

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            if (originalWidth <= 0 || originalHeight <= 0) return null

            // 2. Compute inSampleSize
            var sampleSize = 1
            val maxSide = max(originalWidth, originalHeight)
            while (maxSide / (sampleSize * 2) >= MAX_DIMENSION) {
                sampleSize *= 2
            }

            // 3. Decode scaled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Memory-safe
            }
            val bitmap = contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return null

            // 4. Scale to exact max dimension if still larger
            val finalBitmap = if (bitmap.width > MAX_DIMENSION || bitmap.height > MAX_DIMENSION) {
                val ratio = MAX_DIMENSION.toFloat() / max(bitmap.width, bitmap.height)
                val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                if (scaled != bitmap) {
                    bitmap.recycle()
                }
                scaled
            } else {
                bitmap
            }

            // 5. Save to app-private storage
            val coversDir = File(context.filesDir, "custom_covers").apply { mkdirs() }
            val destFile = File(coversDir, "cover_${bookId}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(destFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            finalBitmap.recycle()

            Uri.fromFile(destFile).toString()
        } catch (_: Exception) {
            null
        }
    }
}
