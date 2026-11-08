package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraSettings
import com.example.model.VideoResolution

/**
 * Video Quality Submenu inspired by Jetpack Camera App (JCA).
 * Provides uncropped 4:3 dimensions: 4096x3072, 4000x3000, 2880x2160, 1920x1440,
 * 10-bit HLG toggle, exposure dots toggle, histogram toggle, 3A lock toggle, and bitrate options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoQualityMenu(
    settings: CameraSettings,
    sheetState: SheetState,
    onSettingsChanged: (CameraSettings) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF10141D),
        contentColor = Color.White,
        modifier = modifier.testTag("video_quality_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Video Quality Settings",
                        tint = Color(0xFF80D8FF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Video Quality & Capture Settings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Xiaomi 12S Ultra • Sony IMX586 / IMX989",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF90A4AE)
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_quality_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Menu",
                        tint = Color(0xFFB0BEC5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 1: 4:3 Uncropped Dimensions Submenu
            SectionHeader(
                icon = Icons.Default.Videocam,
                title = "4:3 Uncropped Resolutions (No Crop - Widest FOV)"
            )
            Text(
                text = "Full sensor readout preserves 100% vertical & horizontal field of view without 16:9 cropping.",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF78909C),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            VideoResolution.values().forEach { res ->
                val isSelected = res == settings.resolution
                ResolutionOptionItem(
                    resolution = res,
                    isSelected = isSelected,
                    onClick = {
                        onSettingsChanged(settings.copy(resolution = res))
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Section 2: 10-bit HLG HDR Toggle
            SectionHeader(
                icon = Icons.Default.HighQuality,
                title = "Color Profile & Dynamic Range"
            )

            ToggleCard(
                title = "10-bit HLG (HEVC Main 10)",
                subtitle = "Hybrid Log-Gamma 10-bit high dynamic range color profile with wide color gamut.",
                checked = settings.is10BitHlgEnabled,
                testTag = "toggle_10bit_hlg",
                onCheckedChange = { checked ->
                    onSettingsChanged(settings.copy(is10BitHlgEnabled = checked))
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Section 3: Exposure Dots (Alternative to Zebra)
            SectionHeader(
                icon = Icons.Default.Warning,
                title = "Exposure Assist: Spot Dots Overlay"
            )

            ToggleCard(
                title = "Exposure Spot Dots (Zebra Alternative)",
                subtitle = "Yellow spots for overexposed areas (>95%). Red spots for underexposed areas (<5%).",
                checked = settings.isExposureDotsEnabled,
                testTag = "toggle_exposure_dots",
                onCheckedChange = { checked ->
                    onSettingsChanged(settings.copy(isExposureDotsEnabled = checked))
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Section 4: RGB + Luma Histogram
            SectionHeader(
                icon = Icons.Default.ShowChart,
                title = "Histogram Assist"
            )

            ToggleCard(
                title = "RGB + White Luma Histogram",
                subtitle = "Real-time 4-channel line graph: Red, Blue, Green, and White luminance curves.",
                checked = settings.isHistogramEnabled,
                testTag = "toggle_histogram",
                onCheckedChange = { checked ->
                    onSettingsChanged(settings.copy(isHistogramEnabled = checked))
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Section 5: White balance and ISO Lock on record
            SectionHeader(
                icon = Icons.Default.Lock,
                title = "Recording Exposure Lock"
            )

            ToggleCard(
                title = "Lock WB & ISO on Recording Start",
                subtitle = "Freezes Auto Exposure (ISO/shutter) and White Balance automatically when recording starts.",
                checked = settings.lockWbAndIsoOnRecord,
                testTag = "toggle_3a_lock",
                onCheckedChange = { checked ->
                    onSettingsChanged(settings.copy(lockWbAndIsoOnRecord = checked))
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Section 6: Target Bitrate
            SectionHeader(
                icon = Icons.Default.Tune,
                title = "Target Encoding Bitrate"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(50, 85, 120, 150).forEach { mbps ->
                    val isSelected = settings.targetBitrateMbps == mbps
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (isSelected) Color(0xFF263238) else Color(0xFF181F2C),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) Color(0xFF80D8FF) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                onSettingsChanged(settings.copy(targetBitrateMbps = mbps))
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$mbps M",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) Color(0xFF80D8FF) else Color(0xFFCFD8DC)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF80D8FF),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFECEFF1)
        )
    }
}

@Composable
private fun ResolutionOptionItem(
    resolution: VideoResolution,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) Color(0xFF1E2838) else Color(0xFF141923),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) Color(0xFF80D8FF) else Color(0x22FFFFFF),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("res_option_${resolution.width}x${resolution.height}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${resolution.width}×${resolution.height}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSelected) Color(0xFF80D8FF) else Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2E3846), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "4:3 Uncropped",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFA5D6A7)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = resolution.description,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF90A4AE)
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFF80D8FF),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141923), shape = RoundedCornerShape(12.dp))
            .border(0.5.dp, Color(0x22FFFFFF), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF78909C)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF80D8FF),
                    checkedTrackColor = Color(0xFF006064),
                    uncheckedThumbColor = Color(0xFF90A4AE),
                    uncheckedTrackColor = Color(0xFF263238)
                ),
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
