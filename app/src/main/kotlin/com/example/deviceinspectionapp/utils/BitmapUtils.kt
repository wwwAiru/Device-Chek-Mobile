package com.example.deviceinspectionapp

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.io.InputStream

object BitmapUtils {


    /**
     * Получает размеры изображения по URI.
     *
     * @param uri URI изображения.
     * @param context Контекст для получения ContentResolver.
     * @return Пара ширины и высоты изображения, или null в случае ошибки.
     */
    fun getImageDimensions(uri: Uri, context: Context): Pair<Int, Int>? {
        return try {
            context.contentResolver.openInputStream(uri).use { inputStream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(inputStream, null, options)
                options.outWidth to options.outHeight
            }
        } catch (e: Exception) {
            Log.e("BitmapUtils", "Ошибка при получении размеров изображения: ${e.message}")
            null
        }
    }


    /**
     * Поворачивает изображение на указанный угол.
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Поворачивает изображение в зависимости от EXIF-ориентации.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun rotateImageIfRequired(
        bitmap: Bitmap,
        photoUri: Uri,
        contentResolver: ContentResolver
    ): Bitmap {
        var inputStream: InputStream? = null
        return try {
            inputStream = contentResolver.openInputStream(photoUri)
            val exif = inputStream?.let { ExifInterface(it) }
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap // Если не удается прочитать EXIF, возвращаем оригинал
        } finally {
            inputStream?.close()
        }
    }

    /**
     * Создает миниатюру из файла изображения.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun createThumbnailFromFile(
        photoFile: File,
        contentResolver: ContentResolver,
        thumbWidth: Int = 100,
        thumbHeight: Int = 100
    ): Bitmap {
        // Декодируем исходное изображение
        val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        val photoUri = Uri.fromFile(photoFile)

        // Поворачиваем изображение, если требуется
        val rotatedBitmap = rotateImageIfRequired(originalBitmap, photoUri, contentResolver)

        // Уменьшаем изображение до указанных размеров
        return getResizedBitmap(rotatedBitmap, thumbWidth, thumbHeight)
    }

    /**
     * Изменяет размер изображения до указанных ширины и высоты.
     */
    private fun getResizedBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
