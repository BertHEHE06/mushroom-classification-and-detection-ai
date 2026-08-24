package com.hehe.mushroom_app_v3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class DetectionResult(
    val boundingBox: RectF,
    val confidence: Float
)

class YoloDetector(context: Context) {

    private val interpreter: Interpreter

    private val inputSize = 640
    private val confidenceThreshold = 0.7f
    private var isDetectorActive = true

    init {
        val modelBytes = context.assets.open("best_float16_v13.tflite").use { it.readBytes() }
        val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size).apply {
            order(ByteOrder.nativeOrder())
            put(modelBytes)
            rewind()
        }
        interpreter = Interpreter(modelBuffer)
        android.util.Log.d("YOLO_INPUT",  interpreter.getInputTensor(0).shape().contentToString())
        android.util.Log.d("YOLO_OUTPUT", interpreter.getOutputTensor(0).shape().contentToString())
    }

    @Synchronized
    fun detect(bitmap: Bitmap): DetectionResult? {

        if (!isDetectorActive) return null

        val origW = bitmap.width.toFloat()
        val origH = bitmap.height.toFloat()

        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (px in pixels) {
            inputBuffer.putFloat(((px shr 16) and 0xFF) / 255f)
            inputBuffer.putFloat(((px shr 8)  and 0xFF) / 255f)
            inputBuffer.putFloat((px          and 0xFF) / 255f)
        }

        resized.recycle()
        inputBuffer.rewind()

        val output = Array(1) { Array(5) { FloatArray(8400) } }
        interpreter.run(inputBuffer, output)

        var bestConf = confidenceThreshold
        var bestIdx  = -1

        for (i in 0 until 8400) {
            val conf = output[0][4][i]
            if (conf > bestConf) {
                bestConf = conf
                bestIdx  = i
            }
        }

        if (bestIdx == -1) return null

        val cx = output[0][0][bestIdx]
        val cy = output[0][1][bestIdx]
        val w  = output[0][2][bestIdx]
        val h  = output[0][3][bestIdx]

        val x1 = ((cx - w / 2f) * origW).coerceAtLeast(0f)
        val y1 = ((cy - h / 2f) * origH).coerceAtLeast(0f)
        val x2 = ((cx + w / 2f) * origW).coerceAtMost(origW)
        val y2 = ((cy + h / 2f) * origH).coerceAtMost(origH)

        return DetectionResult(RectF(x1, y1, x2, y2), bestConf)
    }

    @Synchronized
    fun close() {
        isDetectorActive = false
        interpreter.close()
    }
}