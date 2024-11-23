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
        if (input.fileUri.toString().isBlank() || input.destinationUri.toString().isBlank()) {
            throw IllegalArgumentException(
                "Необходимо передать корректные URI для обрезки изображения"
            )
        }

        if (input.stageIdx < 0 || input.photoIdx < 0) {
            throw IllegalArgumentException(
                "Индексы stageIdx и photoIdx должны быть положительными"
            )
        }

        photoEditorCall = input
        // Формируем Intent для вызова UCrop
        return UCrop.of(input.fileUri, input.destinationUri)
            .withOptions(UCrop.Options().apply {
                setCompressionQuality(100)
                setFreeStyleCropEnabled(true)
            })
            .getIntent(context)
            .apply {
                putExtra("stageIdx", input.stageIdx)
                putExtra("photoIdx", input.photoIdx)
            }
    }


    override fun parseResult(resultCode: Int, intent: Intent?): PhotoEditorCall? {
        return when {
            resultCode == Activity.RESULT_OK && intent != null -> {
                val outputUri = UCrop.getOutput(intent)
                if (outputUri != null) {
                    photoEditorCall.copy(fileUri = outputUri)
                } else {
                    Log.e("UCrop", "Обрезка завершилась без результата (outputUri == null)")
                    null
                }
            }
            resultCode == UCrop.RESULT_ERROR && intent != null -> {
                val cropError = UCrop.getError(intent)
                Log.e("UCrop", "Ошибка обрезки: ${cropError?.localizedMessage}")
                throw IllegalStateException("Ошибка редактирования: ${cropError?.localizedMessage}").apply {
                    cropError?.let { initCause(it) }
                }
            }
            else -> {
                Log.e("UCrop", "Операция обрезки была отменена или завершилась с некорректным статусом (resultCode = $resultCode)")
                null
            }
        }
    }


}

data class PhotoEditorCall(
    val fileUri: Uri,
    val destinationUri: Uri,
    val stageIdx: Int,
    val photoIdx: Int
)
