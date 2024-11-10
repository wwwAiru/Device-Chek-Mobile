package com.example.deviceinspectionapp

import DeviceCheckViewModel
import StageDTO
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class StageManager(
    private val context: Context,
    private val stages: List<StageDTO>,
    private val onCameraIconClicked: (stageCodeName: String, photoCodeName: String) -> Unit,
    private val viewModel: DeviceCheckViewModel
) : RecyclerView.Adapter<StageManager.StageViewHolder>() {

    // Метод для обновления миниатюры для конкретной фотографии
    fun updateThumbnailUri(stageCodeName: String, photoCodeName: String, uri: Uri) {
        val stageIndex = stages.indexOfFirst { it.stageCodeName == stageCodeName }
        if (stageIndex == -1) return

        val stage = stages[stageIndex]
        val photoIndex = stage.photos.indexOfFirst { it.photoCodeName == photoCodeName }
        if (photoIndex == -1) return

        // Обновляем URI миниатюры
        stage.photos[photoIndex].thumbnailUri = uri

        // Обновляем данные на экране
        notifyDataSetChanged()  // Обновляем весь список, чтобы перерисовать все элементы
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stage, parent, false)
        return StageViewHolder(view)
    }

    override fun getItemCount(): Int = stages.size

    override fun onBindViewHolder(holder: StageViewHolder, position: Int) {
        val stage = stages[position]
        holder.bind(stage)
    }

    inner class StageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvStageName: TextView = view.findViewById(R.id.tvStageName)
        private val gridLayout: GridLayout = view.findViewById(R.id.gridLayoutPhotos)

        fun bind(stage: StageDTO) {
            tvStageName.text = stage.caption
            gridLayout.removeAllViews()

            for ((photoIndex, photo) in stage.photos.withIndex()) {
                val photoView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_photo, gridLayout, false)
                val ivPhoto = photoView.findViewById<ImageView>(R.id.ivPhoto)

                // Получаем URI миниатюры или основного изображения
                val imageUri = photo.thumbnailUri ?: run {
                    val photoFile = File(context.filesDir, "images/${photo.imageFileName}")
                    val thumbnailFile = File(context.filesDir, "thumb_${photo.imageFileName}")
                    when {
                        thumbnailFile.exists() -> FileProvider.getUriForFile(context, context.packageName, thumbnailFile)
                        photoFile.exists() -> FileProvider.getUriForFile(context, context.packageName, photoFile)
                        else -> null
                    }
                }

                // Устанавливаем изображение, если URI найден
                if (imageUri != null) {
                    ivPhoto.setImageURI(imageUri)
                } else {
                    ivPhoto.setImageResource(R.drawable.ic_camera)
                }

                ivPhoto.tag = photoIndex  // Присваиваем индекс фото для уникальности
                ivPhoto.setOnClickListener {
                    viewModel.setStage(stage.stageCodeName)
                    viewModel.setPhoto(photo.photoCodeName)
                    viewModel.setPhotoIndex(photoIndex)
                    onCameraIconClicked(stage.stageCodeName, photo.photoCodeName)
                }

                gridLayout.addView(photoView)
            }
        }

    }
}
