package com.example.deviceinspectionapp.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.deviceinspectionapp.mainService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            withContext(Dispatchers.IO) {
                mainService.uploadAllPhotos()
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}