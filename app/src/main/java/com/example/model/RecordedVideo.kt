package com.example.model

import android.net.Uri

data class RecordedVideo(
    val uri: Uri,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val resolution: String,
    val timestamp: Long,
    val isHlg: Boolean
)
