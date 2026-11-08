package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.example.model.CameraLens

/**
 * Lens Selector pill for switching between:
 * - Sony IMX586 (0.5x Ultra-Wide, uncropped 4:3 max FOV)
 * - Sony IMX989 (1.0x Main Sensor, 1.0" Type)
 */
@Composable
fun LensSelector(
    selectedLens: CameraLens,
    onLensSelected: (CameraLens) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0xCC10141D), shape = RoundedCornerShape(24.dp))
            .border(1.dp, Color(0x33FFFFFF), shape = RoundedCornerShape(24.dp))
            .padding(4.dp)
            .testTag("lens_selector"),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CameraLens.values().forEach { lens ->
            val isSelected = lens == selectedLens
            Box(
                modifier = Modifier
                    .background(
                        color = if (isSelected) Color(0xFF263238) else Color.Transparent,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) Color(0xFF80D8FF) else Color.Transparent,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onLensSelected(lens) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("lens_button_${lens.id}"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = lens.focalLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSelected) Color(0xFF80D8FF) else Color(0xFFB0BEC5)
                    )
                    Text(
                        text = lens.sensorName,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSelected) Color.White else Color(0xFF78909C)
                    )
                }
            }
        }
    }
}
