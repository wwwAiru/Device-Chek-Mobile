package com.example.deviceinspectionapp

import PoverkaDTO
import StageDTO
import android.content.Context
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.WindowMetrics
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File

class StageViewHolder(
    private val stageView: View,
    private val context: DeviceCheckActivity,
    private val poverkaDTO: PoverkaDTO,
    private val takePictureLauncher: (CameraCall) -> Unit,
    private val photoDirectory: File
) : RecyclerView.ViewHolder(stageView) {

    private val gridLayout: GridLayout = stageView.findViewById(R.id.gridLayoutPhotos)
    private val photoViews: List<View> = List(10) {
        LayoutInflater.from(stageView.context)
            .inflate(R.layout.item_photo, gridLayout, false)
    }
    private var stageIdx: Int = -1
    private lateinit var stageDTO: StageDTO

    init {
        val iconSize = calculateIconSize(context) // Вычисляем размер иконок
        photoViews.forEachIndexed { photoIdx, photoView ->
            photoView.visibility = View.GONE
            val imageView: ImageView = photoView.findViewById(R.id.ivPhoto)

            // Устанавливаем динамический размер для иконок
            imageView.layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            // Обновление в onClick для ImageView
            imageView.setOnClickListener {
                val photoDTO = stageDTO.photos[photoIdx]
                val photoFile = File(context.photoDirectory, photoDTO.imageFileName)

                if (photoFile.exists()) {
                    // Открываем BottomSheetDialog, если фото уже существует
                    showPhotoOptionsBottomSheet(stageIdx, photoIdx)
                } else {
                    // Иначе начинаем процесс фотографирования
                    takePhoto(this, photoIdx)
                }
            }

            gridLayout.addView(photoView)
        }
    }

    fun bind(stageIdx: Int, stageDTO: StageDTO) {
        this.stageIdx = stageIdx
        this.stageDTO = stageDTO

        photoViews.forEachIndexed { photoIdx, photoView ->
            val imageView: ImageView = photoView.findViewById(R.id.ivPhoto)

            // Сбросьте изображение и тег перед установкой миниатюры
            imageView.setImageDrawable(null)
            imageView.tag = null

            if (photoIdx < stageDTO.photos.size) {
                val textView: TextView = photoView.findViewById(R.id.photoName)
                textView.text = stageDTO.photos[photoIdx].caption

                val photoDTO = stageDTO.photos[photoIdx]
                val thumbFile = File(context.photoDirectory, "thumb_${photoDTO.imageFileName}")

                if (thumbFile.exists()) {
                    val thumbUri = FsUtils.getFileUri(context, thumbFile)
                    imageView.setImageURI(thumbUri)
                    imageView.tag = thumbUri
                } else {
                    // Если миниатюры нет, установите значок камеры
                    imageView.setImageResource(R.drawable.ic_camera)
                }

                photoView.visibility = View.VISIBLE
            } else {
                photoView.visibility = View.GONE
            }
        }
    }

    private fun calculateIconSize(context: Context): Int {
        val size = Point()

        // Проверка версии Android и использование соответствующего метода
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Для Android 11 (API 30) и выше
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val windowMetrics: WindowMetrics = windowManager.currentWindowMetrics
            windowMetrics.bounds.let {
                size.set(it.width(), it.height())
            }
        } else {
            // Для более старых версий Android
            val displayMetrics = DisplayMetrics()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display: Display? = windowManager.defaultDisplay // Для старых версий
            if (display != null) {
                display.getMetrics(displayMetrics)
                size.set(displayMetrics.widthPixels, displayMetrics.heightPixels)
            }
        }

        val screenWidth = size.x
        // Размер иконки вычисляем как примерно треть ширины экрана, можно изменить деление для других размеров.
        return (screenWidth / 3.5).toInt()  // Здесь можно изменить деление для другой пропорции
    }

    // Функция отображения BottomSheetDialog для фото
    private fun showPhotoOptionsBottomSheet(stageIdx: Int, photoIdx: Int) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val bottomSheetView = LayoutInflater.from(context)
            .inflate(R.layout.bottom_sheet_photo_options, null, false)

        // Находим и устанавливаем фото
        val imageView: ImageView = bottomSheetView.findViewById(R.id.fullImageView)
        val photoDTO = poverkaDTO.stages[stageIdx].photos[photoIdx]
        val photoFile = File(context.photoDirectory, photoDTO.imageFileName)

        if (photoFile.exists()) {
            val photoUri = Uri.fromFile(photoFile)
            imageView.setImageURI(photoUri)
        }

        // Обработчик для кнопки редактирования фото
        bottomSheetView.findViewById<View>(R.id.editButton).setOnClickListener {
            // Код для редактирования фото
            bottomSheetDialog.dismiss()
        }

        // Обработчик для кнопки повторной съёмки фото
        bottomSheetView.findViewById<View>(R.id.retakeButton).setOnClickListener {
            // Переснимаем фото
            takePhoto(this, photoIdx)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }

    // Функция для съемки фото
    private fun takePhoto(stageViewHolder: StageViewHolder, photoIdx: Int) {
        val photoFile =
            File(context.photoDirectory, stageViewHolder.stageDTO.photos[photoIdx].imageFileName)
        // Создаем дескриптор файла для фото
        val photoUri = FileProvider.getUriForFile(context, context.packageName, photoFile)
        // Запуск камеры для фотографирования
        context.takePictureLauncher.launch(CameraCall(photoUri, stageViewHolder.stageIdx))
    }
}
