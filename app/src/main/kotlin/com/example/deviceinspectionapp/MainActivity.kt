package com.example.deviceinspectionapp

import PoverkaDTO
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.deviceinspectionapp.utils.TestData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class MainActivity : AppCompatActivity() {

    private var cameraAppPackageName: String? = null
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var photoDirectory: File
    private lateinit var poverkaDTO: PoverkaDTO

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация запуска разрешений
        setupPermissionLauncher()

        // Инициализация директории для фото
        setupPhotoDirectory()

        // Загрузка данных поверки из JSON или генерация заглушек
        poverkaDTO = TestData.createTestInspectionData()

        // Ищем приложение для камеры
        cameraAppPackageName = findCameraApp()
        if (cameraAppPackageName == null) {
            Toast.makeText(this, "Приложение камеры не найдено. Завершаем работу.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Кнопка для начала новой поверки
        val btnStartInspection: Button = findViewById(R.id.btnStartInspection)
        btnStartInspection.setOnClickListener {
            if (checkPermissions()) {
                startDeviceCheckActivity()
            } else {
                requestPermissions()
            }
        }
    }

    private fun setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allPermissionsGranted = permissions.all { it.value }
            if (allPermissionsGranted) {
                Toast.makeText(this, "Разрешение предоставлено.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Разрешение не предоставлено, приложение закрыто.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun setupPhotoDirectory() {
        photoDirectory = File(filesDir, "images")
        if (!photoDirectory.exists() && !photoDirectory.mkdirs()) {
            Toast.makeText(this, "Не удалось создать директорию для фото, приложение закрыто.", Toast.LENGTH_LONG).show()
            finish()
        } else {
            Log.d("MainActivity", "Директория для фото: ${photoDirectory.absolutePath}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun findCameraApp(): String? {
        return packageManager.queryIntentActivities(
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
            PackageManager.MATCH_ALL
        )
            .let {
                if (it.isNotEmpty()) {
                    it[0].activityInfo.packageName
                } else {
                    Log.d("MainActivity", "Камера не найдена")
                    Toast.makeText(this, "Приложение камера не найдено", Toast.LENGTH_SHORT).show()
                    null
                }
            }
    }

    private fun checkPermissions(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val storagePermission = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            PackageManager.PERMISSION_GRANTED
        }
        return cameraPermission == PackageManager.PERMISSION_GRANTED && storagePermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions.toTypedArray())
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
}
