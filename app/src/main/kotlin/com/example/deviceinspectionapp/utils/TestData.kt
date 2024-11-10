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
                        photos = generatePhotos("check1", 10) // Генерация 10 фотографий для стадии
                    ),
                    StageDTO(
                        stageCodeName = "check2",
                        caption = "пролив №2",
                        photos = generatePhotos("check2", 10) // Генерация 10 фотографий для стадии
                    )
                )
            )
        }

        // Функция для генерации фотографий на стадию
        private fun generatePhotos(stageCodeName: String, photoCount: Int): List<PhotoDTO> {
            val photos = mutableListOf<PhotoDTO>()
            for (i in 1..photoCount) {
                photos.add(
                    PhotoDTO(
                        photoCodeName = "gidrometr",
                        caption = "гидрометр",
                        imageFileName = "3b45f2a2-d2ad-4a0a-bbcf-68b8e25326cf_${stageCodeName}_gidrometr_$i.jpg"
                    )
                )
            }
            return photos
        }
    }
}
