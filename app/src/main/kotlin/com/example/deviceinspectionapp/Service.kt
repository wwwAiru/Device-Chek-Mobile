package com.example.deviceinspectionapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.deviceinspectionapp.dto.FileMetadata
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.UUID

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

    private fun extractUuidFromFile(file: File): UUID {
        val regex = "(?:thumb_)?([a-f0-9\\-]{36})".toRegex()
        val matchResult = regex.find(file.name)
        return if (matchResult != null) {
            UUID.fromString(matchResult.groupValues[1])
        } else {
            throw IllegalArgumentException("Не удалось извлечь UUID из имени файла: ${file.name}")
        }
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
        Log.i("Service", "Отправка файла: ${photo.name} с UUID: $uuid")
        return client.submitFormWithBinaryData(
            url = "http://${settings.serverAddress}/upload/photo",
            formData = formData {
                append("uuid", uuid)
                append("file", photo.readBytes(), Headers.build {
                    append(
                        HttpHeaders.ContentDisposition,
                        "form-data; name=\"file\"; filename=\"${photo.name}\""
                    )
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

        if (!mutex.tryLock()) {
            Log.i("Service", "Загрузка уже выполняется.")
            return
        }

        try {
            val localFiles = photoDirectory.listFiles()
                ?.filter { it.extension in listOf("jpg", "jpeg") }
                ?: emptyList()

            if (localFiles.isEmpty()) {
                uploadingError = "Нет фотографий для загрузки."
                Log.i("Service", uploadingError!!)
                hasFilesToUpload = false
                return
            }

            val jsonResponse = uploadJson(jsonData)
            if (!jsonResponse.status.isSuccess()) {
                uploadingError = "Ошибка при загрузке JSON: ${jsonResponse.status}"
                Log.e("Service", uploadingError!!)
                return
            }

            val uuid = Json.decodeFromString<JsonObject>(jsonData)["uuid"]?.jsonPrimitive?.content
            if (uuid.isNullOrEmpty()) {
                uploadingError = "Ошибка: UUID не найден в JSON."
                Log.e("Service", uploadingError!!)
                return
            }

            hasFilesToUpload = true

            while (hasFilesToUpload) {
                val localFileMetadata = localFiles.map { FileMetadata(it.name, it.lastModified()) }

                val serverFileMetadata = getServerFileList(localFileMetadata.map { extractUuidFromFile(File(it.fileName)).toString() }.toSet())

                val filesToUpload = localFiles.filter { file ->
                    val serverMetadata = serverFileMetadata.find { it.fileName == file.name }
                    serverMetadata == null || file.lastModified() > serverMetadata.lastModified
                }

                if (filesToUpload.isEmpty()) {
                    Log.i("Service", "Все файлы загружены.")
                    hasFilesToUpload = false
                    break
                }

                for (file in filesToUpload) {
                    try {
                        val response = uploadPhoto(file, uuid)
                        if (response.status.isSuccess()) {
                            Log.i("Service", "Файл ${file.name} успешно загружен.")
                        } else {
                            Log.e("Service", "Ошибка загрузки файла ${file.name}: ${response.status}")
                        }
                    } catch (e: Exception) {
                        Log.e("Service", "Ошибка при загрузке файла ${file.name}: ${e.localizedMessage}")
                    }
                }
            }
        } catch (e: Exception) {
            uploadingError = "Ошибка при загрузке: ${e.localizedMessage}"
            Log.e("Service", uploadingError!!)
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun getServerFileList(fileUUIDs: Set<String>): List<FileMetadata> {
        val response: HttpResponse = client.post("http://${settings.serverAddress}/files") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(fileUUIDs))
        }

        if (!response.status.isSuccess()) {
            Log.e("Service", "Ошибка ответа сервера: ${response.status}")
            throw IllegalStateException("Ошибка при получении списка файлов с сервера: ${response.status}")
        }

        return Json.decodeFromString(response.bodyAsText())
    }
}

@Serializable
data class Settings(
    val serverAddress: String? = null,
    val login: String? = null
)
