package com.example.deviceinspectionapp.utils

import PhotoDTO
import PoverkaDTO
import StageDTO
import android.content.Context
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class DeviceCheckUtil private constructor() {

    companion object {
        fun createCheckDataJson(context: Context): String {
            val uuid = UUID.randomUUID().toString()
            val poverkaDTO = PoverkaDTO(
                uuid = uuid,
                stages = List(6) { index ->
                    StageDTO(
                        stageCodeName = "check",
                        caption = "пролив №${index + 1}",
                        photos = generatePhotos(uuid, "check", index + 1, 10) // Генерация фотографий для стадии
                    )
                }
            )

            // Сохранение JSON поверки с его UUID
            savePoverkaJson(context, poverkaDTO)

            // Возвращение JSON строки
            return Json.encodeToString(poverkaDTO)
        }

        private fun generatePhotos(uuid: String, stageCodeName: String, stageNumber: Int, count: Int): List<PhotoDTO> {
            val photos = mutableListOf<PhotoDTO>()
            for (i in 0 until count) {
                val photoCodeName = "$i"
                val imageFileName = "${uuid}_${stageCodeName}${stageNumber}_$photoCodeName.jpg" // Генерация имени файла
                photos.add(
                    PhotoDTO(
                        photoCodeName = photoCodeName,
                        caption = "Фото $i для стадии $stageCodeName$stageNumber",
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

            jsonFile.parentFile?.mkdirs()
            jsonFile.writeText(jsonString)
            Log.d("TestData", "JSON поверки сохранен: ${jsonFile.absolutePath}")
        }
    }
}
