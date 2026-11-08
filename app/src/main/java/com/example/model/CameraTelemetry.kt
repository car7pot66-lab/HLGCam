package com.example.model

data class CameraTelemetry(
    val iso: Int = 100,
    val exposureTimeNs: Long = 0L,
    val wbKelvin: Int = 5500,
    val fps: Float = 30.0f,
    val bitrateMbps: Float = 85.0f,
    val isLocked: Boolean = false,
    val isRecording: Boolean = false,
    val dynamicRangeProfile: String = "10-bit HLG",
    val sensorName: String = "Sony IMX586 (UW)"
) {
    val formattedIso: String get() = "ISO $iso"
    val formattedWb: String get() = "WB ${wbKelvin}K"
    val formattedFps: String get() = "%.1f FPS".format(fps)
    val formattedBitrate: String get() = "%.1f Mbps".format(bitrateMbps)
}
