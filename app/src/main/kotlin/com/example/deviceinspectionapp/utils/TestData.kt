package com.example.deviceinspectionapp

import PhotoDTO
import PoverkaDTO
import StageDTO

class TestData private constructor() {

    companion object {
        fun createTestInspectionData(): PoverkaDTO {
            return PoverkaDTO(
                uuid = "3b45f2a2-d2ad-4a0a-bbcf-68b8e25326cf",
                stages = listOf(
                    StageDTO(
                        stageCodeName = "check1",
                        caption = "пролив №1",
                        photos = generatePhotos("check1", 5) // Генерация 10 фотографий для стадии
                    ),
                    StageDTO(
                        stageCodeName = "check2",
                        caption = "пролив №2",
                        photos = generatePhotos("check2", 6) // Генерация 10 фотографий для стадии
                    )
                )
            )
        }

        fun generatePhotos(stageCodeName: String, count: Int): List<PhotoDTO> {
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
    }
}
