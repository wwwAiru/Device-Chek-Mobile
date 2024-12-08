package com.example.deviceinspectionapp

import PoverkaDTO
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.deviceinspectionapp.utils.TestData
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class MainActivity : AppCompatActivity() {

    private var cameraAppPackageName: String? = null
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var photoDirectory: File
    private lateinit var poverkaDTO: PoverkaDTO
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация компонентов
        initializeComponents()

        // Настройка UI
        setupUI()
    }

    // Отображение меню
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        return true
    }

    // Обработка действий меню
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.toolbar_menu_action_settings -> {
                startSettingsActivity()
                true
            }
            R.id.action_upload -> {
                uploadPhotos()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Инициализация компонентов
    private fun initializeComponents() {
        // Инициализация сервиса
        mainService = Service(filesDir)
        setupPermissionLauncher()
        setupPhotoDirectory()
        poverkaDTO = TestData.createTestInspectionData()
        cameraAppPackageName = findCameraApp()

        if (cameraAppPackageName == null) {
            Toast.makeText(this, "Приложение камеры не найдено. Завершаем работу.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // Настройка UI
    private fun setupUI() {
        // Настройка Toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        // кнопка Начало новой поверки
        val btnStartInspection: Button = findViewById(R.id.btnStartInspection)
        btnStartInspection.setOnClickListener {
            if (checkCameraPermission()) {
                startDeviceCheckActivity()
            } else {
                requestCameraPermission()
            }
        }
    }

    // Настройка лаунчера для разрешений
    private fun setupPermissionLauncher() {
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val isCameraPermissionGranted = permissions[Manifest.permission.CAMERA] == true
                val message = if (isCameraPermissionGranted) {
                    "Разрешение на камеру предоставлено."
                } else {
                    "Разрешение на камеру не предоставлено."
                }
                Toast.makeText(
                    this,
                    message,
                    if (isCameraPermissionGranted) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                ).show()
            }
    }

    // Настройка директории для фото
    private fun setupPhotoDirectory() {
        photoDirectory = File(filesDir, "images")
        if (!photoDirectory.exists() && !photoDirectory.mkdirs()) {
            Toast.makeText(this, "Не удалось создать директорию для фото, приложение закрыто.", Toast.LENGTH_LONG).show()
            finish()
        } else {
            Log.d("MainActivity", "Директория для фото: ${photoDirectory.absolutePath}")
        }
    }

    // Поиск камеры
    private fun findCameraApp(): String? {
        return packageManager.queryIntentActivities(
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
            PackageManager.MATCH_ALL
        ).let {
            if (it.isNotEmpty()) {
                it[0].activityInfo.packageName
            } else {
                Log.d("MainActivity", "Камера не найдена")
                Toast.makeText(this, "Приложение камера не найдено", Toast.LENGTH_SHORT).show()
                null
            }
        }
    }

    // Проверка разрешений на камеру
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    // Запрос на разрешение камеры
    private fun requestCameraPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
    }

    // Запуск SettingsActivity
    private fun startSettingsActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun startDeviceCheckActivity() {
        if (cameraAppPackageName.isNullOrBlank()) {
            Toast.makeText(this, "Ошибка: приложение камеры не найдено.", Toast.LENGTH_LONG).show()
            return
        }

        if (!photoDirectory.exists()) {
            Toast.makeText(this, "Ошибка: директория для фото отсутствует.", Toast.LENGTH_LONG).show()
            return
        }

        val poverkaJson = Json.encodeToString(poverkaDTO)
        val intent = Intent(this, DeviceCheckActivity::class.java).apply {
            putExtra("jsonData", poverkaJson)
            putExtra("cameraAppPackageName", cameraAppPackageName)
            putExtra("photoDirectoryPath", photoDirectory.absolutePath)
        }
        startActivity(intent)
    }

    // Загрузка фото на сервер
    private fun uploadPhotos() {
        mainService.uploadAllPhotos(this, photoDirectory) { success ->
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Выгрузка фото завершена успешно", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Ошибка при выгрузке фото", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
