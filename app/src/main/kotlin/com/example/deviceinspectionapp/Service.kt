package com.example.deviceinspectionapp

import android.util.Log
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

lateinit var mainService: Service

class Service(private val filesDir: File) {

    lateinit var settings: Settings

    init {
        loadSettings()
    }

    // Загрузка настроек
    fun loadSettings(): Settings {
        val settingsFile = File(filesDir, "settings/settings.json")
        return if (settingsFile.exists()) {
            val jsonString = settingsFile.readText()
            Json.decodeFromString(Settings.serializer(), jsonString).also {
                Log.i("Settings", "Настройки загружены")
                settings = it
            }
        } else {
            // Если файла настроек нет, создаем дефолтные настройки
            val defaultSettings = Settings(serverAddress = "192.168.1.0", login = "admin")
            saveSettings(defaultSettings) // Сохраняем дефолтные настройки
            settings = defaultSettings
            defaultSettings
        }
    }

    // Сохранение настроек
    fun saveSettings(newSettings: Settings = settings) {
        val settingsFile = File(filesDir, "settings/settings.json")
        settingsFile.parentFile?.mkdirs() // Создаем директорию, если она не существует
        val jsonString = Json.encodeToString(newSettings)
        settingsFile.writeText(jsonString)
        Log.i("Settings", "Настройки сохранены")
    }
}

// Класс для настроек
@kotlinx.serialization.Serializable
data class Settings(
    @SerialName("server_address")
    val serverAddress: String,
    val login: String
)
