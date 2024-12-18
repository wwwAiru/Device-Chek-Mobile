import kotlinx.serialization.Serializable

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
    val caption: String,
    val imageFileName: String,
    var uri: String? = null
)


