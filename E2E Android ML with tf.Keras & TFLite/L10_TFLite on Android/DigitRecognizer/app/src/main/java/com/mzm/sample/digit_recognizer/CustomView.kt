package com.mzm.sample.digit_recognizer

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * A custom view that allows the user to draw the digits
 * Note we are using a black background for the canvas and white stroke
 * to better match the MNIST training data
 */
class CustomView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val path = Path()
    private lateinit var canvas: Canvas
    private lateinit var bitmap: Bitmap

    private var currX = 0f
    private var currY = 0f

    // Paint for drawing digit
    private val paint = Paint().apply {
        isAntiAlias = true          // smooth out edges of drawing
        isDither = true
        color = Color.WHITE         // set stroke color to black
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 24f

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)  // set canvas background to black
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touch_start(x, y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                touch_move(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                touch_up()
                invalidate()
            }
        }
        return true
    }

    private fun touch_start(x: Float, y: Float) {
        path.reset()
        path.moveTo(x, y)
        this.currX = x
        this.currY = y
    }

    private fun touch_move(x: Float, y: Float) {
        val dx = Math.abs(x - this.currX)
        val dy = Math.abs(y - this.currY)
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            path.quadTo(this.currX, this.currY, (x + this.currX) / 2, (y + this.currY) / 2)
            this.currX = x
            this.currY = y
        }
    }

    private fun touch_up() {
        path.lineTo(currX, currY)
        canvas.drawPath(path, paint)
        path.reset()
    }

    fun reset() {
        path.reset()
        bitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)
        invalidate()
    }

    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        draw(canvas)
        return bitmap
    }

    companion object {
        private val TOUCH_TOLERANCE = 4f
    }
}
