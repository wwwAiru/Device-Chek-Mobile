package com.example.deviceinspectionapp

import android.Manifest
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
import androidx.work.WorkManager
import com.example.deviceinspectionapp.utils.DeviceCheckUtil
import com.example.deviceinspectionapp.workers.UploadWorker
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import java.util.concurrent.TimeUnit
import androidx.work.PeriodicWorkRequestBuilder

class MainActivity : AppCompatActivity() {

    private var cameraAppPackageName: String? = null
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var photoDirectory: File
    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var cloudIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("", "Main.onCreate")
        setContentView(R.layout.activity_main)

        initializeComponents()
        setupUI()
        //шедулер 15 минут
        schedulePeriodicUpload(15)
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
                mainService.uploadAllPhotos()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initializeComponents() {
        mainService = Service(::updateFilesSyncState, filesDir, this)
        Log.d("","mainService = Service(::updateUploadingState, filesDir, this)")
        setupPermissionLauncher()
        setupPhotoDirectory()
        cameraAppPackageName = findCameraApp()

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
        cloudIcon.setImageResource(R.drawable.ic_cloud_default)
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
        photoDirectory = File(filesDir, "checks")
        if (!photoDirectory.exists() && !photoDirectory.mkdirs()) {
            Toast.makeText(this, "Не удалось создать директорию для фото, приложение закрыто.", Toast.LENGTH_LONG).show()
            finish()
        } else {
            Log.d("MainActivity", "Директория для фото: ${photoDirectory.absolutePath}")
        }
    }

    private fun schedulePeriodicUpload(timeMinutes: Long) {
        val uploadRequest = PeriodicWorkRequestBuilder<UploadWorker>(timeMinutes, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueue(uploadRequest)
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

        val poverkaJson = DeviceCheckUtil.createCheckDataJson(this)
        val intent = Intent(this, DeviceCheckActivity::class.java).apply {
            putExtra("jsonData", poverkaJson)
            putExtra("cameraAppPackageName", cameraAppPackageName)
            putExtra("photoDirectoryPath", photoDirectory.absolutePath)
        }
        startActivity(intent)
    }

    private fun updateFilesSyncState() {
        runOnUiThread {
            Log.d("updateFilesSyncState", "updateFilesSyncState started")
            Log.d("updateFilesSyncState", "state:" +
                    " isUploadingRunning ${mainService.isUploadingRunning()} ${mainService.progress}%\n" +
                    " hasUploadingError ${mainService.hasUploadingError()}\n" +
                    " hasFilesToUpload ${mainService.hasFilesToUpload}")
            if (mainService.isUploadingRunning()) {
                progressBar.visibility = View.VISIBLE
                progressBar.progress = mainService.progress
                cloudIcon.setImageResource(R.drawable.ic_cloud_uploading)
                return@runOnUiThread
            } else { progressBar.visibility = View.GONE }

            if (mainService.hasUploadingError()) {
                cloudIcon.setImageResource(R.drawable.ic_cloud_error)
                return@runOnUiThread
            }

            if (mainService.hasFilesToUpload) {
                cloudIcon.setImageResource(R.drawable.ic_cloud_pending)
                return@runOnUiThread
            } else {
                cloudIcon.setImageResource(R.drawable.ic_cloud_success)
                return@runOnUiThread
            }
        }
    }

}

