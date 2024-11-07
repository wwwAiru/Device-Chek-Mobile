package com.example.deviceinspectionapp

import StageDTO
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

/**
 * Менеджер для отображения этапов и их фотографий в RecyclerView
 * @param stages Список этапов для отображения
 * @param onCameraIconClicked Лямбда-функция, вызываемая при нажатии на иконку камеры
 */
class StageManager(
    private val stages: List<StageDTO>,
    private val onCameraIconClicked: (stageCodeName: String, photoCodeName: String) -> Unit
) : RecyclerView.Adapter<StageManager.StageViewHolder>() {

    /**
     * Создаёт новый экземпляр ViewHolder для элемента списка
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StageViewHolder {
        // Раздуваем макет элемента списка из ресурса
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stage, parent, false)
        return StageViewHolder(view)
    }

    /**
     * Привязывает данные к ViewHolder
     */
    override fun onBindViewHolder(holder: StageViewHolder, position: Int) {
        // Получаем текущий этап по позиции и связываем его с ViewHolder
        val stage = stages[position]
        holder.bind(stage)
    }

    /**
     * Возвращает общее количество этапов
     */
    override fun getItemCount(): Int = stages.size

    /**
     * ViewHolder для отображения одного этапа
     */
    inner class StageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Инициализация представлений для названия этапа и контейнера для фотографий
        private val tvStageName: TextView = view.findViewById(R.id.tvStageName)
        private val gridLayout: ViewGroup = view.findViewById(R.id.gridLayoutPhotos)

        /**
         * Привязывает данные этапа к представлениям
         * @param stage Этап, данные которого нужно отобразить
         */
        fun bind(stage: StageDTO) {
            // Установка названия этапа
            tvStageName.text = stage.caption
            // Очищаем контейнер для фотографий перед добавлением новых
            gridLayout.removeAllViews()

            // Проходим по всем фотографиям текущего этапа
            for (photo in stage.photos) {
                // Создание нового представления для фотографии
                val photoView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_photo, gridLayout, false)

                // Инициализация ImageView для отображения фотографии
                val ivPhoto = photoView.findViewById<ImageView>(R.id.ivPhoto)
                ivPhoto.setImageResource(R.drawable.ic_camera) // Установите иконку камеры как начальное изображение

                // Установка изображения, если фото уже сделано
                if (photo.imageFileName.isNotEmpty()) {
                    val imageFile = File(photo.imageFileName)
                    // Проверяем, существует ли файл изображения
                    if (imageFile.exists()) {
                        val imageUri = Uri.fromFile(imageFile) // Создаём URI из файла
                        ivPhoto.setImageURI(imageUri) // Устанавливаем изображение в ImageView
                    }
                }

                // Устанавливаем слушатель для обработки нажатия на значок камеры
                ivPhoto.setOnClickListener {
                    // Вызываем метод для обработки нажатия на значок камеры
                    onCameraIconClicked(stage.stageCodeName, photo.photoCodeName)
                }

                // Добавляем представление фотографии в контейнер
                gridLayout.addView(photoView)
            }
        }
    }
}
