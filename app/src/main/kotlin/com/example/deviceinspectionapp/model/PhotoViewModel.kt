package com.example.deviceinspectionapp.model

import PoverkaAdapter
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class PhotoViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    var photoIdx: Int?
        get() = savedStateHandle["photoIdx"]
        set(value) { savedStateHandle["photoIdx"] = value }

    var stageIdx: Int?
        get() = savedStateHandle["stageIdx"]
        set(value) { savedStateHandle["stageIdx"] = value }
}
