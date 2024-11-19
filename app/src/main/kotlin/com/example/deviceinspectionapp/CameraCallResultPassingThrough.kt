package com.example.deviceinspectionapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract

class CameraCallResultPassingThrough : ActivityResultContract<CameraCall, CameraCall?>() {
    private lateinit var cameraCall: CameraCall
    override fun createIntent(context: Context, input: CameraCall) : Intent {
        cameraCall = input
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, input.fileUri)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): CameraCall? = when {
        resultCode != Activity.RESULT_OK -> null
        else -> cameraCall
    }
}

data class CameraCall(val fileUri: Uri, val stageIdx: Int, val photoIdx: Int)