package com.example.deviceinspectionapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

lateinit var mainService: Service

class Service(private val filesDir: File) {

    var settings: Settings = Settings("127.0.0.1:8080", "user")
        set(v) {
            saveSettings(v)
            field = v
        }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }
    val mutex = Mutex()
    var uploadingError: String? = null
    var hasFilesToUpload: Boolean = false

    //mutex locked, uploadingError = null
    //mutex locked, uploadingError != null
    //mutex not locked, uploadingError = null
    //mutex not locked, uploadingError != null

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
            // Просто сохраняем текущие настройки (по умолчанию они уже установлены)
            saveSettings(settings)
        }
    }

    // Сохранение настроек
    private fun saveSettings(newSettings: Settings) {
        val settingsFile = File(filesDir, "settings/settings.json")
        settingsFile.parentFile!!.mkdirs() // Уверены, что parentFile не null
        val jsonString = Json.encodeToString(newSettings)
        settingsFile.writeText(jsonString)
        Log.i("Settings", "Настройки сохранены: $jsonString")
    }

    // Метод для выгрузки фото на сервер
    private suspend fun uploadPhoto(file: File): HttpResponse {
        Log.i("Upload", "Uploading file: ${file.name}")
        val response: HttpResponse = client.post("http://${settings.serverAddress}/upload") {
            setBody(MultiPartFormDataContent(
                formData {
                    append("file", file.readBytes(), Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                    })
                }
            ))
        }
        Log.i("UploadPhotoRes", "ответ сервера ${file.name}: ${response.status}")
        return response
    }

    /**
     * invoked by timer or manually
     */
    fun uploadAllPhotos(context: Context, photoDirectory: File, updateUI: () -> Unit) {
        if (!isNetworkAvailable(context)) {
            Log.e("Service", "Нет подключения к интернету.")
            uploadingError = "Нет подключения к интернету."
            updateUI()
            return
        }

        try {
            CoroutineScope(Dispatchers.IO).launch {
                if (!mutex.tryLock()) {
                    return@launch
                }

                try {
                    uploadingError = null
                    //TODO: find files to upload (1 - collect local files, 2 - match with server files (taking into account date_created))
                    var photos : List<File> = emptyList() //TODO
                    //TODO hasFilesToUpload
                    updateUI()
                    while (!photos.isEmpty()) {
//                        val photos = photoDirectory.listFiles() ?: emptyArray()
//                        if (photos.isEmpty()) {
//                            Log.i("Service", "Нет фотографий для загрузки.")
//                            return@launch
//                        }

                        photos.forEach { photo ->
                            val response = uploadPhoto(photo)
                            if (response.status.isSuccess()) {
                                Log.i("Service", "Фото ${photo.name} успешно загружено.")
                            } else {
                                Log.e(
                                    "Service",
                                    "${response.status.description} : Ошибка при загрузке фото ${photo.name}: ${response.status}"
                                )
                                throw RuntimeException("${response.status.description} : Ошибка при загрузке фото ${photo.name}: ${response.status}")
                            }
                        }
                        //TODO: find files to upload (1 - collect local files, 2 - match with server files)
                        photos = emptyList() //TODO
                    }
                    hasFilesToUpload = false
                } catch (e: Exception) {
                    uploadingError = e.message
                } finally {
                    mutex.unlock()
                    updateUI()
                }
            }
        } catch (e: Exception) {
            uploadingError = e.message
        }
        updateUI()
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}

// Класс для настроек
@Serializable
data class Settings(
    val serverAddress: String? = null,
    val login: String? = null
)
