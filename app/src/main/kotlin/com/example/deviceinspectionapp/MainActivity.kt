package com.example.deviceinspectionapp

import PoverkaDTO
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.deviceinspectionapp.utils.TestData
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class MainActivity : AppCompatActivity() {

    private var cameraAppPackageName: String? = null
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var photoDirectory: File
    private lateinit var poverkaDTO: PoverkaDTO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeComponents()
        setupUI()
    }

    private fun initializeComponents() {
        setupPermissionLauncher()
        setupPhotoDirectory()
        setupSettingsDirectory()
        Service.loadSettings(this)
        poverkaDTO = TestData.createTestInspectionData()
        cameraAppPackageName = findCameraApp()

        if (cameraAppPackageName == null) {
            Toast.makeText(this,"Приложение камеры не найдено. Завершаем работу.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupUI() {
        val btnStartInspection: Button = findViewById(R.id.btnStartInspection)
        btnStartInspection.setOnClickListener {
            if (checkCameraPermission()) {
                startDeviceCheckActivity()
            } else {
                requestCameraPermission()
            }
        }

        val buttonSettings: FloatingActionButton = findViewById(R.id.settings_button)
        buttonSettings.setOnClickListener {
            startSettingsActivity()
        }
    }

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

    private fun setupPhotoDirectory() {
        photoDirectory = File(filesDir, "images")
        if (!photoDirectory.exists() && !photoDirectory.mkdirs()) {
            Toast.makeText(this, "Не удалось создать директорию для фото, приложение закрыто.", Toast.LENGTH_LONG).show()
            finish()
        } else {
            Log.d("MainActivity", "Директория для фото: ${photoDirectory.absolutePath}")
        }
    }

    private fun setupSettingsDirectory() {
        val settingsDir = File(filesDir, "settings")
        if (!settingsDir.exists() && !settingsDir.mkdirs()) {
            Toast.makeText(this, "Не удалось создать директорию 'settings', приложение закрыто.", Toast.LENGTH_LONG).show()
            finish()
        } else {
            Log.d("Settings", "Директория 'settings' создана.")
        }
    }


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

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
    }

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
}
