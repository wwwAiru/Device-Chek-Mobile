package com.example.deviceinspectionapp

import PoverkaDTO
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DeviceCheckActivity : AppCompatActivity() {
    private lateinit var stageAdapter: StageManager
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var uuid: String
    private var currentStageCodeName: String? = null
    private var currentPhotoCodeName: String? = null
    private var originalPhotoUri: Uri? = null
    private var originalPhotoFile: File? = null

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_check)

        val jsonData = intent.getStringExtra("jsonData")
        val poverkaData = Json.decodeFromString<PoverkaDTO>(jsonData!!)
        uuid = poverkaData.uuid

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewStages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        stageAdapter = StageManager(this, poverkaData.stages) { stageCodeName, photoCodeName ->
            currentStageCodeName = stageCodeName
            currentPhotoCodeName = photoCodeName
            dispatchTakePictureIntent()
        }
        recyclerView.adapter = stageAdapter

        // Инициализация launcher для камеры
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && originalPhotoUri != null) {
                val photoBitmap = BitmapFactory.decodeFile(originalPhotoFile?.absolutePath)
                if (photoBitmap != null) {
                    val rotatedBitmap = rotateImageIfRequired(photoBitmap, originalPhotoUri!!)
                    savePhotoToStorage(rotatedBitmap)
                } else {
                    Toast.makeText(this, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Запрос разрешения на камеру
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(this, "Разрешение на использование камеры не предоставлено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Поиск доступного приложения камеры
        val cameraAppPackageName = packageManager.queryIntentActivities(takePictureIntent, PackageManager.MATCH_ALL).let {
            if (it.isNotEmpty()) {
                it[0].activityInfo.packageName
            } else {
                Log.d("DeviceCheck", "Камера не найдена")
                null
            }
        }
        Log.d("DeviceCheck", "cameraAppPackageName $cameraAppPackageName")

        takePictureIntent.setPackage(cameraAppPackageName)

        if (takePictureIntent.resolveActivity(packageManager) != null) {
            Log.d("DeviceCheck", "Камера найдена, запуск...")

            // Создаем директорию, если она не существует
            val photoDir = File(filesDir, "images")
            if (!photoDir.exists()) {
                photoDir.mkdirs()
            }

            originalPhotoFile = File(photoDir, "${uuid}_${currentStageCodeName}_${currentPhotoCodeName}.jpg")
            originalPhotoUri = FileProvider.getUriForFile(
                this,
                applicationContext.packageName,
                originalPhotoFile!!
            )

            Log.d("DeviceCheck", "Photo URI: $originalPhotoUri")

            // Логирование пути файла
            Log.d("DeviceCheck", "Photo file path: ${originalPhotoFile?.absolutePath}")

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, originalPhotoUri)

            // Запускаем камеру
            cameraLauncher.launch(takePictureIntent)
        } else {
            Log.d("DeviceCheck", "Камера не найдена")
            Toast.makeText(this, "Камера не найдена", Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePhotoToStorage(photoBitmap: Bitmap) {
        saveOriginalPhoto(photoBitmap)
        saveThumbnail(photoBitmap)
    }

    private fun saveOriginalPhoto(photoBitmap: Bitmap) {
        originalPhotoFile?.let { file ->
            saveBitmapToFile(photoBitmap, file)
            Toast.makeText(this, "Фото сохранено", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "Ошибка: файл для оригинала не найден", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveThumbnail(photoBitmap: Bitmap) {
        val thumbnail = Bitmap.createScaledBitmap(photoBitmap, 150, 150, true)
        val thumbnailFileName = "thumb_${uuid}_${currentStageCodeName}_${currentPhotoCodeName}.jpg"
        val thumbnailFile = File(filesDir, thumbnailFileName)  // Используем внутреннее хранилище

        saveBitmapToFile(thumbnail, thumbnailFile)

        val thumbnailUri = FileProvider.getUriForFile(
            this,
            applicationContext.packageName,
            thumbnailFile
        )

        // Обновляем иконку миниатюры
        currentStageCodeName?.let { stageCodeName ->
            currentPhotoCodeName?.let { photoCodeName ->
                stageAdapter.updateThumbnailUri(stageCodeName, photoCodeName, thumbnailUri)
            }
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        try {
            Log.d("DeviceCheck", "Trying to save photo to: ${file.absolutePath}")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            Log.d("DeviceCheck", "Фото сохранено: ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e("DeviceCheck", "Ошибка сохранения фото", e)
            Toast.makeText(this, "Ошибка сохранения фото", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun rotateImageIfRequired(bitmap: Bitmap, photoUri: Uri): Bitmap {
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

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
