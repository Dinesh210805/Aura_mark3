package com.aura.aura_mark3.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.graphics.PixelFormat
import android.media.ImageReader
import androidx.activity.result.ActivityResultLauncher
import androidx.core.graphics.createBitmap
import java.nio.ByteBuffer

class ScreenshotHelper(private val activity: Activity) {
    companion object {
        const val REQUEST_CODE_SCREENSHOT = 1001
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    // Modern Activity Result API method
    fun requestScreenshotPermission(launcher: ActivityResultLauncher<Intent>) {
        val projectionManager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = projectionManager.createScreenCaptureIntent()
        launcher.launch(intent)
    }

    // Legacy method for backward compatibility (deprecated)
    @Deprecated("Use requestScreenshotPermission(ActivityResultLauncher<Intent>) instead")
    fun requestScreenshotPermission() {
        val projectionManager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = projectionManager.createScreenCaptureIntent()
        activity.startActivityForResult(intent, REQUEST_CODE_SCREENSHOT)
    }

    fun onActivityResult(resultCode: Int, data: Intent?, onScreenshotReady: (Bitmap?) -> Unit) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            val projectionManager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            captureScreenshot(onScreenshotReady)
        } else {
            onScreenshotReady(null)
        }
    }

    fun handleScreenshotResult(result: androidx.activity.result.ActivityResult, onScreenshotReady: (Bitmap?) -> Unit) {
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val projectionManager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(result.resultCode, result.data!!)
            captureScreenshot(onScreenshotReady)
        } else {
            onScreenshotReady(null)
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun captureScreenshot(onScreenshotReady: (Bitmap?) -> Unit) {
        val windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Get screen size using non-deprecated methods
        val width: Int
        val height: Int
        val density: Int

        // IDE may warn this is unnecessary, but it's required for runtime compatibility
        // with different Android versions when the app is deployed to various devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            width = bounds.width()
            height = bounds.height()
            density = activity.resources.configuration.densityDpi
        } else {
            @Suppress("DEPRECATION")
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            width = metrics.widthPixels
            height = metrics.heightPixels
            density = metrics.densityDpi
        }

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "AURA_Screenshot",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        Handler(Looper.getMainLooper()).postDelayed({
            val image = imageReader?.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer: ByteBuffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width

                // Using KTX createBitmap extension function
                val bitmap = createBitmap(
                    width + rowPadding / pixelStride,
                    height, Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()
                onScreenshotReady(bitmap)
            } else {
                onScreenshotReady(null)
            }
            release()
        }, 500) // Wait for the image to be ready
    }

    private fun release() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
    }
}
