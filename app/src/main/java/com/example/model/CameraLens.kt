package com.example.model

enum class CameraLens(
    val id: String,
    val sensorName: String,
    val focalLabel: String,
    val zoomFactor: Float,
    val description: String,
    val defaultLens: Boolean = false
) {
    ULTRAWIDE(
        id = "ultrawide",
        sensorName = "Sony IMX586",
        focalLabel = "0.5× (13mm)",
        zoomFactor = 0.5f,
        description = "48MP 1/2.0\" Ultra-Wide • Widest & Highest FOV",
        defaultLens = true
    ),
    MAIN(
        id = "main",
        sensorName = "Sony IMX989",
        focalLabel = "1.0× (23mm)",
        zoomFactor = 1.0f,
        description = "50MP 1.0\" Type Large Sensor • Cinema Dynamic Range",
        defaultLens = false
    )
}
