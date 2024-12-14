package com.example.deviceinspectionapp

import PoverkaDTO
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceinspectionapp.utils.ExifUtils
import java.io.File
import java.io.FileOutputStream

/**
 * https://guides.codepath.com/android/using-the-recyclerview
 */

class PoverkaAdapter(
    private val context: DeviceCheckActivity,
    private val poverkaDTO: PoverkaDTO,
    private val viewModel: SharedViewModel
) : RecyclerView.Adapter<StageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StageViewHolder {
        // Создаем вью для каждого этапа
        val stageView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stage, parent, false) as LinearLayout
        return StageViewHolder(
            context,
            stageView
        )
    }

    override fun onBindViewHolder(holder: StageViewHolder, position: Int) {
        Log.d("onBindViewHolder", "onBindViewHolder(holder: StageViewHolder, position: Int)")
//        holder.bind(position, poverkaDTO.stages[position])
    }

    override fun onBindViewHolder(
        holder: StageViewHolder,
        stageIdx: Int,
        payloads: MutableList<Any>
    ) {
        // Если есть payloads, обрабатываем их
        if (payloads.isNotEmpty()) {
            payloads
                .filterIsInstance<UpdateEvent.PhotoUpdate>()
                .map {
                    it.photoIdx
                }
                .toSet()
                .forEach {
                    Log.d("onBindViewHolder_2", "stage $stageIdx обновление фото: $it")
                    holder.updateThumbnail(it)
                }
        } else {
            // Полное обновление стадии, если payloads пуст
            Log.d("onBindViewHolder_2", "Полное обновление стадии $stageIdx, payloads: $payloads")
            holder.bind(stageIdx, poverkaDTO.stages[stageIdx])
        }
    }




    override fun getItemCount(): Int {
        return poverkaDTO.stages.size
    }

    fun processPhotoEditEvent(stageIdx: Int, photoIdx: Int, editedPhotoUri: Uri) {
        val editedFileName = editedPhotoUri.path!!.substringAfterLast('/')
        val editedPhotoFile = File(context.photoDirectory, editedFileName)
        // Создаем миниатюру из редактированного фото
        val thumbnailBitmap =
            BitmapUtils.createThumbnailFromFile(editedPhotoFile, context.contentResolver)
        // Формируем имя миниатюры
        val thumbFile = File(context.photoDirectory, "thumb_$editedFileName")
        Log.d("creating thumb", "thumb_$editedFileName")
        FileOutputStream(thumbFile).use { out ->
            thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }

        ExifUtils.updateExifTimestamp(editedPhotoFile)
        val exifDataAfterEdit = ExifUtils.readExifData(editedPhotoFile)
        ExifUtils.logExifData("EXIF AFTER", exifDataAfterEdit)
        // Уведомляем адаптер об изменениях конкретного элемента
        notifyItemChanged(stageIdx, UpdateEvent.PhotoUpdate(photoIdx))
        // обновление статуса в mainActivity
        viewModel.uploadState.value = UploadState.PENDING
    }


    fun processPhotoTakenEvent(call: CameraCall) {
        val fileName = call.fileUri.path!!.substringAfterLast('/')
        val photoFile = File(context.photoDirectory, fileName)
        val thumbnailBitmap =
            BitmapUtils.createThumbnailFromFile(photoFile, context.contentResolver)
        val thumbFile = File(context.photoDirectory, "thumb_$fileName")
        Log.d("creating thumb", "thumb_$fileName")
        FileOutputStream(thumbFile).use { out ->
            thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        notifyItemChanged(call.stageIdx, UpdateEvent.PhotoUpdate(call.photoIdx))
        // обновление статуса в mainActivity
        viewModel.uploadState.value = UploadState.PENDING
    }
}

sealed class UpdateEvent {
    data class PhotoUpdate(val photoIdx: Int) : UpdateEvent()
}
