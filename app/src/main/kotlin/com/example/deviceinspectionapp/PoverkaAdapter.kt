import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceinspectionapp.BitmapUtils
import com.example.deviceinspectionapp.CameraCall
import com.example.deviceinspectionapp.DeviceCheckActivity
import com.example.deviceinspectionapp.R
import com.example.deviceinspectionapp.StageViewHolder
import java.io.File
import java.io.FileOutputStream

/**
 * https://guides.codepath.com/android/using-the-recyclerview
 */

class PoverkaAdapter(
    private val context: DeviceCheckActivity,
    private val poverkaDTO: PoverkaDTO
) : RecyclerView.Adapter<StageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StageViewHolder {
        // Создаем вью для каждого этапа
        val stageView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stage, parent, false) as LinearLayout
        return StageViewHolder(
            stageView,
            context,
            poverkaDTO,
            takePictureLauncher = { cameraCall -> context.takePictureLauncher.launch(cameraCall) },
            photoDirectory = context.photoDirectory
        )
    }

    override fun onBindViewHolder(holder: StageViewHolder, position: Int) {
        // Привязываем данные для текущего этапа
        holder.bind(position, poverkaDTO.stages[position])
    }

    override fun getItemCount(): Int {
        return poverkaDTO.stages.size
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun processPhotoTakenEvent(call: CameraCall) {
        val fileName = call.fileUri.path!!.substringAfterLast('/')
        val photoFile = File(context.photoDirectory, fileName)

        if (photoFile.exists()) {
            val thumbnailBitmap = BitmapUtils.createThumbnailFromFile(photoFile, context.contentResolver)
            val thumbFile = File(context.photoDirectory, "thumb_$fileName")
            Log.d("creating thumb", "thumb_$fileName")
            FileOutputStream(thumbFile).use { out ->
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            notifyItemChanged(call.stageIdx)
        }
    }
}
