package com.example.deviceinspectionapp

import PoverkaDTO
import StageDTO
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File

class StageViewHolder(
    private val stageView: View,
    private val context: DeviceCheckActivity,
    private val poverkaDTO: PoverkaDTO,
    private val takePictureLauncher: (CameraCall) -> Unit,
    private val photoDirectory: File
) : RecyclerView.ViewHolder(stageView) {

    private val flexboxLayout: FlexboxLayout = stageView as FlexboxLayout
    private val photoViews: List<View> = List(10) {
        LayoutInflater.from(stageView.context)
            .inflate(R.layout.item_photo, flexboxLayout, false)
    }
    private var stageIdx: Int = -1
    private lateinit var stageDTO: StageDTO

    init {
        photoViews.forEachIndexed { photoIdx, photoView ->
            photoView.visibility = View.GONE
            val imageView: ImageView = photoView.findViewById(R.id.ivPhoto)
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

            flexboxLayout.addView(photoView)
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

    private fun showPhotoOptionsBottomSheet(stageIdx: Int, photoIdx: Int) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val bottomSheetView = LayoutInflater.from(context)
            .inflate(R.layout.bottom_sheet_photo_options, null)

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

    private fun takePhoto(stageViewHolder: StageViewHolder, photoIdx: Int) {
        val photoFile =
            File(context.photoDirectory, stageViewHolder.stageDTO.photos[photoIdx].imageFileName)
        // Создаем дескриптор файла для фото
        val photoUri = FileProvider.getUriForFile(context, context.packageName, photoFile)
        // Запуск камеры для фотографирования
        context.takePictureLauncher.launch(CameraCall(photoUri, stageViewHolder.stageIdx))
    }
}
