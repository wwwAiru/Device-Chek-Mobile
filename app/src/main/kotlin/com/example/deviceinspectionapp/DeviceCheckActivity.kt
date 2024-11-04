package com.example.deviceinspectionapp

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.OutputStream

class DeviceCheckActivity : AppCompatActivity() {
    private lateinit var stageAdapter: StageManager
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var uuid: String
    private var currentStageCodeName: String? = null
    private var currentPhotoCodeName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_check)

        // Получение JSON данных и парсинг
        val jsonData = intent.getStringExtra("jsonData")
        val poverkaData = Json.decodeFromString<PoverkaDTO>(jsonData!!)
        uuid = poverkaData.uuid

        // Инициализация RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewStages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        stageAdapter = StageManager(poverkaData.stages) { stageCodeName, photoCodeName ->
            currentStageCodeName = stageCodeName
            currentPhotoCodeName = photoCodeName
            onCameraIconClicked()
        }
        recyclerView.adapter = stageAdapter

        // Настройка запуска камеры
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val photoBitmap = result.data?.extras?.get("data") as Bitmap
                savePhotoToStorage(photoBitmap)
            }
        }

        // Настройка запроса разрешений
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "Разрешение на использование камеры не предоставлено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Обработчик нажатия на иконку камеры.
     * Проверяет разрешения, если их нет, запрашивает у пользователя.
     */
    private fun onCameraIconClicked() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /**
     * Запуск камеры для получения фотографии.
     */
    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(cameraIntent)
    }

    /**
     * Сохраняет фотографию в локальное хранилище с уникальным именем файла.
     *
     * @param photoBitmap Bitmap изображения для сохранения.
     */
    private fun savePhotoToStorage(photoBitmap: Bitmap) {
        val fileName = "${uuid}_${currentStageCodeName}_${currentPhotoCodeName}.jpg"

        // Настройка параметров для сохранения изображения в медиа-хранилище
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DeviceInspectionApp")
        }

        // Получаем доступ к медиахранилищу
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            var outputStream: OutputStream? = null
            try {
                outputStream = contentResolver.openOutputStream(uri)
                // Сохранение Bitmap в JPEG файл
                outputStream?.let { photoBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
                Toast.makeText(this, "Фото сохранено: $fileName", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(this, "Ошибка сохранения фото", Toast.LENGTH_SHORT).show()
            } finally {
                outputStream?.close()
            }
        } else {
            Toast.makeText(this, "Ошибка доступа к медиахранилищу", Toast.LENGTH_SHORT).show()
        }
    }

}
