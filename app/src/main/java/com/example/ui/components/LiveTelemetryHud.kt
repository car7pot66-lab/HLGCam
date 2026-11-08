package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraSettings
import com.example.model.CameraTelemetry

/**
 * Top Telemetry HUD displaying live ISO, WB, FPS, and Bitrate in tiny, non-bold text.
 * As requested: "include live values for iso value, white balance value, fps, bitrate in tiny text at the top of the preview screen. The text should not be bold."
 */
@Composable
fun LiveTelemetryHud(
    telemetry: CameraTelemetry,
    settings: CameraSettings,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("live_telemetry_hud"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Main telemetry row: ISO, WB, FPS, Bitrate in tiny, non-bold text
            Row(
                modifier = Modifier
                    .background(
                        color = Color(0xCC0A0D14),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ISO value
                TelemetryItem(
                    label = "ISO",
                    value = telemetry.iso.toString(),
                    testTag = "telemetry_iso"
                )

                DividerDot()

                // White balance value
                TelemetryItem(
                    label = "WB",
                    value = "${telemetry.wbKelvin}K",
                    testTag = "telemetry_wb"
                )

                DividerDot()

                // FPS
                TelemetryItem(
                    label = "FPS",
                    value = "%.1f".format(telemetry.fps),
                    testTag = "telemetry_fps"
                )

                DividerDot()

                // Bitrate
                TelemetryItem(
                    label = "BITRATE",
                    value = "%.1f Mbps".format(telemetry.bitrateMbps),
                    testTag = "telemetry_bitrate"
                )
            }

            // Secondary device status badges row (Sensor, HLG, Resolution, 3A Lock)
            Row(
                modifier = Modifier
                    .background(
                        color = Color(0x99000000),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sensor badge
                Text(
                    text = settings.lens.sensorName + " (${settings.lens.focalLabel})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF64B5F6)
                )

                Text(text = "•", fontSize = 10.sp, color = Color.Gray)

                // 10-bit HLG badge
                Text(
                    text = if (settings.is10BitHlgEnabled) "10-bit HLG" else "8-bit SDR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    color = if (settings.is10BitHlgEnabled) Color(0xFFFFD54F) else Color.White
                )

                Text(text = "•", fontSize = 10.sp, color = Color.Gray)

                // Uncropped 4:3 dimension
                Text(
                    text = "${settings.resolution.label} (4:3)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF81C784)
                )

                if (telemetry.isLocked) {
                    Text(text = "•", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = "3A LOCKED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFF5252)
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryItem(
    label: String,
    value: String,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.testTag(testTag)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF9E9E9E)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFFFFFFFF)
        )
    }
}

@Composable
private fun DividerDot() {
    Text(
        text = "│",
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF424242)
    )
}
