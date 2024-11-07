import android.net.Uri
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
    val caption: String = "",
    val description: String = "",
    var imageFileName: String,

    @Serializable(with = UriSerializer::class) // Применяем сериализатор для Uri
    var thumbnailUri: Uri? = null
)