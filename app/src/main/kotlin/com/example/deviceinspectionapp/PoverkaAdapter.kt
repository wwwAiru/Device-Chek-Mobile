import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceinspectionapp.BitmapUtils
import com.example.deviceinspectionapp.CameraCall
import com.example.deviceinspectionapp.DeviceCheckActivity
import com.example.deviceinspectionapp.FsUtils
import com.example.deviceinspectionapp.R
import com.google.android.flexbox.FlexboxLayout
import java.io.File
import java.io.FileOutputStream
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * https://guides.codepath.com/android/using-the-recyclerview
 */

class PoverkaAdapter(
    private val context: DeviceCheckActivity,
    private val poverkaDTO: PoverkaDTO,
) : RecyclerView.Adapter<PoverkaAdapter.StageViewHolder>() {
//    private var currentPhotoIdx: Int = -1
//    private lateinit var currentStageViewHolder: StageViewHolder

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StageViewHolder {
        val stageView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_flexbox, parent, false) as FlexboxLayout
        Log.d("", "PoverkaAdapter.onCreateViewHolder()")
        return StageViewHolder(stageView)
    }

    override fun onBindViewHolder(holder: StageViewHolder, stageIdx: Int) {
        Log.d("", "PoverkaAdapter.onBindViewHolder(${holder}, $stageIdx )")
        // Находим нужную стадию и фото внутри стадии по позиции
        holder.bind(stageIdx, poverkaDTO.stages[stageIdx])
    }

    override fun getItemCount(): Int {
        return poverkaDTO.stages.size
    }

    private fun takePhoto(stageViewHolder: StageViewHolder, photoIdx: Int) {
        val photoFile =
            File(context.photoDirectory, stageViewHolder.stageDTO.photos[photoIdx].imageFileName)
        // Создаем дескриптор файла для фото
        val photoUri = FileProvider.getUriForFile(context, context.packageName, photoFile)
        // Запуск камеры для фотографирования
        context.takePictureLauncher.launch(CameraCall(photoUri, stageViewHolder.stageIdx))
    }

    fun processPhotoTakenEvent(call: CameraCall) {
//        val stageDTO = currentStageViewHolder.stageDTO
//        val photoDTO = stageDTO.photos[currentPhotoIdx]
        val fileName = call.fileUri.path!!.substring(
            call.fileUri.path!!.lastIndexOf('/') + 1,
            call.fileUri.path!!.length
        )
        val photoFile = File(context.photoDirectory, fileName)

        if (photoFile.exists()) {
            val thumbnailBitmap = BitmapUtils.createThumbnailFromFile(photoFile)
            val thumbFile = File(context.photoDirectory, "thumb_${fileName}")
            Log.d("creating thumb", "thumb_${fileName}")
            FileOutputStream(thumbFile).use { out ->
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            notifyItemChanged(call.stageIdx)
        }
    }

    inner class StageViewHolder(stageView: View) : RecyclerView.ViewHolder(stageView) {
        private val flexboxLayout: FlexboxLayout = stageView as FlexboxLayout
        val photoViews: List<View> = List(10) {
            LayoutInflater.from(stageView.context)
                .inflate(R.layout.item_photo, flexboxLayout, false)
        }
        var stageIdx: Int = -1
        lateinit var stageDTO: StageDTO

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



        fun bind(stageIdx: Int, stageDTO: StageDTO) {
            this.stageIdx = stageIdx
            this.stageDTO = stageDTO

            photoViews.forEachIndexed { photoIdx, photoView ->
                if (photoIdx < stageDTO.photos.size) {
                    val textView: TextView = photoView.findViewById(R.id.photoName)
                    textView.text = stageDTO.photos[photoIdx].caption

                    val imageView: ImageView = photoView.findViewById(R.id.ivPhoto)

                    if (imageView.tag is Uri) {
                        imageView.setImageURI(imageView.tag as Uri)
                    } else {
                        val photoDTO = stageDTO.photos[photoIdx]
                        val thumbFile =
                            File(context.photoDirectory, "thumb_${photoDTO.imageFileName}")

                        if (thumbFile.exists()) {
                            imageView.tag = FsUtils.getFileUri(context, thumbFile)
                            imageView.setImageURI(imageView.tag as Uri)
                        } else {
                            imageView.setImageResource(R.drawable.ic_camera)
                        }
                    }
                    photoView.visibility = View.VISIBLE
                } else {
                    photoView.visibility = View.GONE
                }
            }
        }
    }
}