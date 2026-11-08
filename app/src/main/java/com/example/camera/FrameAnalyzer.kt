package com.example.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.model.ExposureDotsData
import com.example.model.HistogramData
import com.example.model.NormalizedDot
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

class FrameAnalyzer(
    private val onHistogramCalculated: (HistogramData) -> Unit,
    private val onExposureDotsCalculated: (ExposureDotsData) -> Unit,
    private val onFpsUpdated: (Float) -> Unit
) : ImageAnalysis.Analyzer {

    private var frameCount = 0
    private var lastFpsTimestamp = System.currentTimeMillis()
    private var currentFps = 30.0f

    // Temp buffers for histogram
    private val rBins = IntArray(HistogramData.BIN_COUNT)
    private val gBins = IntArray(HistogramData.BIN_COUNT)
    private val bBins = IntArray(HistogramData.BIN_COUNT)
    private val lumaBins = IntArray(HistogramData.BIN_COUNT)

    private val normR = FloatArray(HistogramData.BIN_COUNT)
    private val normG = FloatArray(HistogramData.BIN_COUNT)
    private val normB = FloatArray(HistogramData.BIN_COUNT)
    private val normLuma = FloatArray(HistogramData.BIN_COUNT)

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        frameCount++
        if (now - lastFpsTimestamp >= 500) {
            val deltaSec = (now - lastFpsTimestamp) / 1000f
            currentFps = frameCount / deltaSec
            frameCount = 0
            lastFpsTimestamp = now
            onFpsUpdated(currentFps)
        }

        try {
            val planes = image.planes
            if (planes.size < 3) {
                return
            }

            val yPlane = planes[0]
            val uPlane = planes[1]
            val vPlane = planes[2]

            val yBuffer: ByteBuffer = yPlane.buffer
            val uBuffer: ByteBuffer = uPlane.buffer
            val vBuffer: ByteBuffer = vPlane.buffer

            val width = image.width
            val height = image.height

            val yRowStride = yPlane.rowStride
            val yPixelStride = yPlane.pixelStride
            val uvRowStride = uPlane.rowStride
            val uvPixelStride = uPlane.pixelStride

            // Reset histogram counters
            rBins.fill(0)
            gBins.fill(0)
            bBins.fill(0)
            lumaBins.fill(0)

            val overexposedDots = ArrayList<NormalizedDot>(150)
            val underexposedDots = ArrayList<NormalizedDot>(150)

            // Step size to process efficiently
            val step = 6

            val yLimit = yBuffer.limit()
            val uLimit = uBuffer.limit()
            val vLimit = vBuffer.limit()

            for (y in 0 until height step step) {
                val yRowStart = y * yRowStride
                val uvRowStart = (y / 2) * uvRowStride

                for (x in 0 until width step step) {
                    val yIdx = yRowStart + x * yPixelStride
                    if (yIdx >= yLimit) continue

                    val yVal = (yBuffer.get(yIdx).toInt() and 0xFF)

                    val uvCol = (x / 2) * uvPixelStride
                    val uvIdx = uvRowStart + uvCol

                    val uVal = if (uvIdx < uLimit) (uBuffer.get(uvIdx).toInt() and 0xFF) - 128 else 0
                    val vVal = if (uvIdx < vLimit) (vBuffer.get(uvIdx).toInt() and 0xFF) - 128 else 0

                    // YUV to RGB approximation
                    val r = (yVal + 1.402f * vVal).toInt().coerceIn(0, 255)
                    val g = (yVal - 0.344136f * uVal - 0.714136f * vVal).toInt().coerceIn(0, 255)
                    val b = (yVal + 1.772f * uVal).toInt().coerceIn(0, 255)

                    // Bins index (0..63)
                    val rBin = (r * HistogramData.BIN_COUNT) / 256
                    val gBin = (g * HistogramData.BIN_COUNT) / 256
                    val bBin = (b * HistogramData.BIN_COUNT) / 256
                    val lumaBin = (yVal * HistogramData.BIN_COUNT) / 256

                    rBins[rBin]++
                    gBins[gBin]++
                    bBins[bBin]++
                    lumaBins[lumaBin]++

                    // Overexposure detection (> 242 / ~95%) -> Yellow spots
                    if (yVal > 242 && overexposedDots.size < 160) {
                        overexposedDots.add(NormalizedDot(x.toFloat() / width, y.toFloat() / height))
                    }
                    // Underexposure detection (< 15 / ~6%) -> Red spots
                    else if (yVal < 15 && underexposedDots.size < 160) {
                        underexposedDots.add(NormalizedDot(x.toFloat() / width, y.toFloat() / height))
                    }
                }
            }

            // Normalize bins
            var maxCount = 1
            for (i in 0 until HistogramData.BIN_COUNT) {
                maxCount = max(maxCount, max(rBins[i], max(gBins[i], max(bBins[i], lumaBins[i]))))
            }
            val invMax = 1f / maxCount
            for (i in 0 until HistogramData.BIN_COUNT) {
                normR[i] = rBins[i] * invMax
                normG[i] = gBins[i] * invMax
                normB[i] = bBins[i] * invMax
                normLuma[i] = lumaBins[i] * invMax
            }

            onHistogramCalculated(
                HistogramData(
                    red = normR.copyOf(),
                    green = normG.copyOf(),
                    blue = normB.copyOf(),
                    luma = normLuma.copyOf()
                )
            )

            onExposureDotsCalculated(
                ExposureDotsData(
                    overexposedDots = overexposedDots,
                    underexposedDots = underexposedDots
                )
            )

        } catch (e: Exception) {
            // Ignore frame analysis exceptions during camera restart
        } finally {
            image.close()
        }
    }
}
