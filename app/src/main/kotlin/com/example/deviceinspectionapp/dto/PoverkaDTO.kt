import kotlinx.serialization.Serializable

/**
 * Класс данных для описания процесса поверки
 * @param uuid уникальный идентификатор поверки
 * @param caption название поверки
 * @param description описание поверки
 * @param stages список этапов поверки
 */
@Serializable
data class PoverkaDTO(
    val uuid: String,
    val stages: List<StageDTO>
)

