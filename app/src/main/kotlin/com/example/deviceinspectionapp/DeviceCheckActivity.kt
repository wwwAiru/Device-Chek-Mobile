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
        photoDirectory = File(intent.getStringExtra("photoDirectoryPath") ?: "")
        Log.d("DeviceCheckActivity", "photoDirectoryPath: $photoDirectory")

        poverkaDTO = Json.decodeFromString(jsonData ?: String())

        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewStages)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Создаем и устанавливаем адаптер
        poverkaAdapter = PoverkaAdapter(this, poverkaDTO)
        recyclerView.adapter = poverkaAdapter

        // Инициализируем launcher для камеры
        takePictureLauncher = setupTakePictureLauncher()

        // Инициализация launcher для редактирования фото
        editPhotoLauncher = setupEditPhotoLauncher()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setupEditPhotoLauncher(): ActivityResultLauncher<PhotoEditorCall> {
        return registerForActivityResult(PhotoEditorCallResultPassingThrough()) { result ->
            if (result != null) {
                // Данные успешно получены
                val editedPhotoUri = result.fileUri
                val stageIdx = result.stageIdx
                val photoIdx = result.photoIdx

                // Обновляем адаптер с новыми данными
                poverkaAdapter.processPhotoEditEvent(stageIdx, photoIdx, editedPhotoUri)
            } else {
                Log.e("EditPhoto", "Редактирование не выполнено или отменено")
            }
        }
    }



    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupTakePictureLauncher(): ActivityResultLauncher<CameraCall> {
        return registerForActivityResult(CameraCallResultPassingThrough()) { result ->
            Log.d("CameraResult", "Result: $result")
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
