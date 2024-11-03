package com.example.deviceinspectionapp

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DeviceCheckActivity : AppCompatActivity() {
    private lateinit var stageAdapter: StageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_check)

        // Пример данных для тестирования (позже заменим реальными данными)
        val testStages = listOf(
            StageDTO("check1", "Этап 1", description = "", photos = listOf()),
            StageDTO("check2", "Этап 2", description = "", photos = listOf())
        )

        // Настройка RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewStages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        stageAdapter = StageManager(testStages)
        recyclerView.adapter = stageAdapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val photoBitmap = data?.extras?.get("data") as Bitmap
        }
    }

}
