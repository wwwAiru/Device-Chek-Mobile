package com.example.deviceinspectionapp.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.deviceinspectionapp.mainService

class UploadWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            mainService.uploadAllPhotos()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}