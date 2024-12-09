package com.example.deviceinspectionapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

lateinit var mainService: Service

class Service(private val filesDir: File) {

    var settings: Settings = Settings("127.0.0.1:8080", "user")
        set(value) {
            saveSettings(value)
            field = value
        }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    private val mutex = Mutex()
    var uploadingError: String? = null
    var hasFilesToUpload: Boolean = false

    init {
        loadSettings()
    }

    private fun findFilesToUpload(photoDirectory: File): List<File> {
        val files = photoDirectory.listFiles() ?: emptyArray()
        return files.filter { it.extension in listOf("jpg", "jpeg") }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun loadSettings() {
        val settingsFile = File(filesDir, "settings/settings.json")
        if (settingsFile.exists()) {
            val jsonString = settingsFile.readText()
            settings = Json.decodeFromString(Settings.serializer(), jsonString).also {
                Log.i("Settings", "Настройки загружены: $it")
            }
        } else {
            saveSettings(settings)
        }
    }

    private fun saveSettings(newSettings: Settings) {
        val settingsFile = File(filesDir, "settings/settings.json")
        settingsFile.parentFile?.mkdirs()
        settingsFile.writeText(Json.encodeToString(newSettings))
        Log.i("Settings", "Настройки сохранены: $newSettings")
    }

    private suspend fun uploadJson(jsonData: String): HttpResponse {
        Log.i("Service", "Отправка JSON: $jsonData")
        return client.post("http://${settings.serverAddress}/upload/json") {
            contentType(ContentType.Application.Json)
            setBody(jsonData)
        }
    }

    private suspend fun uploadPhoto(photo: File, uuid: String): HttpResponse {
        if (!photo.exists()) throw IllegalArgumentException("Файл ${photo.name} не существует.")
        if (uuid.isBlank()) throw IllegalArgumentException("UUID не может быть пустым.")

        Log.i("Service", "Отправка файла: ${photo.name} с UUID: $uuid")

        return client.submitFormWithBinaryData(
            url = "http://${settings.serverAddress}/upload/photo",
            formData = formData {
                append("uuid", uuid)
                append("file", photo.readBytes(), Headers.build {
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"${photo.name}\"")
                    append(HttpHeaders.ContentType, "application/octet-stream")
                })
            }
        )
    }

    suspend fun uploadAllPhotos(context: Context, jsonData: String, photoDirectory: File) {
        if (!isNetworkAvailable(context)) {
            uploadingError = "Нет подключения к интернету."
            Log.e("Service", uploadingError!!)
            return
        }

        mutex.withLock {
            hasFilesToUpload = findFilesToUpload(photoDirectory).isNotEmpty()

            if (!hasFilesToUpload) {
                uploadingError = "Нет фотографий для загрузки."
                Log.e("Service", uploadingError!!)
                return
            }

            try {
                // Загружаем JSON
                val jsonResponse = uploadJson(jsonData)
                if (!jsonResponse.status.isSuccess()) {
                    uploadingError = "Ошибка при загрузке JSON: ${jsonResponse.status}"
                    Log.e("Service", uploadingError!!)
                    return
                }

                // Извлекаем UUID из JSON
                val uuid = Json.decodeFromString<JsonObject>(jsonData)["uuid"]?.jsonPrimitive?.content ?: return

                // Загружаем фотографии
                for (photo in findFilesToUpload(photoDirectory)) {
                    val photoResponse = uploadPhoto(photo, uuid)
                    if (!photoResponse.status.isSuccess()) {
                        uploadingError = "Ошибка при загрузке фото ${photo.name}: ${photoResponse.status}"
                        Log.e("Service", uploadingError!!)
                        return
                    }
                }

                Log.i("Service", "Загрузка завершена.")
            } catch (e: Exception) {
                uploadingError = "Ошибка при загрузке: ${e.localizedMessage}"
                Log.e("Service", uploadingError!!)
            }
        }
    }
}

@Serializable
data class Settings(
    val serverAddress: String? = null,
    val login: String? = null
)
