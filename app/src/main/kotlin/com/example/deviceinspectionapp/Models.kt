package com.example.deviceinspectionapp

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Класс данных для описания процесса поверки
 * @param uuid уникальный идентификатор поверки
 * @param caption название поверки
 * @param description описание поверки
 * @param stages список этапов поверки
 */
@Serializable
data class PoverkaDTO(
    val uuid: String = UUID.randomUUID().toString(),
    val caption: String = "",
    val description: String = "",
    val stages: List<StageDTO>
)

/**
 * Класс данных для описания этапа поверки
 * @param stageCodeName кодовое название этапа
 * @param caption название этапа
 * @param description описание этапа
 * @param photos список фото, сделанных на этапе
 */
@Serializable
data class StageDTO(
    val stageCodeName: String,
    val caption: String = "",
    val description: String = "",
    val photos: List<PhotoDTO>
)

/**
 * Класс данных для описания фотографии
 * @param photoCodeName кодовое название фотографии
 * @param caption заголовок фотографии
 * @param description описание фотографии
 * @param imageFileName имя файла изображения
 */
@Serializable
data class PhotoDTO(
    val photoCodeName: String,
    val caption: String = "",
    val description: String = "",
    val imageFileName: String
)
