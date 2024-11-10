import android.net.Uri
import androidx.lifecycle.ViewModel
import java.io.File

class DeviceCheckViewModel: ViewModel() {

    var currentPhotoIndex: Int = 0
        private set
    var uuid: String = ""
        private set
    var currentStageCodeName: String = ""
        private set
    var currentPhotoCodeName: String = ""
        private set
    var originalPhotoUri: Uri? = null
        private set
    var originalPhotoFile: File? = null
        private set
    private var _thumbnailUris: MutableMap<String, Uri> = mutableMapOf()
    val thumbnailUris: Map<String, Uri> get() = _thumbnailUris

    // Функции для установки значений
    fun setPhotoIndex(index: Int) {
        currentPhotoIndex = index
    }

    fun setUuid(uuid: String) {
        this.uuid = uuid
    }

    fun setStage(stageCodeName: String) {
        currentStageCodeName = stageCodeName
    }

    fun setPhoto(photoCodeName: String) {
        currentPhotoCodeName = photoCodeName
    }

    fun setOriginalPhotoUri(uri: Uri) {
        originalPhotoUri = uri
    }

    fun setOriginalPhotoFile(file: File) {
        originalPhotoFile = file
    }

    // Обновление URI фотографии
    fun updatePhotoUri(stageCodeName: String, photoCodeName: String, thumbnailUri: Uri) {
        val key = "$stageCodeName-$photoCodeName"
        if (_thumbnailUris[key] != thumbnailUri) {
            _thumbnailUris[key] = thumbnailUri
        }
    }

    // Удаление URI по ключу
    fun removeThumbnailUri(stageCodeName: String, photoCodeName: String) {
        val key = "$stageCodeName-$photoCodeName"
        _thumbnailUris.remove(key)
    }

    // Получение URI по ключу
    fun getThumbnailUri(stageCodeName: String, photoCodeName: String): Uri? {
        val key = "$stageCodeName-$photoCodeName"
        return _thumbnailUris[key]
    }
}
