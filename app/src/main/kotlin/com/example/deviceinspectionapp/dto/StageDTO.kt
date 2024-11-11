import kotlinx.serialization.Serializable

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
    val caption: String,
    val photos: List<PhotoDTO>,
    var isCompleted: Boolean = false
)
