package com.example.deviceinspectionapp

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object FsUtils {

    // Получить URI для файла с использованием FileProvider
    fun getFileUri(context: Context, file: File): Uri? {
        return if (file.exists()) {
            FileProvider.getUriForFile(context, context.packageName, file)
        } else {
            null
        }
    }
}
