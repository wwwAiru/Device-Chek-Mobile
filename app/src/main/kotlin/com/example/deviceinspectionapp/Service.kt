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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

lateinit var mainService: Service

class Service(val updateFilesSyncUI: () -> Unit, private val filesDir: File, private val context: Context) {

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
    @Volatile
    var hasFilesToUpload: Boolean = false
        private set
    @Volatile
    var progress = 0
        private set
    @Volatile
    var uploadingErrorMessage: String? = null
        private set

    init {
        loadSettings()
    }

    fun notifyFilesChanged() {
        hasFilesToUpload = true
        updateFilesSyncUI()
    }

    fun isUploadingRunning(): Boolean {
        return mutex.isLocked
    }

    fun hasUploadingError(): Boolean {
        return uploadingErrorMessage != null
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

    private suspend fun uploadFile(file: File): HttpResponse {
        val uuid = extractUuidFromFile(file).toString()
        Log.i("Service", "Отправка файла: ${file.name} с UUID: $uuid")
        return client.submitFormWithBinaryData(
            url = "http://${settings.serverAddress}/upload/photo",
            formData = formData {
                append("uuid", uuid)
                append("file", file.readBytes(), Headers.build {
                    append(
                        HttpHeaders.ContentDisposition,
                        "form-data; name=\"file\"; filename=\"${file.name}\""
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
            false
        }
    }

    private suspend fun findFilesToUpload(): List<File> {
        val checksDir = File(filesDir, "checks")
        Log.i("Service", "Путь к директории файлов: ${checksDir.absolutePath}")

        val localFiles = checksDir.listFiles()
            ?.filter { it.extension in listOf("jpg", "jpeg", "json") }
            ?: emptyList()

        Log.i("Service", "Найденные файлы: ${localFiles.map { it.name }}")

        val serverFileList = getServerFileList(localFiles.mapNotNull { extractUuidFromFile(it).toString() }.toSet())

        val filesToUpload = localFiles.filter { file ->
            val serverFileMetadata = serverFileList.find { it.fileName == file.name }
            serverFileMetadata == null || file.lastModified() > serverFileMetadata.lastModified
        }
        return filesToUpload
    }




    fun uploadAllPhotos() {
        if (!isNetworkAvailable(context)) {
            uploadingErrorMessage = "Нет подключения к интернету."
            Log.e("Service", "Нет подключения к интернету. Завершение метода.")
            updateFilesSyncUI()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!mutex.tryLock()) {
                    Log.d("Service", "Files sync process already running - skip")
                    return@launch
                }
                Log.d("Service", "Files sync process started.")

                var totalBytes = 0L
                var uploadedBytes = 0L
                var filesToUpload = findFilesToUpload()

                if (filesToUpload.isEmpty()) {
                    Log.d("Service", "No files to upload found.")
                    hasFilesToUpload = false
                    uploadingErrorMessage = null
                    updateFilesSyncUI()
                    return@launch
                }
                Log.d("Service", "Found files to upload.")
                hasFilesToUpload = true
                progress = 0
                uploadingErrorMessage = null
                updateFilesSyncUI()

                for (i in 0..10000) {
                    totalBytes += filesToUpload.sumOf { it.length() }

                    for (file in filesToUpload) {
                        try {
                            val response = uploadFile(file)
                            if (response.status.isSuccess()) {
                                Log.i("Service", "Файл ${file.name} успешно загружен.")
                                uploadedBytes += file.length()
                                progress = ((uploadedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt()
                                Log.d("Service", "uploadedBytes = $uploadedBytes; /totalBytes = $totalBytes Progress: $progress")
                                updateFilesSyncUI()
                            } else {
                                Log.e(
                                    "Service",
                                    "Ошибка загрузки файла ${file.name}: ${response.status}"
                                )
                                uploadingErrorMessage =
                                    "Ошибка загрузки файла ${file.name}: ${response.status}"
                                return@launch
                            }
                        } catch (e: Exception) {
                            Log.e(
                                "Service",
                                "Исключение при загрузке файла ${file.name}: ${e.localizedMessage}"
                            )
                            uploadingErrorMessage =
                                "Исключение при загрузке файла ${file.name}: ${e.localizedMessage}"
                            return@launch
                        }
                    }

                    filesToUpload = findFilesToUpload()
                    if (filesToUpload.isEmpty()) {
                        Log.d("Service", "All files are uploaded.")
                        hasFilesToUpload = false
                        uploadingErrorMessage = null
                        progress = 100
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("Service", e.toString())
                uploadingErrorMessage = e.localizedMessage
            } finally {
                Log.d("Service", "Files sync process finished.")
                mutex.unlock()
                updateFilesSyncUI()
            }
        }
    }

    private suspend fun getServerFileList(fileUUIDs: Set<String>): Set<FileMetadata> {
        Log.i("Service", "Отправка UUID на сервер: $fileUUIDs")
        val response: HttpResponse = client.post("http://${settings.serverAddress}/files") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(fileUUIDs))
        }

        if (!response.status.isSuccess()) {
            Log.e("Service", "Ошибка ответа сервера: ${response.status}")
            Log.e("Service", "Ответ сервера: ${response.bodyAsText()}")
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
