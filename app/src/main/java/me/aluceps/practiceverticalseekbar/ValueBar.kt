package me.aluceps.practiceverticalseekbar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View

enum class Orientation {
    Horizontal,
    Vertical;

    companion object {
        fun createOrNull(value: Int): Orientation? = when (value) {
            Horizontal.ordinal -> Horizontal
            Vertical.ordinal -> Vertical
            else -> null
        }
    }
}

/**
 * via: https://github.com/IntertechInc/android-custom-view-tutorial/blob/master/customviews/src/main/java/com/intertech/customviews/ValueBar.java
 */
class ValueBar @JvmOverloads constructor(
    context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        setup(context, attrs, defStyleAttr)
    }

    private val barCenter
        get() = (height - paddingTop - paddingBottom) / 2 + paddingTop

    private val barLength
        get() = width - paddingLeft - paddingRight

    private var barValue = 0
    private var barLabelMinValue = 0
    private var barLabelMaxValue = 0

    private var barHeight = 0f
    private var barValueSize = 0f
    private var barLabelValueSize = 0f

    private var barColor = Color.WHITE
    private var barValueColor = Color.WHITE
    private var barLabelValueColor = Color.WHITE

    private var barOrientation = Orientation.Horizontal

    private val baseBar by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = barColor
        }
    }

    private val point by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = barColor
            strokeWidth = barHeight
        }
    }

    private val minValue by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = barLabelValueSize
            color = barLabelValueColor
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            measureText(barLabelMinValue.toString())
        }
    }

    private val maxValue by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = barLabelValueSize
            color = barLabelValueColor
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            measureText(barLabelMaxValue.toString())
        }
    }

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
        typedArray?.getInt(R.styleable.ValueBar_bar_orientation, Orientation.Horizontal.ordinal)
            ?.let { Orientation.createOrNull(it)?.let { barOrientation = it } }
        typedArray?.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        debugLog("onMeasure")
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec))
    }

    private fun measureWidth(measureSpec: Int): Int {
        debugLog("measureWidth")
        var size = paddingLeft + paddingRight
        val bounds = Rect()

        val minText = minValue.toString()
        minValue.getTextBounds(minText, 0, minText.length, bounds)
        size += bounds.width()

        val maxText = maxValue.toString()
        maxValue.getTextBounds(maxText, 0, maxText.length, bounds)
        size += bounds.width()

        return resolveSizeAndState(size, measureSpec, 0)
    }

    private fun measureHeight(measureSpec: Int): Int {
        debugLog("measureHeight")
        var size = paddingTop + paddingBottom
        size += minValue.fontSpacing.toInt()

        val maxValueTextSpacing = maxValue.fontSpacing
        size += Math.max(maxValueTextSpacing, barHeight).toInt()

        return resolveSizeAndState(size, measureSpec, 0)
    }

    override fun onDraw(canvas: Canvas?) {
        debugLog("onDraw")
        canvas ?: return
        drawBar(canvas)
        drawMinValue(canvas)
        drawMaxValue(canvas)
    }

    private fun drawBar(canvas: Canvas) {
        debugLog("drawBar")
        val halfBarHeight = barHeight / 2
        val minRect = getRect(minValue, barLabelMinValue.toString())
        val maxRect = getRect(maxValue, barLabelMaxValue.toString())

        val top = barCenter - halfBarHeight
        val bottom = barCenter + halfBarHeight
        val left = paddingLeft + minRect.width() + TEXT_MARGIN * 3
        val right = width - paddingRight - maxRect.width() - TEXT_MARGIN * 2
        val rect = RectF(left, top, right, bottom)

        debugLog("drawBar: rect=$rect barCenter=$barCenter halfBarHeight=$halfBarHeight")
        canvas.drawRoundRect(rect, halfBarHeight, halfBarHeight, baseBar)
        canvas.drawCircle(left, barCenter.toFloat(), barHeight * 2, point)
        canvas.drawCircle(right, barCenter.toFloat(), barHeight * 2, point)
    }

    private fun drawMinValue(canvas: Canvas) {
        debugLog("drawMinValue")
        val text = barLabelMinValue.toString()
        val rect = getRect(minValue, barLabelMinValue.toString())

        val x = paddingLeft.toFloat()
        val y = (barCenter + rect.height() / 2).toFloat()

        canvas.drawText(text, x, y, minValue)
    }

    private fun drawMaxValue(canvas: Canvas) {
        debugLog("drawMaxValue")
        val text = barLabelMaxValue.toString()
        val rect = getRect(maxValue, barLabelMaxValue.toString())

        val x = (width - paddingRight - rect.width()).toFloat()
        val y = (barCenter + rect.height() / 2).toFloat()

        canvas.drawText(text, x, y, maxValue)
    }

    private fun getRect(paint: Paint, text: String): Rect = Rect().apply {
        paint.getTextBounds(text, 0, text.length, this)
    }

    private fun debugLog(message: String) {
        Log.d("ValueBar", message)
    }

    companion object {
        private const val TEXT_MARGIN = 8f
    }
}