import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceinspectionapp.BitmapUtils
import com.example.deviceinspectionapp.FsUtils
import com.example.deviceinspectionapp.R
import java.io.File

class PoverkaAdapter(
    private val context: Context,
    private val poverkaDTO: PoverkaDTO,
    private val onCameraIconClicked: (stageCodeName: Int, photoCodeName: Int) -> Unit
) : RecyclerView.Adapter<PoverkaAdapter.PhotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        // Находим нужную стадию и фото внутри стадии по позиции
        var currentPosition = position
        for (stage in poverkaDTO.stages) {
            if (currentPosition < stage.photos.size) {
                val photo = stage.photos[currentPosition]
                holder.bind(photo, stage.stageCodeName)
                break
            }
            currentPosition -= stage.photos.size
        }
    }

    override fun getItemCount(): Int {
        return poverkaDTO.stages.sumOf { it.photos.size } // Общее количество фотографий во всех стадиях
    }

    // Метод для обновления миниатюры фотографии по позиции
    fun updatePhotoThumbnail(stagePosition: Int, photoPosition: Int, thumbnailBitmap: Bitmap) {
        val itemPosition = getItemPosition(stagePosition, photoPosition)
        if (itemPosition != -1) {
            notifyItemChanged(itemPosition)
        }
    }


    // Получение позиции элемента по stagePosition и photoPosition
    private fun getItemPosition(stagePosition: Int, photoPosition: Int): Int {
        var currentPosition = 0
        for (stage in poverkaDTO.stages) {
            if (currentPosition + stage.photos.size > stagePosition) {
                return currentPosition + photoPosition
            }
            currentPosition += stage.photos.size
        }
        return -1 // В случае ошибки
    }

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoImageView: ImageView = itemView.findViewById(R.id.ivPhoto)

        // В методе bind добавляем проверку на uri
        fun bind(photoDTO: PhotoDTO, stageCodeName: String) {
            // Проверяем, есть ли URI
            if (!photoDTO.uri.isNullOrEmpty()) {
                val photoFile = File(context.filesDir, photoDTO.imageFileName)
                if (photoFile.exists()) { // Проверяем, существует ли файл
                    val thumbnailBitmap = BitmapUtils.createThumbnailFromFile(photoFile)
                    photoImageView.setImageBitmap(thumbnailBitmap)
                } else {
                    // Если файл не найден, установить иконку камеры
                    setCameraIcon()
                }
            } else {
                // Если uri отсутствует, установить иконку камеры
                setCameraIcon()
            }
        }

        private fun setCameraIcon() {
            photoImageView.setImageResource(R.drawable.ic_camera)
            photoImageView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val stagePosition = getStagePosition(position)
                    val photoPosition = getPhotoPosition(position)
                    if (stagePosition != -1 && photoPosition != -1) { // Проверка корректности индексов
                        onCameraIconClicked(stagePosition, photoPosition)
                    }
                }
            }
        }


        // Получение позиции элемента по stagePosition и photoPosition с проверкой индексов
        private fun getItemPosition(stagePosition: Int, photoPosition: Int): Int {
            if (stagePosition !in poverkaDTO.stages.indices) {
                return -1 // В случае ошибки возвращаем -1
            }
            val stage = poverkaDTO.stages[stagePosition]
            if (photoPosition !in stage.photos.indices) {
                return -1 // В случае ошибки возвращаем -1
            }

            var currentPosition = 0
            for ((index, s) in poverkaDTO.stages.withIndex()) {
                if (index == stagePosition) {
                    return currentPosition + photoPosition
                }
                currentPosition += s.photos.size
            }
            return -1 // В случае ошибки
        }

        // Проверка индексов внутри методов для получения позиции стадии и фотографии
        private fun getStagePosition(position: Int): Int {
            var currentPosition = position
            for ((index, stage) in poverkaDTO.stages.withIndex()) {
                if (currentPosition < stage.photos.size) {
                    return index // Возвращаем позицию стадии
                }
                currentPosition -= stage.photos.size
            }
            return -1 // Если не удалось найти позицию
        }

        private fun getPhotoPosition(position: Int): Int {
            var currentPosition = position
            for (stage in poverkaDTO.stages) {
                if (currentPosition < stage.photos.size) {
                    return currentPosition // Возвращаем позицию фотографии
                }
                currentPosition -= stage.photos.size
            }
            return -1 // Если не удалось найти позицию
        }
    }
}
