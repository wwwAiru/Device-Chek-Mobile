package com.example.deviceinspectionapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.yalantis.ucrop.UCrop
import java.io.File

/**
 *  https://github.com/Yalantis/uCrop
 * */
class PhotoEditActivity : AppCompatActivity() {
    private lateinit var photoUri: Uri
    private var stageIdx: Int = -1
    private var photoIdx: Int = -1

    private lateinit var uCropLauncher: ActivityResultLauncher<Intent>

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация uCropLauncher
        setupUCropLauncher()

        // Получаем данные из Intent
        intent.getParcelableExtra("photoUri", Uri::class.java)?.let {
            photoUri = it
        } ?: run {
            Log.e("PhotoEditActivity", "Не удалось получить URI изображения")
            finish()
            return
        }

        stageIdx = intent.getIntExtra("stageIdx", -1)
        photoIdx = intent.getIntExtra("photoIdx", -1)

        if (stageIdx == -1 || photoIdx == -1) {
            Log.e("PhotoEditActivity", "Недостаточно данных для редактирования")
            finish()
            return
        }

        // Настройка uCrop
        startUCrop(photoUri)
    }


    private fun setupUCropLauncher() {
        uCropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val resultUri = data?.let { UCrop.getOutput(it) }
                if (resultUri != null) {
                    handleCropResult(resultUri)
                } else {
                    Log.e("PhotoEditActivity", "URI результата пустой")
                    setResult(RESULT_CANCELED)
                    finish()
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Log.e("PhotoEditActivity", "Ошибка редактирования: $cropError")
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun startUCrop(uri: Uri) {
        val destinationFile = File(uri.path!!) // Используем тот же файл для сохранения
        val destinationUri = Uri.fromFile(destinationFile)

        val uCropIntent = UCrop.of(uri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1000, 1000) // Задаем максимальные размеры
            .getIntent(this)

        uCropLauncher.launch(uCropIntent)
    }

    private fun handleCropResult(resultUri: Uri) {
        val resultFile = File(resultUri.path!!)

        if (resultFile.exists()) {
            // Возвращаем результат в вызывающую активность
            val resultIntent = Intent().apply {
                putExtra("photoUri", resultUri)
                putExtra("stageIdx", stageIdx)
                putExtra("photoIdx", photoIdx)
            }
            setResult(RESULT_OK, resultIntent)
        } else {
            Log.e("PhotoEditActivity", "Файл результата не существует: ${resultFile.absolutePath}")
            setResult(RESULT_CANCELED)
            finish()
        }
        finish()
    }
}
