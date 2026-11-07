package com.example.simplecamera.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility functions for Camera capture, FileProvider URI creation,
 * EXIF rotation handling, and Android System Sharing interface invocation.
 */
object CameraUtils {

    private const val FILE_PROVIDER_SUFFIX = ".fileprovider"
    private const val PHOTO_DIR_NAME = "camera_photos"

    /**
     * Creates a new temporary file in the app cache and returns its content:// Uri
     * generated via FileProvider.
     *
     * Concept: FileProvider generates secure content:// URIs required by modern Android (API 24+)
     * instead of raw file:// URIs, preventing FileUriExposedException.
     */
    fun createImageFileUri(context: Context): Uri {
        val photosDir = File(context.cacheDir, PHOTO_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val photoFile = File(photosDir, "IMG_${timeStamp}.jpg")

        val authority = "${context.packageName}$FILE_PROVIDER_SUFFIX"
        return FileProvider.getUriForFile(context, authority, photoFile)
    }

    /**
     * Safely decodes a Bitmap from a content URI, correcting orientation via EXIF metadata
     * and downsampling large images to prevent OutOfMemory errors.
     * Android camera hardware frequently captures in landscape sensor orientation and embeds
     * rotation flags (e.g. 90 or 270 degrees). This function ensures the displayed photo
     * matches the true orientation in Compose UI.
     */
    fun loadRotatedBitmap(context: Context, photoUri: Uri, maxDimension: Int = 2048): Bitmap? {
        return try {
            val resolver = context.contentResolver

            // 1. Calculate sample size to avoid OutOfMemory errors on high-resolution cameras
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            resolver.openInputStream(photoUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            // 2. Decode bitmap with inSampleSize
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val originalBitmap = resolver.openInputStream(photoUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // 3. Read EXIF orientation
            val orientation = resolver.openInputStream(photoUri)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

            val rotationAngle = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            // 4. Rotate if necessary
            if (rotationAngle != 0f) {
                val matrix = Matrix().apply { postRotate(rotationAngle) }
                Bitmap.createBitmap(
                    originalBitmap,
                    0,
                    0,
                    originalBitmap.width,
                    originalBitmap.height,
                    matrix,
                    true
                )
            } else {
                originalBitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Opens the Android System Sharing interface using an ACTION_SEND Intent.
     *
     * Concepts Demonstrated:
     * 1. Implicit Intent: Intent.ACTION_SEND
     * 2. MIME Type: "image/jpeg"
     * 3. Intent Extras: Intent.EXTRA_STREAM with content:// Uri
     * 4. Temporary Security Grants: FLAG_GRANT_READ_URI_PERMISSION & ClipData
     * 5. System Chooser: Intent.createChooser() allows selecting any available app
     *    (WhatsApp, Bluetooth, Nearby Share / Quick Share, Telegram, Gmail, etc.)
     */
    fun sharePhoto(context: Context, photoUri: Uri) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, photoUri)
                putExtra(Intent.EXTRA_SUBJECT, "Photo from Simple Camera App")
                putExtra(Intent.EXTRA_TEXT, "Captured using Simple Camera Application 📸")
                clipData = android.content.ClipData.newRawUri("photo", photoUri)
                // Grants target apps (Bluetooth, WhatsApp, Gmail, etc.) read access to the FileProvider URI
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share Photo via")
            // Ensure chooser runs cleanly across all activity contexts
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Unable to share photo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
