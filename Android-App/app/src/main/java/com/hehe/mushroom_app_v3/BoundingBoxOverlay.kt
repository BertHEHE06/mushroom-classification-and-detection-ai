package com.hehe.mushroom_app_v3

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class BoundingBoxOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var boundingBox: RectF? = null
    private var label: String = ""
    private var isPoison: Boolean = false

    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }
    private val labelBgPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val labelTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 28f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    private var origW = 1f
    private var origH = 1f

    fun setBox(
        box: RectF?,
        label: String,
        isPoison: Boolean,
        origW: Float,
        origH: Float
    ) {
        this.boundingBox = box
        this.label = label
        this.isPoison = isPoison
        this.origW = origW
        this.origH = origH
        invalidate()
    }

    fun clear() {
        boundingBox = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val box = boundingBox ?: return

        val color = if (isPoison) Color.parseColor("#CC2020") else Color.parseColor("#4A7C3F")
        boxPaint.color = color
        labelBgPaint.color = color

        val viewW = width.toFloat()
        val viewH = height.toFloat()
        val imgAspect = origW / origH
        val viewAspect = viewW / viewH
        val renderedW: Float
        val renderedH: Float
        val offsetX: Float
        val offsetY: Float
        if (imgAspect > viewAspect) {
            renderedW = viewW
            renderedH = viewW / imgAspect
            offsetX = 0f
            offsetY = (viewH - renderedH) / 2f
        } else {
            renderedH = viewH
            renderedW = viewH * imgAspect
            offsetX = (viewW - renderedW) / 2f
            offsetY = 0f
        }
        val scaleX = renderedW / origW
        val scaleY = renderedH / origH
        val left   = offsetX + box.left   * scaleX
        val top    = offsetY + box.top    * scaleY
        val right  = offsetX + box.right  * scaleX
        val bottom = offsetY + box.bottom * scaleY
        canvas.drawRoundRect(left, top, right, bottom, 8f, 8f, boxPaint)
        val textBounds = Rect()
        labelTextPaint.getTextBounds(label, 0, label.length, textBounds)
        val labelW = textBounds.width() + 20f
        val labelH = textBounds.height() + 14f
        canvas.drawRoundRect(
            left, top,
            left + labelW, top + labelH,
            4f, 4f, labelBgPaint
        )
        canvas.drawText(label, left + 10f, top + labelH - 7f, labelTextPaint)
    }
}