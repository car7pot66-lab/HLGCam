package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HistogramData

/**
 * Real-time RGB + White Luminance Histogram.
 * Draws Red, Green, Blue, and White lines representing live frame color distributions and luminance.
 */
@Composable
fun HistogramView(
    data: HistogramData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 150.dp, height = 75.dp)
            .background(Color(0xCC000000), shape = RoundedCornerShape(8.dp))
            .border(1.dp, Color(0x33FFFFFF), shape = RoundedCornerShape(8.dp))
            .padding(4.dp)
            .testTag("histogram_view")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with legend
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "HISTOGRAM",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFAAAAAA)
                )
                Spacer(modifier = Modifier.weight(1f))
                LegendDot(color = Color(0xFFFF5252), label = "R")
                Spacer(modifier = Modifier.width(4.dp))
                LegendDot(color = Color(0xFF69F0AE), label = "G")
                Spacer(modifier = Modifier.width(4.dp))
                LegendDot(color = Color(0xFF448AFF), label = "B")
                Spacer(modifier = Modifier.width(4.dp))
                LegendDot(color = Color(0xFFFFFFFF), label = "Y")
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Canvas drawing the 4 lines: Red, Green, Blue, White (Luma)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 2.dp, vertical = 2.dp)
            ) {
                val w = size.width
                val h = size.height
                val count = HistogramData.BIN_COUNT
                val dx = w / (count - 1).toFloat()

                // Draw background grid lines (25%, 50%, 75%)
                for (ratio in listOf(0.25f, 0.5f, 0.7f)) {
                    val y = h * ratio
                    drawLine(
                        color = Color(0x22FFFFFF),
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                // Helper to create path from normalized bins
                fun createChannelPath(bins: FloatArray): Path {
                    val path = Path()
                    for (i in 0 until count) {
                        val x = i * dx
                        val y = h - (bins.getOrElse(i) { 0f }.coerceIn(0f, 1f) * (h - 2f)) - 1f
                        if (i == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    return path
                }

                // Draw Red line
                drawPath(
                    path = createChannelPath(data.red),
                    color = Color(0xFFFF4040),
                    style = Stroke(width = 1.8f)
                )

                // Draw Green line
                drawPath(
                    path = createChannelPath(data.green),
                    color = Color(0xFF00E676),
                    style = Stroke(width = 1.8f)
                )

                // Draw Blue line
                drawPath(
                    path = createChannelPath(data.blue),
                    color = Color(0xFF2979FF),
                    style = Stroke(width = 1.8f)
                )

                // Draw White (Luminance) line
                drawPath(
                    path = createChannelPath(data.luma),
                    color = Color(0xFFFFFFFF),
                    style = Stroke(width = 2.0f)
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(color, shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = label,
            fontSize = 7.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFFCCCCCC)
        )
    }
}
