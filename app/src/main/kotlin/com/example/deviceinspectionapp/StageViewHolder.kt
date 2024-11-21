package com.example.deviceinspectionapp

import StageDTO
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceinspectionapp.preferences.AppPreferences
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File

class StageViewHolder(
    private val context: DeviceCheckActivity,
    stageView: View,
) : RecyclerView.ViewHolder(stageView) {
    private val flexLayout: FlexboxLayout = stageView.findViewById(R.id.flexLayoutPhotos)
    private val photoViews: List<View> = List(10) {
        LayoutInflater.from(stageView.context)
            .inflate(R.layout.item_photo, flexLayout, false)
    }
    private var stageIdx: Int = -1
    private lateinit var stageDTO: StageDTO

    init {
        // Вычисляем размер иконок в зависимости от устройства и версии
        val iconSize = calculateIconSize(context)

        // Перебираем все фото
        photoViews.forEachIndexed { photoIdx, photoView ->
            photoView.visibility = View.GONE  // Скрываем фото до тех пор, пока не будет установлено

            val imageView: ImageView = photoView.findViewById(R.id.ivPhoto)
            val textView: TextView = photoView.findViewById(R.id.photoName)


            // Устанавливаем динамический размер для иконок и текста
            val layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            imageView.layoutParams = layoutParams
            textView.maxWidth = iconSize

            // Устанавливаем обработчик клика на фото
            imageView.setOnClickListener {
                val photoDTO = stageDTO.photos[photoIdx]
                val photoFile = File(context.photoDirectory, photoDTO.imageFileName)

                if (photoFile.exists()) {
                    // Фото существует, показываем BottomSheet с опциями
                    showPhotoOptionsBottomSheet(photoIdx)
                } else {
                    // Иначе начинаем процесс фотографирования
                    takePhoto(photoIdx)
                }
            }

            // Добавляем фото в FlexboxLayout
            flexLayout.addView(photoView)
        }
    }


    fun bind(stageIdx: Int, stageDTO: StageDTO) {
        this.stageIdx = stageIdx
        this.stageDTO = stageDTO

        photoViews.forEachIndexed { photoIdx, photoView ->
            val imageView: ImageView = photoView.findViewById(R.id.ivPhoto)

            if (photoIdx < this.stageDTO.photos.size) {
                val photoDTO = this.stageDTO.photos[photoIdx]

                val textView: TextView = photoView.findViewById(R.id.photoName)
                textView.text = photoDTO.caption

                val thumbFile = File(context.photoDirectory, "thumb_${photoDTO.imageFileName}")

                // Сбрасываем кэш изображения перед обновлением
                imageView.setImageDrawable(null)

                if (thumbFile.exists()) {
                    imageView.setImageURI(FsUtils.getFileUri(context, thumbFile))
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

        // Получаем размеры экрана
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val windowMetrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics
        } else {
            val display = windowManager.defaultDisplay
            @Suppress("DEPRECATION")
            display.getMetrics(DisplayMetrics())
            return DisplayMetrics().widthPixels
        }

        val screenWidth = windowMetrics.bounds.width()

        // Получаем количество иконок в строке из AppPreferences
        val iconsInRow = AppPreferences.getIconsInRow(context)

        // Получаем размеры отступов контейнера и иконок из ресурсов
        val containerPadding = context.resources.getDimensionPixelSize(R.dimen.activity_device_check_padding) * 2 // для обеих сторон (левая и правая)
        val photoLinearLayoutPadding = context.resources.getDimensionPixelSize(R.dimen.item_photo_linearlayout_padding) * 2
        val stageScrollViewPadding = context.resources.getDimensionPixelSize(R.dimen.item_stage_scrollview_padding) * 2
        val stageFlexboxPadding = context.resources.getDimensionPixelSize(R.dimen.item_stage_flexbox_padding) * 2
        val photoIconPadding = context.resources.getDimensionPixelSize(R.dimen.item_photo_icon_padding) * 2
        val photoIconMargin = context.resources.getDimensionPixelSize(R.dimen.item_photo_cardview_margin) * 2

        // Рассчитываем доступную ширину для иконок с учетом паддингов и маржинов
        val availableWidth = screenWidth -
                            containerPadding -
                            photoLinearLayoutPadding -
                            stageScrollViewPadding -
                            stageFlexboxPadding -
                            (photoIconPadding * iconsInRow) -
                            (photoIconMargin * iconsInRow)

        // Рассчитываем размер каждой иконки
        return (availableWidth / iconsInRow)
    }



    // Функция отображения BottomSheetDialog для фото
    private fun showPhotoOptionsBottomSheet(photoIdx: Int) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val bottomSheetView = LayoutInflater.from(context)
            .inflate(R.layout.bottom_sheet_photo_options, null, false)

        // Находим и устанавливаем фото
        val imageView: ImageView = bottomSheetView.findViewById(R.id.fullImageView)
        val photoDTO = stageDTO.photos[photoIdx]
        val photoFile = File(context.photoDirectory, photoDTO.imageFileName)

        val photoUri = Uri.fromFile(photoFile)
        imageView.setImageURI(photoUri)

        // Обработчик для кнопки редактирования фото
        bottomSheetView.findViewById<View>(R.id.editButton).setOnClickListener {
            startPhotoEditing(photoUri, stageIdx, photoIdx)
            bottomSheetDialog.dismiss()
        }

        // Обработчик для кнопки повторной съёмки фото
        bottomSheetView.findViewById<View>(R.id.retakeButton).setOnClickListener {
            // Переснимаем фото
            takePhoto(photoIdx)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }


    // Функция для съемки фото
    private fun takePhoto(photoIdx: Int) {
        val photoFile =
            File(context.photoDirectory, stageDTO.photos[photoIdx].imageFileName)
        // Создаем дескриптор файла для фото
        val photoUri = FileProvider.getUriForFile(context, context.packageName, photoFile)
        // Запуск камеры для фотографирования
        context.takePictureLauncher.launch(CameraCall(photoUri, stageIdx, photoIdx))
    }

    /**
     * Запуск uCrop для обрезки изображения.
     */
    private fun startPhotoEditing(sourceUri: Uri, stageIdx: Int, photoIdx: Int) {
        // Получаем размеры изображения
        val originalSize = BitmapUtils.getImageDimensions(sourceUri, context)
        if (originalSize == null) {
            Log.e("UCrop", "Невозможно получить размеры изображения: $sourceUri")
            return
        }

        // Формируем URI для сохранения результата с оригинальным именем файла
        val destFile = File(context.photoDirectory, sourceUri.lastPathSegment!!)
        val destUri = Uri.fromFile(destFile)

        // Создаем объект PhotoEditorCall с нужными данными
        val photoEditorCall = PhotoEditorCall(
            fileUri = sourceUri,
            destinationUri = destUri,
            stageIdx = stageIdx,
            photoIdx = photoIdx
        )

        // Запускаем UCrop с использованием ActivityResultLauncher
        context.editPhotoLauncher.launch(photoEditorCall)
    }




}
