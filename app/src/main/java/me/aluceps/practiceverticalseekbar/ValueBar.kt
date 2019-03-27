package me.aluceps.practiceverticalseekbar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import java.util.*

data class BarInfo(
    val startX: Int,
    val startY: Int,
    val stopX: Int,
    val stopY: Int,
    val length: Int
)

/**
 * via: https://github.com/IntertechInc/android-custom-view-tutorial/blob/master/customviews/src/main/java/com/intertech/customviews/ValueBar.java
 */
class ValueBar @JvmOverloads constructor(
    context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        setup(context, attrs, defStyleAttr)
    }

    private var barValue = 0
    private var barLabelMinValue = 0
    private var barLabelMaxValue = 0

    private var barHeight = 0f
    private var barValueSize = 0f
    private var barLabelValueSize = 0f

    private var barColor = Color.WHITE
    private var barValueColor = Color.WHITE
    private var barLabelValueColor = Color.WHITE

    private val baseBar by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = barColor
            strokeWidth = barHeight
        }
    }

    private val point by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = barColor
            strokeWidth = barHeight
        }
    }

    private val cursor by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED
            strokeWidth = barHeight
        }
    }

    private val minValue by lazy {
        Paint().createText(barLabelMinValue)
    }

    private val maxValue by lazy {
        Paint().createText(barLabelMaxValue)
    }

    private val margin by lazy {
        TEXT_MARGIN * 10
    }

    private val radius by lazy {
        barHeight * 2
    }

    private val contentWidth by lazy {
        getRect(maxValue, barLabelMaxValue).width() + margin + barHeight
    }

    /**
     * この情報は onMeasure 以降に取得可能
     */
    private val barInfo by lazy {
        BarInfo(
            (measuredWidth - paddingRight - barHeight).toInt(),
            measuredHeight - paddingBottom,
            (measuredWidth - paddingRight - barHeight).toInt(),
            paddingTop,
            measuredHeight - paddingTop - paddingBottom
        )
    }

    private var cursorY = 0

    private var myCanvas: Canvas? = null

    private fun setup(context: Context?, attrs: AttributeSet?, defStyleAttr: Int = 0) {
        val typedArray = context?.obtainStyledAttributes(attrs, R.styleable.ValueBar, defStyleAttr, 0)
        typedArray?.getInt(R.styleable.ValueBar_bar_value, 0)?.let { barValue = it }
        typedArray?.getInt(R.styleable.ValueBar_bar_label_min_value, 0)?.let { barLabelMinValue = it }
        typedArray?.getInt(R.styleable.ValueBar_bar_label_max_value, 100)?.let { barLabelMaxValue = it }
        typedArray?.getDimension(R.styleable.ValueBar_bar_height, 0f)?.let { barHeight = it }
        typedArray?.getDimension(R.styleable.ValueBar_bar_value_size, 0f)?.let { barValueSize = it }
        typedArray?.getDimension(R.styleable.ValueBar_bar_label_value_size, 0f)?.let { barLabelValueSize = it }
        typedArray?.getColor(R.styleable.ValueBar_bar_color, Color.WHITE)?.let { barColor = it }
        typedArray?.getColor(R.styleable.ValueBar_bar_value_color, Color.WHITE)?.let { barValueColor = it }
        typedArray?.getColor(R.styleable.ValueBar_bar_label_value_color, Color.WHITE)?.let { barLabelValueColor = it }
        typedArray?.recycle()

        Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    invalidate()
                }
            }, 10, 10)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec))
    }

    private fun measureWidth(measureSpec: Int): Int {
        var size = paddingLeft + paddingRight
        size += contentWidth.toInt()
        return resolveSizeAndState(size, measureSpec, 0)
    }

    private fun measureHeight(measureSpec: Int): Int {
        var size = paddingTop + paddingBottom
        size += height - paddingTop - paddingBottom
        return resolveSizeAndState(size, measureSpec, 0)
    }

    private fun Paint.createText(value: Int): Paint = apply {
        isAntiAlias = true
        color = barLabelValueColor
        textSize = barLabelValueSize
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        measureText(value.toString())
    }

    override fun onDraw(canvas: Canvas?) {
        canvas ?: return
        drawBar(canvas)
        drawCursor(canvas)
        drawMinValue(canvas)
        drawMaxValue(canvas)
        myCanvas = canvas
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> Unit
            MotionEvent.ACTION_MOVE -> when {
                barInfo.stopY <= event.y && barInfo.startY >= event.y -> cursorY = event.y.toInt()
                barInfo.stopY > event.y -> cursorY = barInfo.stopY
                barInfo.startY < event.y -> cursorY = barInfo.startY
            }
            MotionEvent.ACTION_UP -> Unit
            MotionEvent.ACTION_CANCEL -> Unit
        }
        return true
    }

    private fun drawBar(canvas: Canvas) {
        val x = barInfo.startX.toFloat()
        val y1 = barInfo.startY.toFloat()
        val y2 = barInfo.stopY.toFloat()
        canvas.drawLine(x, y1, x, y2, baseBar)
        canvas.drawCircle(x, y1, radius, point)
        canvas.drawCircle(x, y2, radius, point)
    }

    private fun drawMinValue(canvas: Canvas) {
        val textRect = getRect(minValue, barLabelMinValue)
        val textValue = barLabelMinValue.toString()
        val x = barInfo.startX - margin - textRect.width()
        val y = (barInfo.startY + textRect.height() / 2).toFloat()
        canvas.drawText(textValue, x, y, minValue)
    }

    private fun drawMaxValue(canvas: Canvas) {
        val textRect = getRect(maxValue, barLabelMaxValue)
        val textValue = barLabelMaxValue.toString()
        val x = barInfo.stopX - margin - textRect.width()
        val y = (barInfo.stopY + textRect.height() / 2).toFloat()
        canvas.drawText(textValue, x, y, maxValue)
    }

    private fun drawCursor(canvas: Canvas) {
        if (cursorY == 0) cursorY = barInfo.startY
        val x = barInfo.startX.toFloat()
        canvas.drawCircle(x, cursorY.toFloat(), radius, cursor)
    }

    private fun getRect(paint: Paint, value: Int): Rect = Rect().apply {
        val text = value.toString()
        paint.getTextBounds(text, 0, text.length, this)
    }

    private fun debugLog(message: String) {
        Log.d("ValueBar", message)
    }

    companion object {
        private const val TEXT_MARGIN = 8f
    }
}