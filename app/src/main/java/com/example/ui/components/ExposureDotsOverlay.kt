package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.example.model.ExposureDotsData

/**
 * Exposure Dots Overlay (Alternative to Zebra stripes).
 * Renders Yellow spots for overexposed areas and Red spots for underexposed areas.
 * As requested: "Add zebra overlay option. Instead of zebra, use dot's. Yellow spots over exposed. Red spots under exposed."
 */
@Composable
fun ExposureDotsOverlay(
    dotsData: ExposureDotsData,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("exposure_dots_overlay")
    ) {
        val w = size.width
        val h = size.height

        // 1. Draw Red spots for underexposed areas (< 5% luminance)
        for (dot in dotsData.underexposedDots) {
            val cx = dot.x * w
            val cy = dot.y * h

            // Inner solid red dot
            drawCircle(
                color = Color(0xFFFF1744),
                radius = 4.5f,
                center = Offset(cx, cy)
            )
            // Subtle dark ring for contrast on dark shadows
            drawCircle(
                color = Color(0x66000000),
                radius = 5.5f,
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
            )
        }

        // 2. Draw Yellow spots for overexposed areas (> 95% luminance / highlights)
        for (dot in dotsData.overexposedDots) {
            val cx = dot.x * w
            val cy = dot.y * h

            // Inner solid yellow dot
            drawCircle(
                color = Color(0xFFFFD600),
                radius = 4.5f,
                center = Offset(cx, cy)
            )
            // Subtle black border ring for contrast on bright highlights
            drawCircle(
                color = Color(0x88000000),
                radius = 5.5f,
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
            )
        }
    }
}
