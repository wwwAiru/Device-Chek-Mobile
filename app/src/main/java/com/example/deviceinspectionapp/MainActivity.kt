package com.example.deviceinspectionapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Находим кнопку по её ID
        val btnStartInspection: Button = findViewById(R.id.btnStartInspection)

        // Устанавливаем слушатель нажатия
        btnStartInspection.setOnClickListener {
            // Создаем намерение для перехода на DeviceCheckActivity
            val intent = Intent(this, DeviceCheckActivity::class.java)
            startActivity(intent) // Переход на новое активити
        }
    }
}
