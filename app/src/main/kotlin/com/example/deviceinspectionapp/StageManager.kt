package com.example.deviceinspectionapp

import PhotoDTO
import StageDTO
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class StageManager(
    private val context: Context,
    private val stages: List<StageDTO>,
    private val onCameraIconClicked: (stageCodeName: String, photoCodeName: String) -> Unit
) : RecyclerView.Adapter<StageManager.StageViewHolder>() {

    private val stageList = stages.toMutableList()

    // Создание ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stage, parent, false)
        return StageViewHolder(view)
    }

    // Привязка данных
    override fun onBindViewHolder(holder: StageViewHolder, position: Int) {
        val stage = stageList[position]
        holder.bind(stage)
    }

    // Количество элементов в списке
    override fun getItemCount(): Int = stageList.size

    // Обновление миниатюры фотографии
    fun updateThumbnailUri(stageCodeName: String, photoCodeName: String, thumbnailUri: Uri) {
        // Ищем нужную стадию и фотографию
        val stage = stageList.find { it.stageCodeName == stageCodeName }
        val photo = stage?.photos?.find { it.photoCodeName == photoCodeName }

        // Если фотография найдена, обновляем ее URI
        photo?.let {
            it.thumbnailUri = thumbnailUri
            // Обновляем только соответствующую фотографию в ViewHolder
            notifyItemChanged(stageList.indexOf(stage))
        }
    }

    // ViewHolder для этапа
    inner class StageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvStageName: TextView = view.findViewById(R.id.tvStageName)
        private val gridLayout: ViewGroup = view.findViewById(R.id.gridLayoutPhotos)

        // Привязка данных этапа к UI
        // Привязка данных этапа к UI
        fun bind(stage: StageDTO) {
            tvStageName.text = stage.caption
            gridLayout.removeAllViews()

            for (photo in stage.photos) {
                val photoView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_photo, gridLayout, false)

                val ivPhoto = photoView.findViewById<ImageView>(R.id.ivPhoto)
                ivPhoto.setImageResource(R.drawable.ic_camera) // Иконка камеры по умолчанию

                // Используем thumbnailUri, если он установлен
                val imageUri = photo.thumbnailUri ?: run {
                    // Если thumbnailUri отсутствует, проверяем файлы в локальном хранилище
                    val photoFile = File(context.filesDir, "images/${photo.imageFileName}")
                    val thumbnailFile = File(context.filesDir, "thumb_${photo.imageFileName}")

                    Log.d("StageManager", "Photo file: ${photoFile.absolutePath}")
                    Log.d("StageManager", "Thumbnail file: ${thumbnailFile.absolutePath}")

                    when {
                        photoFile.exists() -> FileProvider.getUriForFile(context, "${context.packageName}", photoFile)
                        thumbnailFile.exists() -> FileProvider.getUriForFile(context, "${context.packageName}", thumbnailFile)
                        else -> null
                    }
                }

                // Устанавливаем изображение, если найден URI
                if (imageUri != null) {
                    ivPhoto.setImageURI(imageUri)
                    Log.d("StageManager", "Setting photo with URI")
                } else {
                    // Иконка камеры, если ни фото, ни миниатюры нет
                    ivPhoto.setImageResource(R.drawable.ic_camera)
                    Log.d("StageManager", "Setting camera icon")
                }

                // Обработчик клика по фото
                ivPhoto.setOnClickListener {
                    onCameraIconClicked(stage.stageCodeName, photo.photoCodeName)
                }

                gridLayout.addView(photoView)
            }
        }

    }
}
