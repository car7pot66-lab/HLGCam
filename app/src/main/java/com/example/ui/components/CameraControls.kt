package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraSettings

@Composable
fun CameraControls(
    isRecording: Boolean,
    recordingDurationSec: Long,
    settings: CameraSettings,
    is3aLocked: Boolean,
    onRecordToggle: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleDots: () -> Unit,
    onToggleHistogram: () -> Unit,
    onToggle3aLock: () -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Recording Timer Indicator
        AnimatedVisibility(visible = isRecording) {
            Row(
                modifier = Modifier
                    .background(Color(0xD9B71C1C), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .scale(pulseScale)
                        .background(Color.White, shape = CircleShape)
                )
                val mins = recordingDurationSec / 60
                val secs = recordingDurationSec % 60
                Text(
                    text = "REC %02d:%02d • 10-BIT HLG".format(mins, secs),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom action bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quick Action 1: Gallery
            ControlIconButton(
                icon = Icons.Default.PhotoLibrary,
                label = "Library",
                testTag = "gallery_button",
                onClick = onOpenGallery
            )

            // Quick Action 2: Exposure Dots quick toggle
            ControlIconButton(
                icon = Icons.Default.Warning,
                label = "Dots",
                isActive = settings.isExposureDotsEnabled,
                activeColor = Color(0xFFFFD600),
                testTag = "quick_toggle_dots",
                onClick = onToggleDots
            )

            // Main Shutter / Record Button
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .border(3.dp, Color.White, CircleShape)
                    .padding(6.dp)
                    .clickable { onRecordToggle() }
                    .testTag("record_shutter_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    // Recording stop square
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFFF1744), RoundedCornerShape(4.dp))
                    )
                } else {
                    // Idle record circle
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFFF1744), CircleShape)
                    )
                }
            }

            // Quick Action 3: Histogram quick toggle
            ControlIconButton(
                icon = Icons.Default.ShowChart,
                label = "Graph",
                isActive = settings.isHistogramEnabled,
                activeColor = Color(0xFF69F0AE),
                testTag = "quick_toggle_histogram",
                onClick = onToggleHistogram
            )

            // Quick Action 4: Settings / Resolution Submenu
            ControlIconButton(
                icon = Icons.Default.Tune,
                label = "Settings",
                testTag = "quality_settings_button",
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun ControlIconButton(
    icon: ImageVector,
    label: String,
    testTag: String,
    isActive: Boolean = false,
    activeColor: Color = Color(0xFF80D8FF),
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(
                    color = if (isActive) Color(0xFF263238) else Color(0x9910141D),
                    shape = CircleShape
                )
                .border(
                    width = if (isActive) 1.5.dp else 0.5.dp,
                    color = if (isActive) activeColor else Color(0x33FFFFFF),
                    shape = CircleShape
                )
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeColor else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            color = if (isActive) activeColor else Color(0xFFB0BEC5)
        )
    }
}
