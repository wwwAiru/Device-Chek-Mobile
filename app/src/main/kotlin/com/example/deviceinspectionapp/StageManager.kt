package com.example.deviceinspectionapp

import StageDTO
import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

/**
 * Менеджер для отображения этапов и их фотографий в RecyclerView.
 * @param context Контекст приложения для доступа к ресурсам.
 * @param stages Список этапов для отображения.
 * @param onCameraIconClicked Лямбда-функция, вызываемая при нажатии на иконку камеры.
 */
class StageManager(
    private val context: Context,
    private val stages: List<StageDTO>,
    private val onCameraIconClicked: (stageCodeName: String, photoCodeName: String) -> Unit
) : RecyclerView.Adapter<StageManager.StageViewHolder>() {

    /**
     * Создаёт новый экземпляр ViewHolder для элемента списка.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stage, parent, false)
        return StageViewHolder(view)
    }

    /**
     * Привязывает данные к ViewHolder.
     */
    override fun onBindViewHolder(holder: StageViewHolder, position: Int) {
        val stage = stages[position]
        holder.bind(stage)
    }

    /**
     * Возвращает общее количество этапов.
     */
    override fun getItemCount(): Int = stages.size

    /**
     * ViewHolder для отображения одного этапа.
     */
    inner class StageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvStageName: TextView = view.findViewById(R.id.tvStageName)
        private val gridLayout: ViewGroup = view.findViewById(R.id.gridLayoutPhotos)

        /**
         * Привязывает данные этапа к представлениям.
         * @param stage Этап, данные которого нужно отобразить.
         */
        fun bind(stage: StageDTO) {
            tvStageName.text = stage.caption
            gridLayout.removeAllViews()

            for (photo in stage.photos) {
                val photoView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_photo, gridLayout, false)

                val ivPhoto = photoView.findViewById<ImageView>(R.id.ivPhoto)
                ivPhoto.setImageResource(R.drawable.ic_camera) // Иконка камеры по умолчанию

                // Путь к файлу изображения в локальном хранилище
                val photoFile = File(context.filesDir, "photos/${photo.imageFileName}")
                if (photoFile.exists()) {
                    // Если фото существует, загружаем его миниатюру
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    ivPhoto.setImageBitmap(bitmap)
                }

                // Устанавливаем слушатель для обработки нажатия на значок камеры
                ivPhoto.setOnClickListener {
                    onCameraIconClicked(stage.stageCodeName, photo.photoCodeName)
                }

                gridLayout.addView(photoView)
            }
        }
    }
}
