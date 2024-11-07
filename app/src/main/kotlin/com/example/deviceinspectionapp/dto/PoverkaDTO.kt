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