package com.example.deviceinspectionapp

import PoverkaAdapter
import PoverkaDTO
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
    lateinit var takePictureLauncher: ActivityResultLauncher<Intent>

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_check)

        val jsonData = intent.getStringExtra("jsonData")
        photoDirectory = File(intent.getStringExtra("photoDirectoryPath") ?: "")
        Log.d("", "photoDirectoryPath $photoDirectory")

        poverkaDTO = Json.decodeFromString(jsonData ?: String())

        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewStages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        poverkaAdapter = PoverkaAdapter(this, poverkaDTO)
        recyclerView.adapter = poverkaAdapter

        takePictureLauncher = setupTakePictureLauncher()
    }


    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupTakePictureLauncher(): ActivityResultLauncher<Intent> {
        return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("CameraResult", "ResultCode: ${result.resultCode}, Data: ${result.data}")
            if (result.resultCode == RESULT_OK) {
                poverkaAdapter.processPhotoTakenEvent()
            } else {
                Toast.makeText(this, "Фото не было сделано", Toast.LENGTH_SHORT).show()
                Log.e("CameraError", "Ошибка при съемке фото")
            }
        }
    }
}
