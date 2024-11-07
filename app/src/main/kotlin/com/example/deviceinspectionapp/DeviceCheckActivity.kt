package com.example.deviceinspectionapp

import PoverkaDTO
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceinspectionapp.R
import com.example.deviceinspectionapp.StageManager
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DeviceCheckActivity : AppCompatActivity() {
    private lateinit var stageAdapter: StageManager
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var uuid: String
    private var currentStageCodeName: String? = null
    private var currentPhotoCodeName: String? = null
    private var originalPhotoUri: Uri? = null
    private var originalPhotoFile: File? = null

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_check)

        val jsonData = intent.getStringExtra("jsonData")
        val poverkaData = Json.decodeFromString<PoverkaDTO>(jsonData!!)
        uuid = poverkaData.uuid

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewStages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        stageAdapter = StageManager(this, poverkaData.stages) { stageCodeName, photoCodeName ->
            currentStageCodeName = stageCodeName
            currentPhotoCodeName = photoCodeName
            onCameraIconClicked()
        }
        recyclerView.adapter = stageAdapter

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && originalPhotoUri != null) {
                val photoBitmap = BitmapFactory.decodeFile(originalPhotoFile?.absolutePath)
                if (photoBitmap != null) {
                    val rotatedBitmap = rotateImageIfRequired(photoBitmap, originalPhotoUri!!)
                    savePhotoToStorage(rotatedBitmap)
                } else {
                    Toast.makeText(this, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show()
                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "Разрешение на использование камеры не предоставлено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onCameraIconClicked() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        // Создаём файл для сохранения фото
        val fileName = "${uuid}_${currentStageCodeName}_${currentPhotoCodeName}.jpg"
        originalPhotoFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)

        // Логируем путь файла для отладки
        Log.d("DeviceCheck", "File path: ${originalPhotoFile?.absolutePath}")
        Log.d("DeviceCheck", "File exists: ${originalPhotoFile?.exists()}")

        // Проверка существования файла
        if (!originalPhotoFile?.exists()!!) {
            originalPhotoFile?.parentFile?.mkdirs() // Создание директории, если она не существует
        }

        originalPhotoUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            originalPhotoFile!!
        )

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, originalPhotoUri)
        cameraLauncher.launch(cameraIntent)
    }

    private fun savePhotoToStorage(photoBitmap: Bitmap) {
        saveOriginalPhoto(photoBitmap)
        saveThumbnail(photoBitmap)
    }

    private fun saveOriginalPhoto(photoBitmap: Bitmap) {
        originalPhotoFile?.let { file ->
            saveBitmapToFile(photoBitmap, file)
            Toast.makeText(this, "Фото сохранено", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "Ошибка: файл для оригинала не найден", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveThumbnail(photoBitmap: Bitmap) {
        val thumbnail = Bitmap.createScaledBitmap(photoBitmap, 150, 150, true)
        val thumbnailFileName = "thumb_${uuid}_${currentStageCodeName}_${currentPhotoCodeName}.jpg"
        val thumbnailFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), thumbnailFileName)
        saveBitmapToFile(thumbnail, thumbnailFile)

        val ivPhoto = findViewById<ImageView>(R.id.ivPhoto)
        ivPhoto.isVisible = true
        ivPhoto.setImageBitmap(thumbnail)
    }

    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Ошибка сохранения фото", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Проверяет и поворачивает изображение при необходимости.
     *
     * @param bitmap Bitmap изображения для проверки и поворота.
     * @param photoUri Uri фото для получения метаданных.
     * @return Повернутый Bitmap, если требуется, иначе оригинальный.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun rotateImageIfRequired(bitmap: Bitmap, photoUri: Uri): Bitmap {
        val inputStream = contentResolver.openInputStream(photoUri)
        val exif = inputStream?.let { ExifInterface(it) }
        val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            else -> bitmap
        }
    }

    /**
     * Поворачивает Bitmap на указанный угол.
     *
     * @param bitmap Bitmap изображения для поворота.
     * @param degrees Угол поворота.
     * @return Повернутый Bitmap.
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
