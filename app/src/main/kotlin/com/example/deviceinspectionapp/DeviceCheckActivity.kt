package com.example.deviceinspectionapp

import DeviceCheckViewModel
import PoverkaDTO
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DeviceCheckActivity : AppCompatActivity() {

    private lateinit var stageAdapter: StageManager
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var viewModel: DeviceCheckViewModel

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_check)

        viewModel = ViewModelProvider(this)[DeviceCheckViewModel::class.java]

        val jsonData = intent.getStringExtra("jsonData")
        val poverkaData = Json.decodeFromString<PoverkaDTO>(jsonData!!)
        viewModel.setUuid(poverkaData.uuid)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewStages)
        recyclerView.layoutManager = LinearLayoutManager(this)

        stageAdapter = StageManager(
            context = this,
            stages = poverkaData.stages,
            onCameraIconClicked = { stageCodeName, photoCodeName ->
                viewModel.setStage(stageCodeName)
                viewModel.setPhoto(photoCodeName)
                dispatchTakePictureIntent()
            },
            viewModel = viewModel
        )
        recyclerView.adapter = stageAdapter

        viewModel.thumbnailUris.observe(this) { updatedUris ->
            updatedUris.forEach { (key, uri) ->
                val (stageCodeName, photoCodeName) = key.split("-")
                Log.d("DeviceCheckActivity", "Обновление URI миниатюры: $stageCodeName, $photoCodeName -> $uri")
                stageAdapter.updateThumbnailUri(stageCodeName, photoCodeName, uri)
            }
            // Принудительное уведомление адаптера о том, что данные изменились
            stageAdapter.notifyDataSetChanged()
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val photoUri = viewModel.originalPhotoUri.value
                if (photoUri != null) {
                    val photoBitmap = BitmapFactory.decodeFile(viewModel.originalPhotoFile.value?.absolutePath)
                    if (photoBitmap != null) {
                        val rotatedBitmap = rotateImageIfRequired(photoBitmap, photoUri)
                        savePhotoToStorage(rotatedBitmap)
                    } else {
                        Toast.makeText(this, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(this, "Разрешение на использование камеры не предоставлено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val cameraAppPackageName = packageManager.queryIntentActivities(takePictureIntent, PackageManager.MATCH_ALL).let {
            if (it.isNotEmpty()) {
                it[0].activityInfo.packageName
            } else {
                Log.d("DeviceCheck", "Камера не найдена")
                null
            }
        }
        takePictureIntent.setPackage(cameraAppPackageName)

        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoDir = File(filesDir, "images")
            if (!photoDir.exists()) {
                photoDir.mkdirs()
            }

            val uuid = viewModel.uuid.value
            val currentStageCodeName = viewModel.currentStageCodeName.value
            val currentPhotoCodeName = viewModel.currentPhotoCodeName.value
            val photoIndex = viewModel.currentPhotoIndex.value // Получаем индекс фотографии

            if (currentStageCodeName != null && currentPhotoCodeName != null && photoIndex != null) {
                val photoFile = File(photoDir, "${uuid}_${currentStageCodeName}_${currentPhotoCodeName}_$photoIndex.jpg")
                viewModel.setOriginalPhotoFile(photoFile)

                val photoUri = FileProvider.getUriForFile(this, applicationContext.packageName, photoFile)
                viewModel.setOriginalPhotoUri(photoUri)

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                cameraLauncher.launch(takePictureIntent)
            } else {
                Toast.makeText(this, "Не выбраны стадия или фото", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Камера не найдена", Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePhotoToStorage(photoBitmap: Bitmap) {
        saveOriginalPhoto(photoBitmap)
        saveThumbnail(photoBitmap)
    }



    private fun saveOriginalPhoto(photoBitmap: Bitmap) {
        viewModel.originalPhotoFile.value?.let { file ->
            saveBitmapToFile(photoBitmap, file)
            Toast.makeText(this, "Фото сохранено", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "Ошибка: файл для оригинала не найден", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveThumbnail(photoBitmap: Bitmap) {
        val thumbnail = Bitmap.createScaledBitmap(photoBitmap, 150, 150, true)
        val thumbnailFileName = "thumb_${viewModel.uuid.value}_${viewModel.currentStageCodeName.value}_${viewModel.currentPhotoCodeName.value}_${viewModel.currentPhotoIndex.value}.jpg"
        val thumbnailFile = File(filesDir, thumbnailFileName)

        saveBitmapToFile(thumbnail, thumbnailFile)

        val thumbnailUri = FileProvider.getUriForFile(this, applicationContext.packageName, thumbnailFile)
        viewModel.currentStageCodeName.value?.let { stageCodeName ->
            viewModel.currentPhotoCodeName.value?.let { photoCodeName ->
                viewModel.updatePhotoUri(stageCodeName, photoCodeName, thumbnailUri)
                stageAdapter.updateThumbnailUri(stageCodeName, photoCodeName, thumbnailUri)
                Log.d("DeviceCheckActivity", "Сохранение миниатюры: $thumbnailFileName -> $thumbnailUri")
            }
        }
    }


    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Ошибка сохранения фото: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("DeviceCheck", "Error saving photo: ${e.message}", e)
        }
    }

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

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
