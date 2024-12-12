package com.example.deviceinspectionapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.deviceinspectionapp.dto.FileMetadata
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.UUID

lateinit var mainService: Service

class Service(private val filesDir: File) {

    var settings: Settings = Settings("192.168.0.118:8080", "user")
        set(value) {
            saveSettings(value)
            field = value
        }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 1000
        }
    }

    private val mutex = Mutex()
    var uploadingMessage: String? = null
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

    suspend fun checkServerConnection(): Boolean {
        return try {
            val response: HttpResponse = client.get("http://${settings.serverAddress}/ping")
            response.status.isSuccess()
        } catch (e: Exception) {
            Log.e("Service", "Ошибка соединения с сервером: ${e.localizedMessage}")
            withContext(Dispatchers.Main) {
            }
            false
        }
    }

    suspend fun uploadAllPhotos(
        context: Context,
        jsonData: String,
        photoDirectory: File,
        progressListener: ProgressListener,
        uploadStateListener: UploadStateListener
    ) {
        var uploadedBytes: Long = 0
        val totalBytes: Long

        if (!isNetworkAvailable(context)) {
            uploadingMessage = "Нет подключения к интернету."
            Log.e("Service", "Нет подключения к интернету. Завершение метода.")
            withContext(Dispatchers.Main) {
                uploadStateListener.updateState(UploadState.ERROR)
            }
            return
        }

        if (!mutex.tryLock()) {
            Log.i("Service", "Загрузка уже выполняется. Завершение метода.")
            return
        }

        try {
            val localFiles = photoDirectory.listFiles()
                ?.filter { it.extension in listOf("jpg", "jpeg") }
                ?: emptyList()

            if (localFiles.isEmpty()) {
                uploadingMessage = "Нет фотографий для выгрузки."
                Log.i("Service", "Нет фотографий для выгрузки. Завершение метода.")
                withContext(Dispatchers.Main) {
                    uploadStateListener.updateState(UploadState.DEFAULT)
                }
                return
            }

            uploadingMessage = null
            Log.i("Service", "Файлы найдены: ${localFiles.size}. Начинаем отправку JSON.")

            // Вычисление общего размера всех файлов
            totalBytes = localFiles.sumOf { it.length() }

            withContext(Dispatchers.Main) {
                progressListener.updateProgress(0) // Устанавливаем начальный прогресс
            }

            val jsonResponse = withContext(Dispatchers.IO) {
                uploadJson(jsonData)
            }
            if (!jsonResponse.status.isSuccess()) {
                Log.e("Service", "Ошибка при отправке JSON: ${jsonResponse.status}")
                withContext(Dispatchers.Main) {
                    uploadStateListener.updateState(UploadState.ERROR)
                }
                return
            }

            val uuid = Json.decodeFromString<JsonObject>(jsonData)["uuid"]?.jsonPrimitive?.content
            if (uuid.isNullOrEmpty()) {
                Log.e("Service", "UUID отсутствует в JSON. Завершение метода.")
                withContext(Dispatchers.Main) {
                    uploadStateListener.updateState(UploadState.ERROR)
                }
                return
            }

            // Получение списка файлов с сервера
            val serverFileList = withContext(Dispatchers.IO) {
                getServerFileList(localFiles.mapNotNull { extractUuidFromFile(it).toString() }.toSet())
            }

            Log.i("Service", "Метаданные файлов на сервере получены: ${serverFileList.size}")

            for (file in localFiles) {
                try {
                    val serverFileMetadata = serverFileList.find { it.fileName == file.name }
                    val shouldUpload = serverFileMetadata == null || file.lastModified() > serverFileMetadata.lastModified

                    when {
                        shouldUpload -> {
                            val response = withContext(Dispatchers.IO) {
                                uploadPhoto(file, uuid)
                            }
                            if (response.status.isSuccess()) {
                                Log.i("Service", "Файл ${file.name} успешно загружен.")
                                uploadedBytes += file.length()
                            } else {
                                Log.e("Service", "Ошибка загрузки файла ${file.name}: ${response.status}")
                                continue
                            }
                        }
                        else -> {
                            Log.i("Service", "Файл ${file.name} уже загружен и актуален.")
                            uploadedBytes += file.length()
                        }
                    }

                    val progress = ((uploadedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt()
                    withContext(Dispatchers.Main) {
                        progressListener.updateProgress(progress)
                    }

                } catch (e: Exception) {
                    Log.e("Service", "Ошибка при обработке файла ${file.name}: ${e.localizedMessage}")
                }
            }

            uploadingMessage = "Все фото отправлены"
            Log.i("Service", "Все фото отправлены.")

            withContext(Dispatchers.Main) {
                uploadStateListener.updateState(UploadState.SUCCESS)
            }

        } catch (e: Exception) {
            Log.e("Service", "Ошибка при выполнении метода: ${e.localizedMessage}")
            withContext(Dispatchers.Main) {
                uploadStateListener.updateState(UploadState.ERROR)
            }
        } finally {
            Log.i("Service", "Освобождаем мьютекс.")
            mutex.unlock()
        }
        Log.d("Service", "Метод uploadAllPhotos завершён.")
    }




    private suspend fun getServerFileList(fileUUIDs: Set<String>): Set<FileMetadata> {
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
