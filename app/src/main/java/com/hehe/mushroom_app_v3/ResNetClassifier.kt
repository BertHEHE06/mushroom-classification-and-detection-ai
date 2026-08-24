package com.hehe.mushroom_app_v3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class ClassificationResult(
    val binaryLabel: String,
    val binaryConf: Float,
    val speciesLabel: String,
    val speciesConf: Float,
)

class ResNetClassifier(context: Context) {

    private val interpreter: Interpreter
    private val inputSize = 224
    private val labels: Array<String>
    private val BINARY_THRESHOLD  = 0.70f
    private val SPECIES_THRESHOLD = 0.70f
    private val OUTPUT_IDX_SPECIES = 0
    private val OUTPUT_IDX_BINARY  = 1

    init {
        val modelBytes = context.assets.open("resnet_mushroom_v3.tflite").use { it.readBytes() }
        val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size).apply {
            order(ByteOrder.nativeOrder())
            put(modelBytes)
            rewind()
        }
        interpreter = Interpreter(modelBuffer)

        val jsonStr = context.assets.open("class_indices.json").bufferedReader().use { it.readText() }
        val jsonObj = JSONObject(jsonStr)
        labels = Array(jsonObj.length()) { "" }
        val keys = jsonObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val idx = jsonObj.getInt(key)
            if (idx < labels.size) labels[idx] = key
        }
    }

    private fun padToSquare(bitmap: Bitmap, targetSize: Int): Bitmap {
        val width   = bitmap.width
        val height  = bitmap.height
        val maxSide = maxOf(width, height)

        val squareBitmap = Bitmap.createBitmap(maxSide, maxSide, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(squareBitmap)
        canvas.drawColor(Color.BLACK)

        val left = (maxSide - width)  / 2f
        val top  = (maxSide - height) / 2f
        canvas.drawBitmap(bitmap, left, top, null)

        return Bitmap.createScaledBitmap(squareBitmap, targetSize, targetSize, true)
    }

    fun classify(bitmap: Bitmap): ClassificationResult {
        val resized = padToSquare(bitmap, inputSize)

        val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (px in pixels) {
            val r = ((px shr 16) and 0xFF).toFloat()
            val g = ((px shr 8)  and 0xFF).toFloat()
            val b = (px          and 0xFF).toFloat()
            inputBuffer.putFloat(b - 103.939f)
            inputBuffer.putFloat(g - 116.779f)
            inputBuffer.putFloat(r - 123.68f)
        }
        inputBuffer.rewind()
        if (resized != bitmap) resized.recycle()

        val binaryOutput  = Array(1) { FloatArray(1) }
        val speciesOutput = Array(1) { FloatArray(labels.size) }

        val outputs = mapOf(
            OUTPUT_IDX_SPECIES to speciesOutput,
            OUTPUT_IDX_BINARY  to binaryOutput
        )
        interpreter.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)

        //SPECIES CLASSIFICATION
        val speciesProbs = speciesOutput[0]
        val bestIdx      = speciesProbs.indices.maxByOrNull { speciesProbs[it] } ?: 0
        val speciesConf  = speciesProbs[bestIdx]
        val speciesLabel = if (speciesConf >= SPECIES_THRESHOLD)
            labels.getOrElse(bestIdx) { "Unknown" }
        else
            "Unknown"


        //BINARY CLASSIFICATION (POISONOUS / EDIBLE)
        val probBiner = binaryOutput[0][0]
        val binaryConf = if (probBiner > 0.5f) probBiner else 1f - probBiner

        val binaryLabel = when {
            binaryConf < BINARY_THRESHOLD -> "UNKNOWN"
            probBiner > 0.5f              -> "POISONOUS"
            else                          -> "EDIBLE"
        }
        Log.d(
            "ResNet",
            "Result: $binaryLabel " +
                    "(${String.format("%.1f", binaryConf * 100)}%) | " +
                    "$speciesLabel (${String.format("%.1f", speciesConf * 100)}%)"
        )

        return ClassificationResult(
            binaryLabel = binaryLabel,
            binaryConf  = if (binaryLabel == "UNKNOWN") 0f else binaryConf,
            speciesLabel = speciesLabel,
            speciesConf  = speciesConf
        )
    }

    fun close() = interpreter.close()
}