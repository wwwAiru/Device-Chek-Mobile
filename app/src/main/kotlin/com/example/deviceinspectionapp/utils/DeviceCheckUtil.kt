package com.example.deviceinspectionapp.utils

import PhotoDTO
import PoverkaDTO
import StageDTO
import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class DeviceCheckUtil private constructor() {

    companion object {
        fun createCheckDataJson(context: Context): PoverkaDTO {
            val poverkaDTO = PoverkaDTO(
                uuid = "3b45f2a2-d2ad-4a0a-bbcf-68b8e25326cf",
                stages = listOf(
                    StageDTO(
                        stageCodeName = "check1",
                        caption = "   Пролив №1 - тут для отладки длинное описание чтобы посмотреть как будет выглядеть многострочный текст.",
                        photos = generatePhotos("check1", 5) // Генерация 10 фотографий для стадии
                    ),
                    StageDTO(
                        stageCodeName = "check2",
                        caption = "пролив №2",
                        photos = generatePhotos("check2", 5) // Генерация 10 фотографий для стадии
                    ),
                    StageDTO(
                        stageCodeName = "check3",
                        caption = "пролив №3",
                        photos = generatePhotos("check3", 10) // Генерация 10 фотографий для стадии
                    ),
                    StageDTO(
                        stageCodeName = "check4",
                        caption = "пролив №4",
                        photos = generatePhotos("check4", 3) // Генерация 10 фотографий для стадии
                    ),
                    StageDTO(
                        stageCodeName = "check5",
                        caption = "пролив №5",
                        photos = generatePhotos("check5", 2) // Генерация 10 фотографий для стадии
                    ),
                    StageDTO(
                        stageCodeName = "check6",
                        caption = "пролив №6",
                        photos = generatePhotos("check6", 1) // Генерация 10 фотографий для стадии
                    ),
                    StageDTO(
                        stageCodeName = "check7",
                        caption = "пролив №7",
                        photos = generatePhotos("check7", 1) // Генерация 10 фотографий для стадии
                    ),
                    StageDTO(
                        stageCodeName = "check8",
                        caption = "пролив №8",
                        photos = generatePhotos("check8", 1) // Генерация 10 фотографий для стадии
                    )
                )
            )

            // Сохранение JSON поверки с его UUID
            savePoverkaJson(context, poverkaDTO)

            return poverkaDTO
        }

        private fun generatePhotos(stageCodeName: String, count: Int): List<PhotoDTO> {
            val photos = mutableListOf<PhotoDTO>()
            for (i in 0 until count) {
                val photoCodeName = "$i"
                val imageFileName =
                    "3b45f2a2-d2ad-4a0a-bbcf-68b8e25326cf_${stageCodeName}_$photoCodeName.jpg" // Генерация имени файла
                photos.add(
                    PhotoDTO(
                        photoCodeName = photoCodeName,
                        caption = "Фото $i для стадии $stageCodeName",
                        imageFileName = imageFileName
                    )
                )
            }
            return photos
        }

        private fun savePoverkaJson(context: Context, poverkaDTO: PoverkaDTO) {
            val uuid = poverkaDTO.uuid
            val jsonFileName = "$uuid.json"
            val jsonFile = File(context.filesDir, "checks/$jsonFileName")
            val jsonString = Json.encodeToString(poverkaDTO)

            try {
                jsonFile.parentFile?.mkdirs()
                jsonFile.writeText(jsonString)
                Log.d("TestData", "JSON поверки сохранен: ${jsonFile.absolutePath}")
            } catch (e: Exception) {
                Log.e("TestData", "Ошибка при сохранении JSON поверки: ${e.message}")
                Toast.makeText(context, "Ошибка при сохранении JSON поверки.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
