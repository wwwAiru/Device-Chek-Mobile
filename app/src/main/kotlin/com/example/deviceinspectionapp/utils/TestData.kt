package com.example.deviceinspectionapp.utils

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
                    )
                )
            )
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
    }
}
