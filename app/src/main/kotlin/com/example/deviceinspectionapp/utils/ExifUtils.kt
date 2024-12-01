package com.example.deviceinspectionapp.utils

import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object ExifUtils {

    // Считывание EXIF-данных из файла
    fun readExifData(file: File): Map<String, String?> {
        val exifInterface = ExifInterface(file)
        return mapOf(
            ExifInterface.TAG_DATETIME to exifInterface.getAttribute(ExifInterface.TAG_DATETIME),
            ExifInterface.TAG_DATETIME_ORIGINAL to exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL),
            ExifInterface.TAG_DATETIME_DIGITIZED to exifInterface.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED),
            ExifInterface.TAG_ORIENTATION to exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION),
            ExifInterface.TAG_MAKE to exifInterface.getAttribute(ExifInterface.TAG_MAKE),
            ExifInterface.TAG_MODEL to exifInterface.getAttribute(ExifInterface.TAG_MODEL),
            ExifInterface.TAG_FLASH to exifInterface.getAttribute(ExifInterface.TAG_FLASH),
            ExifInterface.TAG_WHITE_BALANCE to exifInterface.getAttribute(ExifInterface.TAG_WHITE_BALANCE),
            ExifInterface.TAG_EXPOSURE_TIME to exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME),
            ExifInterface.TAG_GPS_LATITUDE to exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE),
            ExifInterface.TAG_GPS_LONGITUDE to exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE),
            ExifInterface.TAG_GPS_LATITUDE_REF to exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF),
            ExifInterface.TAG_GPS_LONGITUDE_REF to exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF),
            ExifInterface.TAG_GPS_ALTITUDE to exifInterface.getAttribute(ExifInterface.TAG_GPS_ALTITUDE),
            ExifInterface.TAG_GPS_ALTITUDE_REF to exifInterface.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF)
        )
    }

    // Логирование EXIF-данных
    fun logExifData(tag: String, exifData: Map<String, String?>) {
        exifData.forEach { (key, value) ->
            Log.d(tag, "$key: $value")
        }
    }

    fun updateExifTimestamp(file: File) {
        val exifInterface = ExifInterface(file)

        // Обновление текущей временной метки
        val currentTimestamp = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).format(Date())
        exifInterface.setAttribute(ExifInterface.TAG_DATETIME, currentTimestamp)
        exifInterface.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, currentTimestamp)
        exifInterface.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, currentTimestamp)

        exifInterface.saveAttributes()
    }

}
