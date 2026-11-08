package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.camera.CameraManagerHelper
import com.example.model.CameraLens
import com.example.model.CameraSettings
import com.example.model.VideoResolution
import com.example.ui.components.CameraControls
import com.example.ui.components.ExposureDotsOverlay
import com.example.ui.components.HistogramView
import com.example.ui.components.LensSelector
import com.example.ui.components.LiveTelemetryHud
import com.example.ui.components.RecordedVideosDialog
import com.example.ui.components.VideoQualityMenu
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    if (!hasCameraPermission) {
        PermissionRequiredScreen(
            onRequestPermission = {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                )
            }
        )
        return
    }

    // Camera settings state - initialized to 4096x3072 uncropped 4:3, Sony IMX586 UltraWide default
    var settings by remember {
        mutableStateOf(
            CameraSettings(
                resolution = VideoResolution.RES_4096_3072,
                lens = CameraLens.ULTRAWIDE,
                is10BitHlgEnabled = true,
                isExposureDotsEnabled = true,
                isHistogramEnabled = true,
                lockWbAndIsoOnRecord = true,
                isAudioEnabled = hasAudioPermission
            )
        )
    }

    var isSettingsSheetOpen by remember { mutableStateOf(false) }
    var isGalleryOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val cameraHelper = remember { CameraManagerHelper(context, lifecycleOwner) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    val telemetry by cameraHelper.telemetry.collectAsState()
    val histogramData by cameraHelper.histogram.collectAsState()
    val exposureDotsData by cameraHelper.exposureDots.collectAsState()
    val recordingDurationSec by cameraHelper.recordingDurationSec.collectAsState()
    val recordedVideos by cameraHelper.recordedVideos.collectAsState()

    DisposableEffect(cameraHelper) {
        onDispose {
            cameraHelper.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_screen_root")
    ) {
        // 4:3 Viewfinder Container (Uncropped aspect ratio 4:3 - Widest & Highest FOV)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            // Uncropped 4:3 Aspect Ratio Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f) // Vertical 4:3 sensor layout, or 4:3 landscape
                    .align(Alignment.Center)
                    .background(Color(0xFF0A0D14))
            ) {
                // 1. CameraX PreviewView
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FIT_CENTER
                            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                            previewViewRef = this
                            cameraHelper.startCamera(surfaceProvider, settings)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 2. Exposure Dots Overlay (Yellow for Overexposed, Red for Underexposed)
                if (settings.isExposureDotsEnabled) {
                    ExposureDotsOverlay(
                        dotsData = exposureDotsData,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 3. Sony IMX Sensor & 4:3 Tag in viewfinder corner
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .background(Color(0x99000000), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "4:3 SENSOR • FULL FOV",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF80D8FF)
                    )
                }
            }
        }

        // 4. Live Telemetry HUD at Top (ISO, WB, FPS, Bitrate in tiny, non-bold text)
        LiveTelemetryHud(
            telemetry = telemetry,
            settings = settings,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )

        // 5. Real-Time RGB + Luma Histogram (Top Right)
        AnimatedVisibility(
            visible = settings.isHistogramEnabled,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 70.dp, end = 12.dp)
        ) {
            HistogramView(data = histogramData)
        }

        // 6. Center Controls: Lens Selector (0.5x IMX586 / 1.0x IMX989)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        ) {
            LensSelector(
                selectedLens = settings.lens,
                onLensSelected = { newLens ->
                    settings = settings.copy(lens = newLens)
                    previewViewRef?.let { pv ->
                        cameraHelper.switchLens(newLens, pv.surfaceProvider)
                    }
                }
            )
        }

        // 7. Bottom Camera Controls (Shutter, Settings, Quick Toggles, Library)
        CameraControls(
            isRecording = telemetry.isRecording,
            recordingDurationSec = recordingDurationSec,
            settings = settings,
            is3aLocked = telemetry.isLocked,
            onRecordToggle = {
                if (telemetry.isRecording) {
                    cameraHelper.stopRecording()
                } else {
                    cameraHelper.startRecording { video ->
                        Toast.makeText(
                            context,
                            "10-bit HLG Video saved: ${video.resolution}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onOpenSettings = { isSettingsSheetOpen = true },
            onToggleDots = {
                settings = settings.copy(isExposureDotsEnabled = !settings.isExposureDotsEnabled)
            },
            onToggleHistogram = {
                settings = settings.copy(isHistogramEnabled = !settings.isHistogramEnabled)
            },
            onToggle3aLock = {
                cameraHelper.lock3A(!telemetry.isLocked)
            },
            onOpenGallery = { isGalleryOpen = true },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 8. Video Quality Submenu (Bottom Sheet)
        if (isSettingsSheetOpen) {
            VideoQualityMenu(
                settings = settings,
                sheetState = sheetState,
                onSettingsChanged = { newSettings ->
                    val oldSettings = settings
                    settings = newSettings
                    previewViewRef?.let { pv ->
                        cameraHelper.updateSettings(newSettings, pv.surfaceProvider)
                    }
                },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                        isSettingsSheetOpen = false
                    }
                }
            )
        }

        // 9. Recorded Videos Gallery Dialog
        if (isGalleryOpen) {
            RecordedVideosDialog(
                videos = recordedVideos,
                onDismiss = { isGalleryOpen = false }
            )
        }
    }
}

@Composable
private fun PermissionRequiredScreen(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color(0xFF80D8FF),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "HLG Pro Camera",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Professional 10-bit HLG uncropped 4:3 video capture for Xiaomi 12S Ultra. Requires camera and audio permissions to access Sony IMX586 and IMX989 sensors.",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = Color(0xFF90A4AE)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.testTag("grant_permission_button")
            ) {
                Text(
                    text = "Grant Camera Permissions",
                    fontSize = 14.sp,
                    color = Color.Black
                )
            }
        }
    }
}
