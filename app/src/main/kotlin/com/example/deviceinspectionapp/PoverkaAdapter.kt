package com.example.deviceinspectionapp

import PoverkaDTO
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream

/**
 * https://guides.codepath.com/android/using-the-recyclerview
 */

class PoverkaAdapter(
    private val context: DeviceCheckActivity,
    private val poverkaDTO: PoverkaDTO
) : RecyclerView.Adapter<StageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StageViewHolder {
        // Создаем вью для каждого этапа
        val stageView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stage, parent, false) as LinearLayout
        return StageViewHolder(
            context,
            stageView,
        )
    }

    override fun onBindViewHolder(holder: StageViewHolder, position: Int) {
        holder.bind(position, poverkaDTO.stages[position])
    }

    override fun onBindViewHolder(holder: StageViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val payload = payloads.last() as Pair<*,*>
            holder.updateThumbnail(payload.first as Int)
        } else {
            // Полное обновление, если `payload` пуст
            holder.bind(position, poverkaDTO.stages[position])
        }
    }


    override fun getItemCount(): Int {
        return poverkaDTO.stages.size
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun processPhotoEditEvent(stageIdx: Int, photoIdx: Int, editedPhotoUri: Uri) {
        val editedFileName = editedPhotoUri.path!!.substringAfterLast('/')
        val editedPhotoFile = File(context.photoDirectory, editedFileName)
            // Создаем миниатюру из редактированного фото
            val thumbnailBitmap = BitmapUtils.createThumbnailFromFile(editedPhotoFile, context.contentResolver)
            // Формируем имя миниатюры
            val thumbFile = File(context.photoDirectory, "thumb_$editedFileName")
            Log.d("creating thumb", "thumb_$editedFileName")
            FileOutputStream(thumbFile).use { out ->
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            // Уведомляем адаптер об изменениях конкретного элемента
            notifyItemChanged(stageIdx, Pair(photoIdx, "update_photo"))
    }


    @RequiresApi(Build.VERSION_CODES.N)
    fun processPhotoTakenEvent(call: CameraCall) {
        val fileName = call.fileUri.path!!.substringAfterLast('/')
        val photoFile = File(context.photoDirectory, fileName)
        val thumbnailBitmap = BitmapUtils.createThumbnailFromFile(photoFile, context.contentResolver)
        val thumbFile = File(context.photoDirectory, "thumb_$fileName")
        Log.d("creating thumb", "thumb_$fileName")
        FileOutputStream(thumbFile).use { out ->
            thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        notifyItemChanged(call.stageIdx, Pair(call.photoIdx, "update_photo"))
    }
}
