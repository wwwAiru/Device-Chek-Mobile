package com.example.deviceinspectionapp

import PoverkaDTO
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.serialization.json.Json
import java.io.File

class DeviceCheckActivity : AppCompatActivity() {

    private lateinit var poverkaDTO: PoverkaDTO
    private lateinit var poverkaAdapter: PoverkaAdapter

    lateinit var photoDirectory: File
    lateinit var takePictureLauncher: ActivityResultLauncher<CameraCall>
    lateinit var editPhotoLauncher: ActivityResultLauncher<PhotoEditorCall>


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_check)

        // Получаем данные из Intent
        val jsonData = intent.getStringExtra("jsonData")
        val photoDirectoryPath = intent.getStringExtra("photoDirectoryPath")

        photoDirectory = File(photoDirectoryPath)

        poverkaDTO = Json.decodeFromString(jsonData!!)

        // Устанавливаем RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewStages)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Создаем и устанавливаем адаптер
        poverkaAdapter = PoverkaAdapter(this, poverkaDTO)
        recyclerView.adapter = poverkaAdapter

        // Инициализируем launcher для камеры и редактирования
        takePictureLauncher = setupTakePictureLauncher()
        editPhotoLauncher = setupEditPhotoLauncher()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setupEditPhotoLauncher(): ActivityResultLauncher<PhotoEditorCall> {
        return registerForActivityResult(PhotoEditorCallResultPassingThrough()) { result ->
            if (result == null) {
                Log.i("EditPhoto", "Редактирование не выполнено или отменено")
                return@registerForActivityResult
            }
            try {
                poverkaAdapter.processPhotoEditEvent(result.stageIdx, result.photoIdx, result.fileUri)
            } catch (e: Exception) {
                Log.e("EditPhotoError", "Ошибка: ${e.message}")
                throw e
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupTakePictureLauncher(): ActivityResultLauncher<CameraCall> {
        return registerForActivityResult(CameraCallResultPassingThrough()) { result ->
            if (result == null) {
                Toast.makeText(this, "Фото не было сделано", Toast.LENGTH_SHORT).show()
                Log.e("CameraError", "Ошибка при съемке фото")
                return@registerForActivityResult
            }
            try {
                poverkaAdapter.processPhotoTakenEvent(result)
            } catch (e: Exception) {
                Log.e("CameraError", "Ошибка: ${e.message}")
                throw e
            }
        }
    }
}
