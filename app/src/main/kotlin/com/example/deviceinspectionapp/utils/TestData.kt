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
                        photos = listOf(
                            PhotoDTO(
                                photoCodeName = "gidrometr",
                                caption = "гидрометр",
                                imageFileName = "3b45f2a2-d2ad-4a0a-bbcf-68b8e25326cf_check1_gidrometr_1.jpg"
                            ),
                            PhotoDTO(
                                photoCodeName = "gidrometr",
                                caption = "гидрометр",
                                imageFileName = "3b45f2a2-d2ad-4a0a-bbcf-68b8e25326cf_check1_gidrometr_2.jpg"
                            )
                        )
                    ),
                    StageDTO(
                        stageCodeName = "check2",
                        caption = "пролив №2",
                        photos = listOf(
                            PhotoDTO(
                                photoCodeName = "gidrometr",
                                caption = "гидрометр",
                                imageFileName = "3b45f2a2-d2ad-4a0a-bbcf-68b8e25326cf_check2_gidrometr_1.jpg"
                            ),
                            PhotoDTO(
                                photoCodeName = "gidrometr",
                                caption = "гидрометр",
                                imageFileName = "3b45f2a2-d2ad-4a0a-bbcf-68b8e25326cf_check2_gidrometr_2.jpg"
                            )
                        )
                    )
                )
            )
        }
    }
}
