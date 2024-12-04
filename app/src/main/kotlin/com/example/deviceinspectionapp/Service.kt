package com.example.deviceinspectionapp

import android.content.Context
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object Service {

    var settings: Settings? = null

    fun loadSettings(context: Context) {
        val settingsFile = File(context.filesDir, "settings/settings.json")
        if (settingsFile.exists()) {
            val jsonString = settingsFile.readText()
            settings = Json.decodeFromString(Settings.serializer(), jsonString)
            Log.i("Settings", "Настройки загружены")
        }

    }

    fun saveSettings(context: Context) {
        val settingsFile = File(context.filesDir, "settings/settings.json")
        val jsonString = Json.encodeToString(settings ?: Settings("defaultServer", "defaultLogin"))
        settingsFile.writeText(jsonString)
        Log.i("Settings", "Настройки сохранены")
    }

}


@kotlinx.serialization.Serializable
data class Settings(
    val serverAddress: String,
    val login: String
)
