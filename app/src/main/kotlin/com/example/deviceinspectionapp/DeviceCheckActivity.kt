package com.example.deviceinspectionapp

import PoverkaAdapter
import PoverkaDTO
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.serialization.json.Json
import java.io.File

class DeviceCheckActivity : AppCompatActivity() {

    private lateinit var poverkaDTO: PoverkaDTO
    private lateinit var poverkaAdapter: PoverkaAdapter

    lateinit var photoDirectory: File
    lateinit var takePictureLauncher: ActivityResultLauncher<CameraCall>

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_check)

        // Получаем данные из Intent
        val jsonData = intent.getStringExtra("jsonData")
        photoDirectory = File(intent.getStringExtra("photoDirectoryPath") ?: "")
        Log.d("DeviceCheckActivity", "photoDirectoryPath: $photoDirectory")

        poverkaDTO = Json.decodeFromString(jsonData ?: String())

        // Настроим RecyclerView с GridLayoutManager
        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewStages)
        val gridLayoutManager = GridLayoutManager(this, 2) // 2 колонки для фото
        recyclerView.layoutManager = gridLayoutManager

        // Создаем и устанавливаем адаптер
        poverkaAdapter = PoverkaAdapter(this, poverkaDTO)
        recyclerView.adapter = poverkaAdapter

        // Инициализируем launcher для камеры
        takePictureLauncher = setupTakePictureLauncher()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupTakePictureLauncher(): ActivityResultLauncher<CameraCall> {
        return registerForActivityResult(CameraCallResultPassingThrough()) { result ->
            Log.d("CameraResult", "Result: ${result}")
            if (result != null) {
                // Обрабатываем результат съемки фотографии
                poverkaAdapter.processPhotoTakenEvent(result)
            } else {
                // Если фото не было сделано
                Toast.makeText(this, "Фото не было сделано", Toast.LENGTH_SHORT).show()
                Log.e("CameraError", "Ошибка при съемке фото")
            }
        }
    }
}
