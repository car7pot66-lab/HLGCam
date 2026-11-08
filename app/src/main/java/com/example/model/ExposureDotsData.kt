package com.example.model

data class NormalizedDot(
    val x: Float, // 0f..1f
    val y: Float  // 0f..1f
)

data class ExposureDotsData(
    val overexposedDots: List<NormalizedDot> = emptyList(),  // Yellow spots (> 95%)
    val underexposedDots: List<NormalizedDot> = emptyList()  // Red spots (< 5%)
)
