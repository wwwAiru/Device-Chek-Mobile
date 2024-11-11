package com.example.deviceinspectionapp

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object FsUtils {

    // Получить URI для файла с использованием FileProvider
    fun getFileUri(context: Context, file: File): Uri? {
        return if (file.exists()) {
            FileProvider.getUriForFile(context, context.packageName, file)
        } else {
            null
        }
    }

    // Сохранить фотографию в внутреннюю директорию (images)
    fun savePhotoToInternalStorage(context: Context, bitmap: Bitmap, photoFileName: String): File {
        val photoDirectory = File(context.filesDir, "images")
        if (!photoDirectory.exists()) {
            if (!photoDirectory.mkdirs()) {
                throw IOException("Не удалось создать директорию для фотографий")
            }
        }

        val photoFile = File(photoDirectory, photoFileName)
        try {
            FileOutputStream(photoFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
        } catch (e: IOException) {
            throw IOException("Ошибка сохранения фото: ${e.message}", e)
        }

        return photoFile
    }

    // Загрузить фото из внутренней директории
    fun loadPhotoFromInternalStorage(context: Context, photoFileName: String): File? {
        val photoDirectory = File(context.filesDir, "images")
        val photoFile = File(photoDirectory, photoFileName)
        return if (photoFile.exists()) photoFile else null
    }

    // Сохранить JSON в файл
    fun saveJsonToFile(context: Context, json: String, fileName: String) {
        val file = File(context.filesDir, fileName)
        try {
            FileOutputStream(file).use { outputStream ->
                outputStream.write(json.toByteArray())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // Загрузить JSON из файла
    fun loadJsonFromFile(context: Context, fileName: String): String? {
        val file = File(context.filesDir, fileName)
        return try {
            FileInputStream(file).use { inputStream ->
                inputStream.bufferedReader().use { reader ->
                    reader.readText()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // Проверка наличия файла
    fun isFileExists(context: Context, fileName: String): Boolean {
        val file = File(context.filesDir, fileName)
        return file.exists()
    }
}
