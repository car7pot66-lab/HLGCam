package com.example.model

enum class VideoResolution(
    val width: Int,
    val height: Int,
    val label: String,
    val description: String,
    val is4to3: Boolean = true
) {
    RES_4096_3072(4096, 3072, "4096×3072", "4:3 Full Sensor Uncropped (Max FOV)"),
    RES_4000_3000(4000, 3000, "4000×3000", "4:3 Full Sensor Uncropped (12MP Native Readout)"),
    RES_2880_2160(2880, 2160, "2880×2160", "4:3 Uncropped High Efficiency"),
    RES_1920_1440(1920, 1440, "1920×1440", "4:3 Uncropped High Speed Preview");

    val aspectRatioLabel: String get() = "4:3"
    val pixelCount: Long get() = width.toLong() * height.toLong()
}
