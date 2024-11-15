import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.deviceinspectionapp.BitmapUtils
import com.example.deviceinspectionapp.CameraCall
import com.example.deviceinspectionapp.DeviceCheckActivity
import com.example.deviceinspectionapp.FsUtils
import com.example.deviceinspectionapp.R
import com.example.deviceinspectionapp.StageViewHolder
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
) : RecyclerView.Adapter<StageViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StageViewHolder {
        val stageView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_flexbox, parent, false) as FlexboxLayout
        return StageViewHolder(
            stageView,
            context,
            poverkaDTO,
            takePictureLauncher = { cameraCall -> context.takePictureLauncher.launch(cameraCall) },
            photoDirectory = context.photoDirectory
        )
    }

    override fun onBindViewHolder(holder: StageViewHolder, stageIdx: Int) {
        holder.bind(stageIdx, poverkaDTO.stages[stageIdx])
    }

    override fun getItemCount(): Int {
        return poverkaDTO.stages.size
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun processPhotoTakenEvent(call: CameraCall) {
//        val stageDTO = currentStageViewHolder.stageDTO
//        val photoDTO = stageDTO.photos[currentPhotoIdx]
        val fileName = call.fileUri.path!!.substring(
            call.fileUri.path!!.lastIndexOf('/') + 1,
            call.fileUri.path!!.length
        )
        val photoFile = File(context.photoDirectory, fileName)

        if (photoFile.exists()) {
            val thumbnailBitmap = BitmapUtils.createThumbnailFromFile(photoFile, context.contentResolver)
            val thumbFile = File(context.photoDirectory, "thumb_${fileName}")
            Log.d("creating thumb", "thumb_${fileName}")
            FileOutputStream(thumbFile).use { out ->
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            notifyItemChanged(call.stageIdx)
        }
    }
}