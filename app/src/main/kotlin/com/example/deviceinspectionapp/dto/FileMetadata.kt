package com.example.deviceinspectionapp.dto

import kotlinx.serialization.Serializable

@Serializable
data class FileMetadata(
    val fileName: String,
    val lastModified: Long
)
