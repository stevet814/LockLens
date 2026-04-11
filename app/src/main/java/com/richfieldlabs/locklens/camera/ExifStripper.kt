package com.richfieldlabs.locklens.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.lang.reflect.Modifier
import java.util.Locale

object ExifStripper {
    private val allExifTags = ExifInterface::class.java.fields
        .asSequence()
        .filter { field ->
            Modifier.isStatic(field.modifiers) &&
                field.type == String::class.java &&
                field.name.startsWith("TAG_")
        }
        .mapNotNull { field -> field.get(null) as? String }
        .toSet()

    /**
     * Rewrites the image so the vault copy keeps no removable metadata.
     * Returns the MIME type of the sanitized file, which may differ for some imported formats.
     */
    fun strip(file: File, mimeType: String? = null): String {
        val normalizedMimeType = mimeType?.substringBefore(';')?.trim()?.lowercase(Locale.US)
        val orientation = readOrientation(file)

        if (isJpeg(file, normalizedMimeType) && orientation.isLosslessSafe()) {
            JpegMetadataStripper.strip(file)
            return "image/jpeg"
        }

        return rewriteWithoutMetadata(file, normalizedMimeType, orientation)
    }

    private fun rewriteWithoutMetadata(file: File, mimeType: String?, orientation: Int): String {
        val source = BitmapFactory.decodeFile(file.absolutePath)
            ?: return fallbackStrip(file, mimeType)
        val normalized = applyOrientation(source, orientation)
        val output = chooseOutputFormat(mimeType, normalized.hasAlpha())
        val tempFile = File(file.parentFile, "${file.name}.clean")

        try {
            tempFile.outputStream().use { stream ->
                check(normalized.compress(output.compressFormat, output.quality, stream)) {
                    "Unable to write sanitized image."
                }
            }
            replaceFile(tempFile, file)
            return output.mimeType
        } finally {
            tempFile.delete()
            if (normalized !== source) normalized.recycle()
            source.recycle()
        }
    }

    private fun fallbackStrip(file: File, mimeType: String?): String {
        stripAllExifTags(file)
        if (isJpeg(file, mimeType)) {
            JpegMetadataStripper.strip(file)
            return "image/jpeg"
        }
        return mimeType ?: inferMimeType(file)
    }

    private fun stripAllExifTags(file: File) {
        runCatching {
            val exif = ExifInterface(file.absolutePath)
            allExifTags.forEach { tag -> exif.setAttribute(tag, null) }
            exif.saveAttributes()
        }
    }

    private fun readOrientation(file: File): Int = runCatching {
        ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED,
        )
    }.getOrDefault(ExifInterface.ORIENTATION_UNDEFINED)

    private fun isJpeg(file: File, mimeType: String?): Boolean {
        if (mimeType == "image/jpeg" || mimeType == "image/jpg") return true
        if (!file.exists() || file.length() < 2) return false
        file.inputStream().use { input ->
            val first = input.read()
            val second = input.read()
            return first == 0xFF && second == 0xD8
        }
    }

    private fun inferMimeType(file: File): String = when (file.extension.lowercase(Locale.US)) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        else -> "image/jpeg"
    }

    private fun chooseOutputFormat(mimeType: String?, hasAlpha: Boolean): OutputFormat = when {
        mimeType == "image/png" || hasAlpha -> OutputFormat(Bitmap.CompressFormat.PNG, 100, "image/png")
        else -> OutputFormat(Bitmap.CompressFormat.JPEG, 100, "image/jpeg")
    }

    private fun applyOrientation(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return source
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun Int.isLosslessSafe(): Boolean =
        this == ExifInterface.ORIENTATION_UNDEFINED || this == ExifInterface.ORIENTATION_NORMAL

    private fun replaceFile(source: File, target: File) {
        if (target.exists() && !target.delete()) {
            error("Unable to replace ${target.absolutePath}")
        }
        if (!source.renameTo(target)) {
            source.copyTo(target, overwrite = true)
        }
    }

    private data class OutputFormat(
        val compressFormat: Bitmap.CompressFormat,
        val quality: Int,
        val mimeType: String,
    )
}
