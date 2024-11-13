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
import com.example.deviceinspectionapp.DeviceCheckActivity
import com.example.deviceinspectionapp.FsUtils
import com.example.deviceinspectionapp.R
import com.example.deviceinspectionapp.model.PhotoViewModel
import com.google.android.flexbox.FlexboxLayout
import java.io.File
import java.io.FileOutputStream

/**
 * https://guides.codepath.com/android/using-the-recyclerview
 */

class PoverkaAdapter(
    private val context: DeviceCheckActivity,
    private val poverkaDTO: PoverkaDTO,
    private val photoViewModel: PhotoViewModel
) : RecyclerView.Adapter<PoverkaAdapter.StageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StageViewHolder {
        val stageView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_flexbox, parent, false) as FlexboxLayout
        return StageViewHolder(stageView)
    }

    override fun onBindViewHolder(holder: StageViewHolder, stageIdx: Int) {
        holder.bind(stageIdx, poverkaDTO.stages[stageIdx])
    }

    override fun getItemCount(): Int {
        return poverkaDTO.stages.size
    }

    private fun takePhoto(stageViewHolder: StageViewHolder, photoIdx: Int) {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = File(context.photoDirectory, stageViewHolder.stageDTO.photos[photoIdx].imageFileName)
        val photoUri = FileProvider.getUriForFile(context, context.packageName, photoFile)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

        // Сохраняем индексы во ViewModel вместо хранения StageViewHolder
        photoViewModel.photoIdx = photoIdx
        photoViewModel.stageIdx = stageViewHolder.stageIdx

        context.takePictureLauncher.launch(takePictureIntent)
    }

    fun processPhotoTakenEvent() {
        // Извлекаем сохраненные индексы из ViewModel
        val stageIdx = photoViewModel.stageIdx ?: return
        val photoIdx = photoViewModel.photoIdx ?: return

        // Находим StageDTO и StageViewHolder по индексу
        val stageDTO = poverkaDTO.stages[stageIdx]
        val photoDTO = stageDTO.photos[photoIdx]
        val photoFile = File(context.photoDirectory, photoDTO.imageFileName)

        if (photoFile.exists()) {
            val thumbnailBitmap = BitmapUtils.createThumbnailFromFile(photoFile)
            val thumbFile = File(context.photoDirectory, "thumb_${photoDTO.imageFileName}")
            FileOutputStream(thumbFile).use { out ->
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            // Обновляем элемент RecyclerView, чтобы отобразить сделанное фото
            notifyItemChanged(stageIdx)
        }
    }

    inner class StageViewHolder(stageView: View) : RecyclerView.ViewHolder(stageView) {
        private val flexboxLayout: FlexboxLayout = stageView as FlexboxLayout
        val photoViews: List<View> = List(10) {
            LayoutInflater.from(stageView.context).inflate(R.layout.item_photo, flexboxLayout, false)
        }
        var stageIdx: Int = -1
        lateinit var stageDTO: StageDTO

        init {
            photoViews.forEachIndexed { photoIdx, photoView ->
                photoView.visibility = View.GONE
                val imageView: ImageView = photoView.findViewById(R.id.ivPhoto)
                imageView.setOnClickListener {
                    takePhoto(this, photoIdx)
                }
                flexboxLayout.addView(photoView)
            }
        }

        fun bind(stageIdx: Int, stageDTO: StageDTO) {
            this.stageIdx = stageIdx
            this.stageDTO = stageDTO

            photoViews.forEachIndexed { photoIdx, photoView ->
                if (photoIdx < stageDTO.photos.size) {
                    val textView: TextView = photoView.findViewById(R.id.photoName)
                    textView.text = stageDTO.photos[photoIdx].caption

                    val imageView: ImageView = photoView.findViewById(R.id.ivPhoto)
                    val thumbFile = File(context.photoDirectory, "thumb_${stageDTO.photos[photoIdx].imageFileName}")

                    if (thumbFile.exists()) {
                        imageView.setImageURI(FsUtils.getFileUri(context, thumbFile))
                    } else {
                        imageView.setImageResource(R.drawable.ic_camera)
                    }
                    photoView.visibility = View.VISIBLE
                } else {
                    photoView.visibility = View.GONE
                }
            }
        }
    }
}
