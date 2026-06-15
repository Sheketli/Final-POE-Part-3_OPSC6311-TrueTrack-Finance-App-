package com.example.truetrackfinance.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "ImageUtil"
private const val MAX_SIZE_BYTES = 1_048_576L  // 1 MB

/**
 * Handles compressing and saving receipt images to private internal storage.
 * Images are stored in [Context.filesDir]/receipts/<timestamp>.jpg.
 */
object ImageUtil {

    /** Create a timestamped receipt File in private storage (does not write yet). */
    fun createReceiptFile(context: Context): File {
        val dir = File(context.filesDir, "receipts").also { it.mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "RECEIPT_$timestamp.jpg")
    }

    /**
     * Compress and save a URI-sourced image (gallery pick) to private storage.
     * Images are progressively compressed until they fit under [MAX_SIZE_BYTES].
     *
     * @return The absolute path of the saved file, or null on failure.
     */
    fun saveCompressedImage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            writeBitmapToStorage(context, bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save image from URI", e)
            null
        }
    }

    /**
     * Save a Bitmap captured via CameraX to private storage.
     * @return The absolute path of the saved file, or null on failure.
     */
    fun saveCameraBitmap(context: Context, bitmap: Bitmap): String? {
        return try {
            writeBitmapToStorage(context, bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save camera bitmap", e)
            null
        }
    }

    private fun writeBitmapToStorage(context: Context, bitmap: Bitmap): String {
        val file = createReceiptFile(context)
        var quality = 90
        do {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            quality -= 10
        } while (file.length() > MAX_SIZE_BYTES && quality > 10)
        Log.d(TAG, "Saved receipt image: ${file.absolutePath} (${file.length()} bytes, quality $quality)")
        return file.absolutePath
    }

    /** Delete a receipt image file from private storage. */
    fun deleteReceiptImage(path: String) {
        val file = File(path)
        if (file.exists()) {
            val deleted = file.delete()
            Log.d(TAG, "Deleted receipt: $path — success: $deleted")
        }
    }
}
