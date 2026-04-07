package com.richfieldlabs.locklens.camera

import androidx.exifinterface.media.ExifInterface
import java.io.File

object ExifStripper {
    private val tagsToStrip = listOf(
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_LENS_SERIAL_NUMBER,
        ExifInterface.TAG_LENS_MAKE,
        ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_BODY_SERIAL_NUMBER,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_IMAGE_DESCRIPTION,
        ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_CAMERA_OWNER_NAME,
    )

    fun strip(file: File) {
        val exif = ExifInterface(file.absolutePath)
        tagsToStrip.forEach { tag ->
            exif.setAttribute(tag, null)
        }
        exif.saveAttributes()
    }

    fun hasGpsData(file: File): Boolean {
        val exif = ExifInterface(file.absolutePath)
        return exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null ||
            exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) != null
    }
}
