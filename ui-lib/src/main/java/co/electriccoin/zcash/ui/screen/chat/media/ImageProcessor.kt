package co.electriccoin.zcash.ui.screen.chat.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageProcessor {
    private const val THUMBNAIL_MAX_SIZE = 400
    private const val IMAGE_QUALITY = 85
    private const val MAX_IMAGE_SIZE = 1920

    fun compressImage(context: Context, uri: Uri, maxSize: Int = MAX_IMAGE_SIZE): File? =
        try {
            val bitmap =
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                } ?: return null

            val scaledBitmap = scaleBitmap(bitmap, maxSize)
            val outputFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outputFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, out)
            }
            if (scaledBitmap !== bitmap) scaledBitmap.recycle()
            bitmap.recycle()
            outputFile
        } catch (e: Exception) {
            null
        }

    fun generateThumbnail(context: Context, uri: Uri): String? =
        try {
            val bitmap =
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                } ?: return null

            val thumbnail = scaleBitmap(bitmap, THUMBNAIL_MAX_SIZE)
            val base64 = bitmapToBase64(thumbnail)
            if (thumbnail !== bitmap) thumbnail.recycle()
            bitmap.recycle()
            base64
        } catch (e: Exception) {
            null
        }

    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val (newWidth, newHeight) =
            if (width > height) {
                maxSize to (maxSize / ratio).toInt()
            } else {
                (maxSize * ratio).toInt() to maxSize
            }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }
}
