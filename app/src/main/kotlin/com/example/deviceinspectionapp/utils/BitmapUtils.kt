package com.example.deviceinspectionapp

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object BitmapUtils {

    fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
        } catch (e: IOException) {
            Log.e("BitmapUtils", "Ошибка сохранения фото: ${e.message}", e)
        }
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun rotateImageIfRequired(bitmap: Bitmap, photoUri: Uri, contentResolver: ContentResolver): Bitmap {
        val inputStream = contentResolver.openInputStream(photoUri)
        val exif = inputStream?.let { ExifInterface(it) }
        val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            else -> bitmap
        }
    }

    fun saveOriginalPhoto(photoBitmap: Bitmap, file: File) {
        saveBitmapToFile(photoBitmap, file)
    }

    fun saveThumbnail(photoBitmap: Bitmap, thumbnailFile: File): Bitmap {
        val thumbnail = Bitmap.createScaledBitmap(photoBitmap, 150, 150, true)
        saveBitmapToFile(thumbnail, thumbnailFile)
        return thumbnail
    }
}
