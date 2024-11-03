package com.example.deviceinspectionapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Находим кнопку по её ID
        val btnStartInspection: Button = findViewById(R.id.btnStartInspection)

        // Устанавливаем слушатель нажатия
        btnStartInspection.setOnClickListener {
            // Создаем тестовые данные в формате JSON
            val testData = PoverkaDTO(
                caption = "Пример поверки",
                stages = listOf(
                    StageDTO("check1", "Этап 1", description = "", photos = listOf()),
                    StageDTO("check2", "Этап 2", description = "", photos = listOf())
                )
            )
            val jsonData = Json.encodeToString(testData)

            // Создаем намерение для перехода на DeviceCheckActivity и передаем JSON
            val intent = Intent(this, DeviceCheckActivity::class.java).apply {
                putExtra("jsonData", jsonData)
            }
            startActivity(intent) // Переход на новое активити
        }
    }
}
