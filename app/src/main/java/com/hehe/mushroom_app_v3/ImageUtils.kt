package com.hehe.mushroom_app_v3

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

fun ImageProxy.toBitmap(): Bitmap? {

    val yPlane = planes[0]
    val uPlane = planes[1]
    val vPlane = planes[2]

    val yBuffer = yPlane.buffer
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer

    val ySize = yBuffer.remaining()
    val nv21 = ByteArray(ySize + width * height / 2)
    val yRowStride    = yPlane.rowStride
    val yPixelStride  = yPlane.pixelStride

    if (yRowStride == width) {
        yBuffer.get(nv21, 0, ySize)
    } else {
        var pos = 0
        for (row in 0 until height) {
            yBuffer.position(row * yRowStride)
            yBuffer.get(nv21, pos, width)
            pos += width
        }
    }
    val vRowStride   = vPlane.rowStride
    val vPixelStride = vPlane.pixelStride
    val uRowStride   = uPlane.rowStride
    val uPixelStride = uPlane.pixelStride

    var pos = ySize
    for (row in 0 until height / 2) {
        for (col in 0 until width / 2) {
            val vIdx = row * vRowStride + col * vPixelStride
            val uIdx = row * uRowStride + col * uPixelStride

            if (vIdx < vBuffer.limit() && uIdx < uBuffer.limit()) {
                nv21[pos++] = vBuffer.get(vIdx)
                nv21[pos++] = uBuffer.get(uIdx)
            } else {
                nv21[pos++] = 0
                nv21[pos++] = 0
            }
        }
    }

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()

    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}