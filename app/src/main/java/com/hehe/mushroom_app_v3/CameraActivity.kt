package com.hehe.mushroom_app_v3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlay: BoundingBoxOverlay
    private lateinit var btnCapture: ImageButton

    private lateinit var detector: YoloDetector

    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalysis: ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null
    private var isClosing = false

    private val frameLock = Any()
    private var lastBitmap: Bitmap? = null
    private var lastResult: DetectionResult? = null

    companion object {
        private const val CAMERA_PERMISSION_CODE = 1001
        const val EXTRA_IMAGE_PATH = "extra_image_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView = findViewById(R.id.previewView)
        overlay     = findViewById(R.id.boundingOverlay)
        btnCapture  = findViewById(R.id.btnCapture)

        detector       = YoloDetector(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
        imageCapture   = ImageCapture.Builder().build()

        btnCapture.setOnClickListener {
            val (bitmap, detection) = synchronized(frameLock) {
                val bmp = lastBitmap?.copy(lastBitmap!!.config ?: Bitmap.Config.ARGB_8888, false)
                Pair(bmp, lastResult)
            }

            if (bitmap == null) {
                Toast.makeText(this, "No mushroom detected. Please point the camera at a mushroom.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnCapture.isEnabled = false
            cameraExecutor.execute {
                try {
                    val cacheFile = File(cacheDir, "captured_mushroom.jpg")
                    FileOutputStream(cacheFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    bitmap.recycle()
                    val intent = Intent(this, MainActivity::class.java).apply {
                        putExtra(EXTRA_IMAGE_PATH, cacheFile.absolutePath)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

                        // Kirim bounding box kalau YOLO sudah detect
                        if (detection != null) {
                            putExtra("box_left",   detection.boundingBox.left)
                            putExtra("box_top",    detection.boundingBox.top)
                            putExtra("box_right",  detection.boundingBox.right)
                            putExtra("box_bottom", detection.boundingBox.bottom)
                            putExtra("box_conf",   detection.confidence)
                        }
                    }

                    runOnUiThread {
                        btnCapture.isEnabled = true
                        startActivity(intent)
                        finish()
                    }
                } catch (e: Exception) {
                    bitmap.recycle()
                    runOnUiThread {
                        btnCapture.isEnabled = true
                        Toast.makeText(this, "Failed to save photo: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            preview.surfaceProvider = previewView.surfaceProvider

            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis?.setAnalyzer(cameraExecutor) { imageProxy ->
                if (isClosing) { imageProxy.close(); return@setAnalyzer }
                processFrame(imageProxy)
            }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis!!,
                imageCapture!!
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processFrame(imageProxy: ImageProxy) {
        if (isClosing) { imageProxy.close(); return }

        try {
            val bitmap = imageProxy.toBitmap() ?: run { imageProxy.close(); return }
            val rotated = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())
            if (rotated !== bitmap) bitmap.recycle()

            if (isClosing) { rotated.recycle(); imageProxy.close(); return }

            val result = detector.detect(rotated)

            synchronized(frameLock) {
                val old = lastBitmap
                lastBitmap = rotated.copy(rotated.config ?: Bitmap.Config.ARGB_8888, false)
                lastResult = result
                old?.recycle()
            }
            rotated.recycle()

            runOnUiThread {
                if (!isClosing) {
                    if (result != null) {
                        overlay.setBox(
                            result.boundingBox,
                            "Mushroom ${String.format("%.0f", result.confidence * 100)}%",
                            false,
                            lastBitmap!!.width.toFloat(),
                            lastBitmap!!.height.toFloat()
                        )
                    } else {
                        overlay.clear()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        imageProxy.close()
    }

    private fun rotateBitmap(bitmap: Bitmap, angle: Float): Bitmap {
        if (angle == 0f) return bitmap
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        isClosing = true
        imageAnalysis?.clearAnalyzer()
        cameraExecutor.shutdownNow()
        detector.close()
        synchronized(frameLock) {
            lastBitmap?.recycle()
            lastBitmap = null
        }
        super.onDestroy()
    }
}