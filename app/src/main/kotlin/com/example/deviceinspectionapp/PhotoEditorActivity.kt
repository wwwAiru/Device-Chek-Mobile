package com.example.deviceinspectionapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.chrisbanes.photoview.PhotoView
import java.io.File
import java.io.FileOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class PhotoEditActivity : AppCompatActivity() {

    private lateinit var photoUri: Uri
    private lateinit var originalBitmap: Bitmap
    private lateinit var photoView: PhotoView
    private var rotationAngle = 0f

    private var startAngle = 0f
    private var isRotating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_edit)

        // Получаем URI изображения из Intent
        photoUri = intent.getParcelableExtra("photoUri") ?: throw IllegalArgumentException("Photo URI is required")

        // Находим PhotoView
        photoView = findViewById(R.id.ivEdit)

        // Загружаем оригинальное изображение
        originalBitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(photoUri))

        // Отображаем изображение в PhotoView
        photoView.setImageBitmap(originalBitmap)

        // Настроим кнопки
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveEditedPhoto() }
        findViewById<Button>(R.id.btnCancel).setOnClickListener { finish() }

        // Устанавливаем слушатель касания
        photoView.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event == null) return false

                when (event.action) {
                    MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE -> {
                        if (event.pointerCount == 2) {
                            val angle = calculateRotationAngle(event)
                            if (isRotating) {
                                val deltaAngle = angle - startAngle
                                rotationAngle += deltaAngle
                                val rotatedBitmap = rotateBitmap(originalBitmap, rotationAngle)
                                photoView.setImageBitmap(rotatedBitmap)
                            }
                            startAngle = angle
                            isRotating = true
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                        isRotating = false
                    }
                }
                return true
            }
        })
    }

    // Вычисляем угол между двумя пальцами
    private fun calculateRotationAngle(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }

    // Поворачиваем изображение на заданный угол
    private fun rotateBitmap(bitmap: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // Сохраняем редактированное изображение
    private fun saveEditedPhoto() {
        try {
            // Извлекаем текущее изображение из PhotoView
            val bitmapToSave = (photoView.drawable as? BitmapDrawable)?.bitmap ?: originalBitmap

            // Перезаписываем файл с редактированным изображением
            val outputFile = File(photoUri.path)
            FileOutputStream(outputFile).use { out ->
                bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            // Возвращаем результат
            val resultIntent = Intent().apply {
                putExtra("photoUri", Uri.fromFile(outputFile))
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка при сохранении изображения", Toast.LENGTH_SHORT).show()
        }
    }
}
