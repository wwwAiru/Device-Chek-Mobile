package com.example.deviceinspectionapp

import PhotoDTO
import PoverkaDTO
import StageDTO
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUEST_CODE_MANAGE_STORAGE = 1002
    private lateinit var manageStoragePermissionLauncher: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Проверяем и запрашиваем разрешения, если нужно
        manageStoragePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // Проверяем результат: предоставлено ли разрешение
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                Toast.makeText(this, "Разрешение на управление хранилищем получено", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Разрешение на управление хранилищем не получено", Toast.LENGTH_SHORT).show()
            }
        }

        // Проверка и запрос разрешений
        if (!hasPermissions()) {
            requestPermissions()
        }

        // Кнопка для начала новой поверки
        val btnStartInspection: Button = findViewById(R.id.btnStartInspection)

        // Обработчик нажатия кнопки
        btnStartInspection.setOnClickListener {
            // Создаем тестовые данные, используя новый формат JSON с учетом формата имени файлов
            val testData = PoverkaDTO(
                uuid = "3b45f2a2-d2ad-4a0a-bbcf-68b8e25326cf",
                stages = listOf(
                    StageDTO(
                        stageCodeName = "check1",
                        caption = "пролив №1",
                        photos = listOf(
                            PhotoDTO(
                                photoCodeName = "gidrometr",
                                caption = "гидрометр",
                                imageFileName = "3b45f2a2-d2ad-4a0a-bbcf-68b8e25326cf_check1_gidrometr.jpg"
                            )
                        )
                    )
                )
            )
            val jsonData = Json.encodeToString(testData)

            // Переход на DeviceCheckActivity с передачей JSON данных
            val intent = Intent(this, DeviceCheckActivity::class.java).apply {
                putExtra("jsonData", jsonData)
            }
            startActivity(intent)
        }
    }

    // Проверяем, есть ли все необходимые разрешения
    private fun hasPermissions(): Boolean {
        val readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        val writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        Log.d("MainActivity", "Read Permission: $readPermission, Write Permission: $writePermission")

        // Для Android 11 и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val manageStoragePermission = Environment.isExternalStorageManager()
            Log.d("MainActivity", "Manage Storage Permission: $manageStoragePermission")
            return readPermission == PackageManager.PERMISSION_GRANTED &&
                    writePermission == PackageManager.PERMISSION_GRANTED &&
                    manageStoragePermission
        }

        // Для старых версий Android
        return readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED
    }

    // Запрашиваем разрешения
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestManageStoragePermission()
            }
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_PERMISSIONS)
        }
    }


    // Запрашиваем разрешение на управление всеми файлами для Android 11 и выше
    private fun requestManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            // Запускаем намерение через зарегистрированный лаунчер
            manageStoragePermissionLauncher.launch(intent)
        } else {
            Toast.makeText(this, "Ваше устройство не поддерживает этот запрос разрешений", Toast.LENGTH_SHORT).show()
        }
    }



    // Обрабатываем результат запроса разрешений
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешения получены", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Разрешения не предоставлены", Toast.LENGTH_SHORT).show()
            }
        }

        // Обработка ответа на запрос разрешения для управления хранилищем
        if (requestCode == REQUEST_CODE_MANAGE_STORAGE) {
            val hasPermission = Environment.isExternalStorageManager()
            if (hasPermission) {
                Toast.makeText(this, "Разрешение на управление хранилищем получено", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Разрешение на управление хранилищем не получено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_MANAGE_STORAGE) {
            if (Environment.isExternalStorageManager()) {
                Toast.makeText(this, "Разрешение на управление хранилищем получено", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Разрешение на управление хранилищем не получено", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
