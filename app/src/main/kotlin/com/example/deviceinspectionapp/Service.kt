package com.example.deviceinspectionapp

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

lateinit var mainService: Service

class Service(private val filesDir: File) {

    var settings: Settings = Settings()
        set(v) {
            saveSettings(v)
            field = v
        }

    init {
        loadSettings()
    }

    // Загрузка настроек
    private fun loadSettings() {
        val settingsFile = File(filesDir, "settings/settings.json")
        if (settingsFile.exists()) {
            val jsonString = settingsFile.readText()
            settings = Json.decodeFromString(Settings.serializer(), jsonString).also {
                Log.i("Settings", "Настройки загружены: $it")
            }
        } else {
            // Если файла настроек нет, создаем дефолтные настройки
            val defaultSettings = Settings(serverAddress = "192.168.1.1", login = "user")
            saveSettings(defaultSettings) // Сохраняем дефолтные настройки
            settings = defaultSettings
        }
    }

    // Сохранение настроек
    private fun saveSettings(newSettings: Settings) {
        val settingsFile = File(filesDir, "settings/settings.json")
        settingsFile.parentFile?.mkdirs() // Создаем директорию, если она не существует
        val jsonString = Json.encodeToString(newSettings)
        settingsFile.writeText(jsonString)
        Log.i("Settings", "Настройки сохранены: $jsonString")
    }
}

// Класс для настроек
@Serializable
data class Settings(
    val serverAddress: String? = null,
    val login: String? = null
)
