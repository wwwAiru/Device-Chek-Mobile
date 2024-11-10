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

        // Кнопка для начала новой поверки
        val btnStartInspection: Button = findViewById(R.id.btnStartInspection)

        // Обработчик нажатия кнопки
        btnStartInspection.setOnClickListener {
            val jsonData = Json.encodeToString(TestData.createTestInspectionData())

            // Переход на DeviceCheckActivity с передачей JSON данных
            val intent = Intent(this, DeviceCheckActivity::class.java).apply {
                putExtra("jsonData", jsonData)
            }
            startActivity(intent)
        }
    }
}
