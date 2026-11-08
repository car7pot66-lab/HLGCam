package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CameraLens
import com.example.model.CameraSettings
import com.example.model.CameraTelemetry
import com.example.model.VideoResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("HLG Pro Camera", appName)
    }

    @Test
    fun `verify 4to3 uncropped resolutions list`() {
        val resolutions = VideoResolution.values()
        assertEquals(4, resolutions.size)
        assertEquals(4096, resolutions[0].width)
        assertEquals(3072, resolutions[0].height)
        assertEquals(4000, resolutions[1].width)
        assertEquals(3000, resolutions[1].height)
        assertEquals(2880, resolutions[2].width)
        assertEquals(2160, resolutions[2].height)
        assertEquals(1920, resolutions[3].width)
        assertEquals(1440, resolutions[3].height)

        resolutions.forEach { res ->
            assertTrue(res.is4to3)
            assertEquals("4:3", res.aspectRatioLabel)
        }
    }

    @Test
    fun `verify xiaomi 12s ultra sensor definitions`() {
        assertEquals("Sony IMX586", CameraLens.ULTRAWIDE.sensorName)
        assertEquals("Sony IMX989", CameraLens.MAIN.sensorName)
        assertEquals(0.5f, CameraLens.ULTRAWIDE.zoomFactor)
        assertEquals(1.0f, CameraLens.MAIN.zoomFactor)
        assertTrue(CameraLens.ULTRAWIDE.defaultLens)
    }

    @Test
    fun `verify live telemetry formatting`() {
        val telemetry = CameraTelemetry(
            iso = 160,
            wbKelvin = 5400,
            fps = 30.0f,
            bitrateMbps = 85.5f,
            isLocked = true,
            isRecording = true
        )
        assertEquals("ISO 160", telemetry.formattedIso)
        assertEquals("WB 5400K", telemetry.formattedWb)
        assertEquals("30.0 FPS", telemetry.formattedFps)
        assertEquals("85.5 Mbps", telemetry.formattedBitrate)
        assertTrue(telemetry.isLocked)
        assertTrue(telemetry.isRecording)
    }

    @Test
    fun `verify camera default settings`() {
        val settings = CameraSettings()
        assertEquals(VideoResolution.RES_4096_3072, settings.resolution)
        assertEquals(CameraLens.ULTRAWIDE, settings.lens)
        assertTrue(settings.is10BitHlgEnabled)
        assertTrue(settings.isExposureDotsEnabled)
        assertTrue(settings.isHistogramEnabled)
        assertTrue(settings.lockWbAndIsoOnRecord)
    }
}
