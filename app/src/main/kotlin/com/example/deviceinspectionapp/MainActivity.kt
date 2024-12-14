package com.example.deviceinspectionapp

import PoverkaDTO
import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.example.deviceinspectionapp.utils.TestData
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class MainActivity : AppCompatActivity() {

    private var cameraAppPackageName: String? = null
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var photoDirectory: File
    private lateinit var poverkaDTO: PoverkaDTO
    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var cloudIcon: ImageView
    private lateinit var sharedViewModel: SharedViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeComponents()
        setupUI()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.toolbar_menu_action_settings -> {
                startSettingsActivity()
                true
            }
            R.id.action_upload -> {
                uploadPhotos()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initializeComponents() {
        mainService = Service(filesDir)
        setupPermissionLauncher()
        setupPhotoDirectory()
        poverkaDTO = TestData.createTestInspectionData()
        cameraAppPackageName = findCameraApp()
        sharedViewModel = ViewModelProvider(AppViewModelStoreOwner)[SharedViewModel::class.java]
        sharedViewModel.uploadState.observe(this) { state ->
            Log.d("MainActivity", "UploadState изменился: $state")
            updateState(state)
        }

        if (cameraAppPackageName == null) {
            Toast.makeText(this, "Приложение камеры не найдено. Завершаем работу.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupUI() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        progressBar = findViewById(R.id.progressBarHorizontal)
        cloudIcon = findViewById(R.id.cloudIcon)
        val btnStartInspection: Button = findViewById(R.id.btnStartInspection)
        btnStartInspection.setOnClickListener {
            if (checkCameraPermission()) {
                startDeviceCheckActivity()
            } else {
                requestCameraPermission()
            }
        }
    }

    private fun setupPermissionLauncher() {
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val isCameraPermissionGranted = permissions[Manifest.permission.CAMERA] == true
                val message = if (isCameraPermissionGranted) {
                    "Разрешение на камеру предоставлено."
                } else {
                    "Разрешение на камеру не предоставлено."
                }
                Toast.makeText(this, message, if (isCameraPermissionGranted) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
            }
    }

    private fun setupPhotoDirectory() {
        photoDirectory = File(filesDir, "images")
        if (!photoDirectory.exists() && !photoDirectory.mkdirs()) {
            Toast.makeText(this, "Не удалось создать директорию для фото, приложение закрыто.", Toast.LENGTH_LONG).show()
            finish()
        } else {
            Log.d("MainActivity", "Директория для фото: ${photoDirectory.absolutePath}")
        }
    }

    private fun findCameraApp(): String? {
        return packageManager.queryIntentActivities(
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
            PackageManager.MATCH_ALL
        ).let {
            if (it.isNotEmpty()) {
                it[0].activityInfo.packageName
            } else {
                Log.d("MainActivity", "Камера не найдена")
                Toast.makeText(this, "Приложение камера не найдено", Toast.LENGTH_SHORT).show()
                null
            }
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
    }

    private fun startSettingsActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun startDeviceCheckActivity() {
        if (cameraAppPackageName.isNullOrBlank()) {
            Toast.makeText(this, "Ошибка: приложение камеры не найдено.", Toast.LENGTH_LONG).show()
            return
        }

        if (!photoDirectory.exists()) {
            Toast.makeText(this, "Ошибка: директория для фото отсутствует.", Toast.LENGTH_LONG).show()
            return
        }

        val poverkaJson = Json.encodeToString(poverkaDTO)
        val intent = Intent(this, DeviceCheckActivity::class.java).apply {
            putExtra("jsonData", poverkaJson)
            putExtra("cameraAppPackageName", cameraAppPackageName)
            putExtra("photoDirectoryPath", photoDirectory.absolutePath)
        }
        startActivity(intent)
    }

    private fun uploadPhotos() {
        CoroutineScope(Dispatchers.Main).launch {
            mainService.uploadAllPhotos(
                this@MainActivity,
                Json.encodeToString(poverkaDTO),
                photoDirectory,
                ::updateProgress,
                ::updateState
            )
            runOnUiThread {
                if (!mainService.uploadingMessage.isNullOrEmpty()) {
                    Toast.makeText(this@MainActivity, mainService.uploadingMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateProgress(progress: Int) {
        Log.d("ProgressBar", "Обновление прогресса: $progress")

        if (progressBar.visibility == View.GONE) {
            progressBar.visibility = View.VISIBLE
            Log.d("ProgressBar", "Прогресс-бар стал видимым")
        }

        progressBar.progress = progress

        if (progress >= 100) {
            progressBar.visibility = View.GONE
            Log.d("ProgressBar", "Прогресс-бар скрыт, загрузка завершена")
        }
    }

    private fun updateState(state: UploadState) {
        runOnUiThread {
            when (state) {
                UploadState.DEFAULT -> cloudIcon.setImageResource(R.drawable.ic_cloud_default)
                UploadState.UPLOADING -> cloudIcon.setImageResource(R.drawable.ic_cloud_uploading)
                UploadState.SUCCESS -> cloudIcon.setImageResource(R.drawable.ic_cloud_success)
                UploadState.ERROR -> cloudIcon.setImageResource(R.drawable.ic_cloud_error)
                UploadState.PENDING -> cloudIcon.setImageResource(R.drawable.ic_cloud_pending)
            }
        }
    }
}

enum class UploadState {
    DEFAULT,
    PENDING,
    UPLOADING,
    SUCCESS,
    ERROR
}

class SharedViewModel : ViewModel() {
    val uploadState = MutableLiveData<UploadState>()
}

object AppViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}

