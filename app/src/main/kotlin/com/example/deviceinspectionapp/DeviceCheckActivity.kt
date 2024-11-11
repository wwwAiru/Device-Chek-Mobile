package com.example.deviceinspectionapp

import PoverkaAdapter
import PoverkaDTO
import android.content.Intent
import android.graphics.BitmapFactory
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class DeviceCheckActivity : AppCompatActivity() {

    private lateinit var poverkaDTO: PoverkaDTO
    private lateinit var photoDirectory: File
    private lateinit var poverkaAdapter: PoverkaAdapter
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private var currentStagePosition: Int = -1
    private var currentPhotoPosition: Int = -1

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_check)

        val jsonData = intent.getStringExtra("jsonData")
        photoDirectory = File(intent.getStringExtra("photoDirectoryPath") ?: "")

        poverkaDTO = Json.decodeFromString(jsonData ?: String())

        setupRecyclerView()
        setupTakePictureLauncher()
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewStages)
        poverkaAdapter = PoverkaAdapter(this, poverkaDTO) { stagePosition, photoPosition ->
            startCameraForPhoto(stagePosition, photoPosition)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = poverkaAdapter
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupTakePictureLauncher() {
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("CameraResult", "ResultCode: ${result.resultCode}, Data: ${result.data}")
            if (result.resultCode == RESULT_OK) {
                handlePhotoCaptured()
            } else {
                Toast.makeText(this, "Фото не было сделано", Toast.LENGTH_SHORT).show()
                Log.e("CameraError", "Ошибка при съемке фото")
            }
        }
    }


    private fun startCameraForPhoto(stagePosition: Int, photoPosition: Int) {
        currentStagePosition = stagePosition
        currentPhotoPosition = photoPosition

        val photoDTO = poverkaDTO.stages[stagePosition].photos[photoPosition]
        val photoFile = File(photoDirectory, photoDTO.imageFileName)

        // Запуск камеры для фотографирования
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Сохранение фото в файл через FileProvider
        val photoUri = FsUtils.getFileUri(this, photoFile)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        takePictureLauncher.launch(takePictureIntent)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun handlePhotoCaptured() {
        if (currentStagePosition != -1 && currentPhotoPosition != -1) {
            val stageDTO = poverkaDTO.stages[currentStagePosition]
            val photoDTO = stageDTO.photos[currentPhotoPosition]
            val photoFile = File(photoDirectory, photoDTO.imageFileName)

            if (photoFile.exists()) {
                val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                val rotatedBitmap = BitmapUtils.rotateImageIfRequired(originalBitmap, Uri.fromFile(photoFile), contentResolver)

                // Сохраняем окончательно обработанное фото
                FsUtils.savePhotoToInternalStorage(this, rotatedBitmap, photoDTO.imageFileName)

                // Обновляем URI в PhotoDTO
                photoDTO.uri = FsUtils.getFileUri(this, photoFile).toString()

                // Создаём миниатюру из оригинального фото для отображения в интерфейсе
                val thumbnailBitmap = BitmapUtils.createThumbnailFromFile(photoFile)
                poverkaAdapter.updatePhotoThumbnail(currentStagePosition, currentPhotoPosition, thumbnailBitmap)

                saveInspectionState()
            } else {
                Log.e("DeviceCheckActivity", "Файл фото не найден по пути: ${photoFile.absolutePath}")
            }
        }
    }


    private fun saveInspectionState() {
        val jsonFile = File(filesDir, "inspection_data.json")
        FsUtils.saveJsonToFile(this, Json.encodeToString(poverkaDTO), jsonFile.name)
    }
}
