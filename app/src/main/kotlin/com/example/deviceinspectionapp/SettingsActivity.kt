package com.example.deviceinspectionapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var etServerAddress: EditText
    private lateinit var etLogin: EditText
    private lateinit var btnTestServer: Button
    private lateinit var btnTestLogin: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etServerAddress = findViewById(R.id.etServerAddress)
        etLogin = findViewById(R.id.etLogin)
        btnTestServer = findViewById(R.id.btnTestServer)
        btnTestLogin = findViewById(R.id.btnTestLogin)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        mainService.settings.let {
            etServerAddress.setText(it.serverAddress)
            etLogin.setText(it.login)
        }

        // Обработчик кнопки "Тест" для адреса сервера
        btnTestServer.setOnClickListener {
            val serverAddress = etServerAddress.text.toString()
            testServerConnection(serverAddress)
        }

        // Обработчик кнопки "Тест" для логина
        btnTestLogin.setOnClickListener {
            val login = etLogin.text.toString()
            testLogin(login)
        }

        // Обработчик кнопки "Сохранить"
        btnSave.setOnClickListener {
            val serverAddress = etServerAddress.text.toString()
            val login = etLogin.text.toString()

            // Сохраняем настройки
            val newSettings = Settings(serverAddress, login)
            mainService.settings = newSettings
            Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Обработчик кнопки "Отмена"
        btnCancel.setOnClickListener {
            finish()
        }
    }


    private fun testServerConnection(serverAddress: String) {
        etServerAddress.setTextColor(getColor(R.color.neutral)) // Сбрасываем цвет в нейтральный
        etServerAddress.clearFocus()

        mainService.settings = mainService.settings.copy(serverAddress = serverAddress)

        lifecycleScope.launch {
            val isConnected = mainService.checkServerConnection()
            if (isConnected) {
                etServerAddress.setTextColor(getColor(R.color.success)) // Зеленый цвет
                Toast.makeText(this@SettingsActivity, "Соединение успешно!", Toast.LENGTH_SHORT).show()
            } else {
                etServerAddress.setTextColor(getColor(R.color.error)) // Красный цвет
                Toast.makeText(this@SettingsActivity, "Ошибка соединения!", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun testLogin(login: String) {
        Toast.makeText(this, "Тест логина := $login", Toast.LENGTH_SHORT).show()
    }
}
