package com.example.deviceinspectionapp

import android.app.Activity
import android.content.Intent
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Менеджер для отображения этапов и их фотографий в RecyclerView
 */
class StageManager(private val stages: List<StageDTO>) : RecyclerView.Adapter<StageManager.StageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stage, parent, false)
        return StageViewHolder(view)
    }

    override fun onBindViewHolder(holder: StageViewHolder, position: Int) {
        val stage = stages[position]
        holder.bind(stage)
    }

    override fun getItemCount(): Int = stages.size

    /**
     * ViewHolder для отображения одного этапа
     */
    inner class StageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvStageName: TextView = view.findViewById(R.id.tvStageName)
        private val gridLayout: ViewGroup = view.findViewById(R.id.gridLayoutPhotos)

        fun bind(stage: StageDTO) {
            tvStageName.text = stage.caption
            gridLayout.removeAllViews()

            for (i in 1..10) {
                val photoView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_photo, gridLayout, false)

                val ivPhoto = photoView.findViewById<ImageView>(R.id.ivPhoto)
                ivPhoto.setImageResource(R.drawable.ic_camera)

                ivPhoto.setOnClickListener {
                    // Здесь вызов камеры
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    (itemView.context as Activity).startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA)
                }

                gridLayout.addView(photoView)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_CAMERA = 1001
    }

}
