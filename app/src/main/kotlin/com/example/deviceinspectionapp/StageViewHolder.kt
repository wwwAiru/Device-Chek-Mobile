package com.example.deviceinspectionapp

import StageDTO
import android.content.Context
import android.net.Uri
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
import com.example.deviceinspectionapp.utils.ExifUtils
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File

class StageViewHolder(
    private val context: DeviceCheckActivity,
    stageView: View,
) : RecyclerView.ViewHolder(stageView) {

    // Корневой уровень
    private val flexLayout: FlexboxLayout = stageView.findViewById(R.id.flexLayoutPhotos) // Контейнер фотографий
    private val stageHeaderContainer: View = stageView.findViewById(R.id.headerContainer) // Контейнер заголовка
    private val photosContainer: View = stageView.findViewById(R.id.photoContainer)       // Контейнер для фото

    // Заголовок этапа
    private val ivExpandArrow: ImageView = stageView.findViewById(R.id.ivExpandArrow)     // Иконка состояния (развёрнут/свёрнут)
    private val tvStageCaption: TextView = stageView.findViewById(R.id.tvStageCaption)    // Название этапа

    // Список фото
    private val photoViews: List<View> = List(10) {
        LayoutInflater.from(stageView.context)
            .inflate(R.layout.item_photo, flexLayout, false)                             // Элементы фото
    }

    private var stageIdx: Int = -1                                                       // Индекс текущего этапа
    private var isExpanded: Boolean = false                                              // Состояние развёрнутости
    private lateinit var stageDTO: StageDTO                                              // Данные этапа

    init {
        // Рассчёт размера иконок
        val iconSize = calculateIconSize(context)

        // Инициализация заголовка
        stageHeaderContainer.setOnClickListener {
            isExpanded = !isExpanded
            updateExpandState()
        }

        // Инициализация фотографий
        photoViews.forEachIndexed { photoIdx, photoView ->
            photoView.visibility = View.GONE // Фото скрыты по умолчанию

            // Элементы фотографии
            val imageView: ImageView = photoView.findViewById(R.id.ivPhoto)
            val textView: TextView = photoView.findViewById(R.id.photoName)

            // Установка размеров
            val layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            imageView.layoutParams = layoutParams
            textView.maxWidth = iconSize

            // Обработчик кликов на фото
            imageView.setOnClickListener {
                val photoDTO = stageDTO.photos[photoIdx]
                val photoFile = File(context.photoDirectory, photoDTO.imageFileName)
                if (photoFile.exists()) {
                    showPhotoOptionsBottomSheet(photoIdx) // Показываем диалог опций
                } else {
                    takePhoto(photoIdx) // Начинаем процесс съёмки
                }
            }

            // Добавление фото в FlexboxLayout
            flexLayout.addView(photoView)
        }
    }

    // Привязка данных этапа
    fun bind(stageIdx: Int, stageDTO: StageDTO) {
        this.stageIdx = stageIdx
        this.stageDTO = stageDTO

        tvStageCaption.text = stageDTO.caption // Обновляем заголовок
        updateExpandState()                   // Обновляем состояние развёрнутости

        // Обновление фото
        photoViews.forEachIndexed { photoIdx, photoView ->
            val imageView: ImageView = photoView.findViewById(R.id.ivPhoto)

            if (photoIdx < stageDTO.photos.size) {
                val photoDTO = stageDTO.photos[photoIdx]
                val textView: TextView = photoView.findViewById(R.id.photoName)
                textView.text = photoDTO.caption

                val thumbFile = File(context.photoDirectory, "thumb_${photoDTO.imageFileName}")
                imageView.setImageDrawable(null) // Сброс кэша изображения

                if (thumbFile.exists()) {
                    imageView.setImageURI(FsUtils.getFileUri(context, thumbFile))
                } else {
                    imageView.setImageResource(R.drawable.ic_camera) // Иконка по умолчанию
                }

                photoView.visibility = View.VISIBLE
            } else {
                photoView.visibility = View.GONE
            }
        }
    }

    // Обновление состояния развёрнутости
    private fun updateExpandState() {
        ivExpandArrow.setImageResource(
            if (isExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
        )
        photosContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
    }

    // Обновление миниатюры фотографии
    fun updateThumbnail(photoIdx: Int) {
        val photoView = photoViews[photoIdx]
        val imageView: ImageView = photoView.findViewById(R.id.ivPhoto)
        val photoDTO = stageDTO.photos[photoIdx]
        val thumbFile = File(context.photoDirectory, "thumb_${photoDTO.imageFileName}")
        // Сбрасываем текущее изображение перед установкой нового
        imageView.setImageDrawable(null)
        if (thumbFile.exists()) {
            imageView.setImageURI(FsUtils.getFileUri(context, thumbFile))
        } else {
            imageView.setImageResource(R.drawable.ic_camera)
            throw RuntimeException("if (thumbFile.exists()) expecting file always exists")
        }
    }

    // Показ диалогового окна с опциями фото
    private fun showPhotoOptionsBottomSheet(photoIdx: Int) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val bottomSheetView = LayoutInflater.from(context)
            .inflate(R.layout.bottom_sheet_photo_options, null, false)

        // Элементы диалога
        val imageView: ImageView = bottomSheetView.findViewById(R.id.fullImageView)
        val photoDTO = stageDTO.photos[photoIdx]
        val photoFile = File(context.photoDirectory, photoDTO.imageFileName)
        imageView.setImageURI(Uri.fromFile(photoFile))

        // Кнопки в диалоге
        bottomSheetView.findViewById<View>(R.id.editButton).setOnClickListener {
            startPhotoEditing(Uri.fromFile(photoFile), stageIdx, photoIdx)
            bottomSheetDialog.dismiss()
        }

        // Обработчик для кнопки повторной съёмки фото
        bottomSheetView.findViewById<View>(R.id.retakeButton).setOnClickListener {
            takePhoto(photoIdx)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }

    // Начало съёмки фото
    private fun takePhoto(photoIdx: Int) {
        val photoFile = File(context.photoDirectory, stageDTO.photos[photoIdx].imageFileName)
        val photoUri = FileProvider.getUriForFile(context, context.packageName, photoFile)
        context.takePictureLauncher.launch(CameraCall(photoUri, stageIdx, photoIdx))
    }

    // Редактирование фото
    private fun startPhotoEditing(sourceUri: Uri, stageIdx: Int, photoIdx: Int) {
        val destFile = File(context.photoDirectory, sourceUri.lastPathSegment!!)
        val destUri = Uri.fromFile(destFile)

        val exifDataBeforeEdit = ExifUtils.readExifData(destFile)
        ExifUtils.logExifData("EXIF BEFORE", exifDataBeforeEdit)

        context.editPhotoLauncher.launch(
            PhotoEditorCall(
                fileUri = sourceUri,
                destinationUri = destUri,
                stageIdx = stageIdx,
                photoIdx = photoIdx
            )
        )
    }

    companion object {
        private var cachedIconSize: Int? = null

        // Метод для вычисления или получения кэшированного значения размера иконок
        fun calculateIconSize(context: Context): Int {
            // Если размер уже вычислен, возвращаем его
            cachedIconSize?.let {
                return it
            }

            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val displayMetrics = DisplayMetrics()

            // Получаем размеры экрана для API 23 и выше
            display.getMetrics(displayMetrics)

            val screenWidth = displayMetrics.widthPixels

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
            val iconSize = (availableWidth / iconsInRow)

            // Кэшируем рассчитанный размер
            cachedIconSize = iconSize

            return iconSize
        }
    }
}
