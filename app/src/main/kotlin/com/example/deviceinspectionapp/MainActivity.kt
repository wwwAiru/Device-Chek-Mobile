package com.example.deviceinspectionapp

import PhotoDTO
import PoverkaDTO
import StageDTO
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
}
