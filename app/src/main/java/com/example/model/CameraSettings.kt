package com.example.model

data class CameraSettings(
    val resolution: VideoResolution = VideoResolution.RES_4096_3072,
    val lens: CameraLens = CameraLens.ULTRAWIDE,
    val is10BitHlgEnabled: Boolean = true,
    val isExposureDotsEnabled: Boolean = true,
    val isHistogramEnabled: Boolean = true,
    val lockWbAndIsoOnRecord: Boolean = true,
    val isAudioEnabled: Boolean = true,
    val targetBitrateMbps: Int = 85,
    val targetFps: Int = 30
)
