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
import com.google.android.flexbox.FlexboxLayout
import java.io.File
import java.io.FileOutputStream

/**
 * https://guides.codepath.com/android/using-the-recyclerview
 */

class PoverkaAdapter(
    private val context: DeviceCheckActivity,
    private val poverkaDTO: PoverkaDTO,
) : RecyclerView.Adapter<PoverkaAdapter.StageViewHolder>() {
    private var currentPhotoIdx: Int = -1
    private lateinit var currentStageViewHolder: StageViewHolder

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
        // Запуск камеры для фотографирования
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        takePictureIntent.putExtra("Index", -1)

        val photoFile = File(context.photoDirectory, stageViewHolder.stageDTO.photos[photoIdx].imageFileName)
        // Создаем дескриптор файла для фото
        val photoUri = FileProvider.getUriForFile(context, context.packageName, photoFile)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        currentPhotoIdx = photoIdx
        currentStageViewHolder = stageViewHolder
        context.takePictureLauncher.launch(takePictureIntent)
    }

    fun processPhotoTakenEvent() {
        val stageDTO = currentStageViewHolder.stageDTO
        val photoDTO = stageDTO.photos[currentPhotoIdx]
        val photoFile = File(context.photoDirectory, photoDTO.imageFileName)

        if (photoFile.exists()) {
            val thumbnailBitmap = BitmapUtils.createThumbnailFromFile(photoFile)
            val thumbFile = File(context.photoDirectory,"thumb_${photoDTO.imageFileName}")
            Log.d("creating thumb", "thumb_${photoDTO.imageFileName}")
            FileOutputStream(thumbFile).use { out ->
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            notifyItemChanged(currentStageViewHolder.stageIdx)
        }
    }

    inner class StageViewHolder(stageView: View) : RecyclerView.ViewHolder(stageView) {
        private val flexboxLayout: FlexboxLayout = stageView as FlexboxLayout
        val photoViews: List<View> = List(10) { // Pre-create item views
            LayoutInflater.from(stageView.context)
                .inflate(R.layout.item_photo, flexboxLayout, false)
        }
        var stageIdx: Int = -1
        lateinit var stageDTO: StageDTO

        init {
            // Add created views to the layout and set them initially to GONE
            photoViews.forEachIndexed { photoIdx, photoView ->
                photoView.visibility = View.GONE
                val imageView: ImageView = photoView.findViewById(R.id.ivPhoto)
                imageView.setOnClickListener {
                    takePhoto(this, photoIdx)
                }
                flexboxLayout.addView(photoView)
            }
        }

        fun bind(stageIdx:Int, stageDTO: StageDTO) {
            this.stageIdx = stageIdx
            this.stageDTO = stageDTO

            photoViews.forEachIndexed { photoIdx, photoView ->
                if (photoIdx < stageDTO.photos.size) {
                    val textView: TextView = photoView.findViewById(R.id.photoName)
                    textView.text = stageDTO.photos[photoIdx].caption

                    val imageView: ImageView = photoView.findViewById(R.id.ivPhoto)

                    if (imageView.tag is Uri) {
                        Log.d("", "setting thumb ${stageIdx}_${photoIdx} : ${imageView.tag} addr: $imageView")
                        imageView.setImageURI(imageView.tag as Uri)
                    } else {
                        val photoDTO = stageDTO.photos[photoIdx]
                        val thumbFile = File(context.photoDirectory, "thumb_${photoDTO.imageFileName}")

                        if (thumbFile.exists()) {
                            imageView.tag = FsUtils.getFileUri(context, thumbFile)
                            imageView.setImageURI(imageView.tag as Uri)
                        } else {
                            Log.d("", "setting ic_camera ${stageIdx}_${photoIdx} : ${imageView.tag} addr: $imageView")
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
