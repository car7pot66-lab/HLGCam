package com.example.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.model.CameraLens
import com.example.model.CameraSettings
import com.example.model.CameraTelemetry
import com.example.model.ExposureDotsData
import com.example.model.HistogramData
import com.example.model.RecordedVideo
import com.example.model.VideoResolution
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalCamera2Interop::class)
class CameraManagerHelper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private val tag = "CameraManagerHelper"

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    // State flows
    private val _telemetry = MutableStateFlow(CameraTelemetry())
    val telemetry: StateFlow<CameraTelemetry> = _telemetry.asStateFlow()

    private val _histogram = MutableStateFlow(HistogramData())
    val histogram: StateFlow<HistogramData> = _histogram.asStateFlow()

    private val _exposureDots = MutableStateFlow(ExposureDotsData())
    val exposureDots: StateFlow<ExposureDotsData> = _exposureDots.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0L)
    val recordingDurationSec: StateFlow<Long> = _recordingDurationSec.asStateFlow()

    private val _recordedVideos = MutableStateFlow<List<RecordedVideo>>(emptyList())
    val recordedVideos: StateFlow<List<RecordedVideo>> = _recordedVideos.asStateFlow()

    private var currentSettings = CameraSettings()
    private var recordingStartTime = 0L

    init {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                cameraProvider = providerFuture.get()
            } catch (e: Exception) {
                Log.e(tag, "Failed to get ProcessCameraProvider", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Start camera session with the specified surface provider and settings
     */
    fun startCamera(
        surfaceProvider: Preview.SurfaceProvider,
        settings: CameraSettings,
        onReady: () -> Unit = {}
    ) {
        currentSettings = settings
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider
            bindCameraUseCases(provider, surfaceProvider, settings)
            onReady()
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("RestrictedApi")
    private fun bindCameraUseCases(
        provider: ProcessCameraProvider,
        surfaceProvider: Preview.SurfaceProvider,
        settings: CameraSettings
    ) {
        try {
            provider.unbindAll()

            // 1. Target resolution 4:3 uncropped strategy
            val targetSize = Size(settings.resolution.width, settings.resolution.height)
            val resolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    ResolutionStrategy(
                        targetSize,
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()

            // 2. Build Preview use case with Camera2 interop for live telemetry
            val previewBuilder = Preview.Builder()
                .setResolutionSelector(resolutionSelector)

            val camera2PreviewExtender = Camera2Interop.Extender(previewBuilder)
            camera2PreviewExtender.setSessionCaptureCallback(object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    val iso = result.get(CaptureResult.SENSOR_SENSITIVITY) ?: 100
                    val expTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: 0L

                    // Extract color temperature (WB Kelvin approximation)
                    val gains = result.get(CaptureResult.COLOR_CORRECTION_GAINS)
                    val kelvin = if (gains != null && gains.red > 0f) {
                        // Approximate CCT from R/B gain ratio
                        val ratio = gains.blue / gains.red
                        val approxKelvin = (3000 + (ratio * 1800)).toInt().coerceIn(2000, 10000)
                        approxKelvin
                    } else {
                        5400
                    }

                    _telemetry.value = _telemetry.value.copy(
                        iso = iso,
                        exposureTimeNs = expTime,
                        wbKelvin = kelvin,
                        dynamicRangeProfile = if (settings.is10BitHlgEnabled) "10-bit HLG (HEVC)" else "SDR (8-bit)",
                        sensorName = if (settings.lens == CameraLens.ULTRAWIDE) "Sony IMX586 (0.5×)" else "Sony IMX989 (1.0×)"
                    )
                }
            })

            val preview = previewBuilder.build()
            preview.setSurfaceProvider(surfaceProvider)

            // 3. Dynamic Range - 10-bit HLG (HEVC Main 10)
            val dynamicRange = if (settings.is10BitHlgEnabled) {
                DynamicRange.HLG_10_BIT
            } else {
                DynamicRange.SDR
            }

            // 4. Build VideoCapture with Recorder
            val qualitySelector = QualitySelector.fromOrderedList(
                listOf(Quality.UHD, Quality.FHD, Quality.HD),
                FallbackStrategy.higherQualityOrLowerThan(Quality.UHD)
            )

            val recorder = Recorder.Builder()
                .setExecutor(cameraExecutor)
                .setQualitySelector(qualitySelector)
                .setTargetVideoEncodingBitRate(settings.targetBitrateMbps * 1_000_000)
                .build()

            val videoCaptureBuilder = VideoCapture.Builder(recorder)
                .setDynamicRange(dynamicRange)

            videoCapture = videoCaptureBuilder.build()

            // 5. ImageAnalysis for real-time RGB+Luma histogram & exposure dots
            val imageAnalysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(640, 480),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                            )
                        )
                        .build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()

            val analyzer = FrameAnalyzer(
                onHistogramCalculated = { data ->
                    if (currentSettings.isHistogramEnabled) {
                        _histogram.value = data
                    }
                },
                onExposureDotsCalculated = { dots ->
                    if (currentSettings.isExposureDotsEnabled) {
                        _exposureDots.value = dots
                    } else {
                        _exposureDots.value = ExposureDotsData()
                    }
                },
                onFpsUpdated = { fpsVal ->
                    _telemetry.value = _telemetry.value.copy(fps = fpsVal)
                }
            )

            imageAnalysis.setAnalyzer(analysisExecutor, analyzer)

            // 6. Camera Selector (Lens: Ultra-Wide Sony IMX586 or Main Sony IMX989)
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            // Bind to lifecycle
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture,
                imageAnalysis
            )

            // Apply zoom factor for Ultra-Wide (0.5x) or Main (1.0x) if supported
            applyLensZoom(settings.lens)

        } catch (e: Exception) {
            Log.e(tag, "Use case binding failed", e)
        }
    }

    private fun applyLensZoom(lens: CameraLens) {
        val cam = camera ?: return
        val zoomRatio = if (lens == CameraLens.ULTRAWIDE) 0.5f else 1.0f
        try {
            val zoomState = cam.cameraInfo.zoomState.value
            if (zoomState != null) {
                val minZoom = zoomState.minZoomRatio
                val maxZoom = zoomState.maxZoomRatio
                val targetZoom = zoomRatio.coerceIn(minZoom, maxZoom)
                cam.cameraControl.setZoomRatio(targetZoom)
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to set zoom ratio for lens $lens", e)
        }
    }

    /**
     * Start 10-bit HLG Video Recording
     * Locks White Balance and ISO if requested.
     */
    @SuppressLint("MissingPermission")
    fun startRecording(onVideoSaved: (RecordedVideo) -> Unit) {
        val vc = videoCapture ?: return
        if (activeRecording != null) return

        // 1. Lock WB and ISO before recording starts if requested
        if (currentSettings.lockWbAndIsoOnRecord) {
            lock3A(true)
        }

        val name = "HLG_${currentSettings.resolution.label}_" +
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/HLGProCamera")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues)
            .build()

        var pendingRecording = vc.output.prepareRecording(context, mediaStoreOutput)

        if (currentSettings.isAudioEnabled) {
            try {
                pendingRecording = pendingRecording.withAudioEnabled()
            } catch (e: SecurityException) {
                Log.w(tag, "Audio permission not granted, recording without audio", e)
            }
        }

        recordingStartTime = System.currentTimeMillis()

        activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    _telemetry.value = _telemetry.value.copy(
                        isRecording = true,
                        isLocked = currentSettings.lockWbAndIsoOnRecord
                    )
                }

                is VideoRecordEvent.Status -> {
                    val durationNanos = event.recordingStats.recordedDurationNanos
                    val durationSec = durationNanos / 1_000_000_000L
                    _recordingDurationSec.value = durationSec

                    val recordedBytes = event.recordingStats.numBytesRecorded
                    if (durationSec > 0) {
                        val liveBitrateMbps = (recordedBytes * 8f / durationSec) / 1_000_000f
                        _telemetry.value = _telemetry.value.copy(
                            bitrateMbps = liveBitrateMbps.coerceAtLeast(10f)
                        )
                    }
                }

                is VideoRecordEvent.Finalize -> {
                    val durationMs = System.currentTimeMillis() - recordingStartTime
                    _recordingDurationSec.value = 0L
                    _telemetry.value = _telemetry.value.copy(isRecording = false)

                    // Unlock WB and ISO once recording finishes
                    if (currentSettings.lockWbAndIsoOnRecord) {
                        lock3A(false)
                    }

                    if (!event.hasError()) {
                        val uri = event.outputResults.outputUri
                        val recorded = RecordedVideo(
                            uri = uri,
                            title = name,
                            durationMs = durationMs,
                            sizeBytes = event.recordingStats.numBytesRecorded,
                            resolution = currentSettings.resolution.label,
                            timestamp = System.currentTimeMillis(),
                            isHlg = currentSettings.is10BitHlgEnabled
                        )
                        _recordedVideos.value = listOf(recorded) + _recordedVideos.value
                        onVideoSaved(recorded)
                    } else {
                        Log.e(tag, "Video recording error: ${event.error}")
                    }
                    activeRecording = null
                }
            }
        }
    }

    /**
     * Stop active video recording
     */
    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    /**
     * Lock or unlock White Balance (AWB) and Auto Exposure (ISO / Shutter)
     */
    fun lock3A(locked: Boolean) {
        val cam = camera ?: return
        try {
            val control = cam.cameraControl
            val requestOptions = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, locked)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, locked)
                .build()

            Camera2CameraControl.from(control).setCaptureRequestOptions(requestOptions)
            _telemetry.value = _telemetry.value.copy(isLocked = locked)
        } catch (e: Exception) {
            Log.w(tag, "Failed to set 3A lock: $locked", e)
        }
    }

    fun switchLens(newLens: CameraLens, surfaceProvider: Preview.SurfaceProvider) {
        if (currentSettings.lens == newLens) return
        currentSettings = currentSettings.copy(lens = newLens)
        cameraProvider?.let { provider ->
            bindCameraUseCases(provider, surfaceProvider, currentSettings)
        }
    }

    fun updateResolution(newResolution: VideoResolution, surfaceProvider: Preview.SurfaceProvider) {
        if (currentSettings.resolution == newResolution) return
        currentSettings = currentSettings.copy(resolution = newResolution)
        cameraProvider?.let { provider ->
            bindCameraUseCases(provider, surfaceProvider, currentSettings)
        }
    }

    fun updateSettings(newSettings: CameraSettings, surfaceProvider: Preview.SurfaceProvider) {
        val needsRebind = currentSettings.resolution != newSettings.resolution ||
                currentSettings.lens != newSettings.lens ||
                currentSettings.is10BitHlgEnabled != newSettings.is10BitHlgEnabled

        currentSettings = newSettings
        if (needsRebind) {
            cameraProvider?.let { provider ->
                bindCameraUseCases(provider, surfaceProvider, currentSettings)
            }
        }
    }

    fun release() {
        cameraExecutor.shutdown()
        analysisExecutor.shutdown()
    }
}
