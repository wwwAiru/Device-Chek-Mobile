package com.example.deviceinspectionapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import com.yalantis.ucrop.UCrop

class PhotoEditorCallResultPassingThrough : ActivityResultContract<PhotoEditorCall, PhotoEditorCall?>() {
    private lateinit var photoEditorCall: PhotoEditorCall

    override fun createIntent(context: Context, input: PhotoEditorCall): Intent {
        photoEditorCall = input
        // Формируем Intent для вызова UCrop
        return UCrop.of(input.fileUri, input.destinationUri)
            .withOptions(UCrop.Options().apply {
                setCompressionQuality(100)
            })
            .getIntent(context)
            .apply {
                putExtra("stageIdx", input.stageIdx)
                putExtra("photoIdx", input.photoIdx)
            }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PhotoEditorCall? {
        // Проверяем, что результат положительный
        if (resultCode == Activity.RESULT_OK && intent != null) {
            // Извлекаем результат обрезки
            val outputUri = UCrop.getOutput(intent)
            if (outputUri != null) {
                // Возвращаем успешно обрезанное изображение
                return photoEditorCall.copy(fileUri = outputUri)
            } else {
                // Логируем ошибку, если URI обрезанного изображения не найден
                Log.e("PhotoEditorResult", "Ошибка: не удалось получить результат обрезки (outputUri пустое).")
            }
        } else if (resultCode == UCrop.RESULT_ERROR && intent != null) {
            // Обработка ошибок, если произошла ошибка обрезки
            val cropError = UCrop.getError(intent)
            if (cropError != null) {
                Log.e("PhotoEditorResult", "Ошибка обрезки:", cropError)
                // Логируем ошибку обрезки для дальнейшего анализа
                cropError.printStackTrace()
            } else {
                Log.e("PhotoEditorResult", "Неизвестная ошибка обрезки.")
            }
        } else {
            // Логируем ошибку, если результат не OK
            Log.e("PhotoEditorResult", "Ошибка при обработке результата обрезки (resultCode = $resultCode).")
        }
        return null
    }
}

data class PhotoEditorCall(
    val fileUri: Uri,
    val destinationUri: Uri,
    val stageIdx: Int,
    val photoIdx: Int
)
