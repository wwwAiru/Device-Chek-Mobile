import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

class DeviceCheckViewModel : ViewModel() {

    private val _currentPhotoIndex = MutableLiveData<Int>()
    val currentPhotoIndex: LiveData<Int> get() = _currentPhotoIndex

    private val _uuid = MutableLiveData<String>()
    val uuid: LiveData<String> get() = _uuid

    private val _currentStageCodeName = MutableLiveData<String>()
    val currentStageCodeName: LiveData<String> get() = _currentStageCodeName

    private val _currentPhotoCodeName = MutableLiveData<String>()
    val currentPhotoCodeName: LiveData<String> get() = _currentPhotoCodeName

    private val _originalPhotoUri = MutableLiveData<Uri>()
    val originalPhotoUri: LiveData<Uri> get() = _originalPhotoUri

    private val _originalPhotoFile = MutableLiveData<File>()
    val originalPhotoFile: LiveData<File> get() = _originalPhotoFile

    private val _thumbnailUris = MutableLiveData<Map<String, Uri>>()
    val thumbnailUris: LiveData<Map<String, Uri>> get() = _thumbnailUris

    fun setPhotoIndex(index: Int) {
        _currentPhotoIndex.value = index
    }

    fun setUuid(uuid: String) {
        _uuid.value = uuid
    }

    fun setStage(stageCodeName: String) {
        _currentStageCodeName.value = stageCodeName
    }

    fun setPhoto(photoCodeName: String) {
        _currentPhotoCodeName.value = photoCodeName
    }

    fun setOriginalPhotoUri(uri: Uri) {
        _originalPhotoUri.value = uri
    }

    fun setOriginalPhotoFile(file: File) {
        _originalPhotoFile.value = file
    }

    fun updatePhotoUri(stageCodeName: String, photoCodeName: String, thumbnailUri: Uri) {
        val key = "$stageCodeName-$photoCodeName"
        val currentMap = _thumbnailUris.value ?: emptyMap()

        if (currentMap[key] != thumbnailUri) {
            val updatedMap = currentMap.toMutableMap()
            updatedMap[key] = thumbnailUri
            _thumbnailUris.value = updatedMap
        }
    }


//    fun updateThumbnailUri(stageCodeName: String, photoCodeName: String, uri: Uri) {
//        val key = "$stageCodeName-$photoCodeName"
//        val updatedMap = _thumbnailUris.value?.toMutableMap() ?: mutableMapOf()
//        updatedMap[key] = uri
//        _thumbnailUris.value = updatedMap
//    }
}
