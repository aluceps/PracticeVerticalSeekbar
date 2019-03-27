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
            0 -> Horizontal
            1 -> Vertical
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

    private var barValue = 0
    private var barLabelMinValue = 0
    private var barLabelMaxValue = 0

    private var barHeight = 0f
    private var barValueSize = 0f
    private var barLabelValueSize = 0f

    private var barColor = Color.WHITE
    private var barValueColor = Color.WHITE
    private var barLabelValueColor = Color.WHITE

    private var barOrientation = Orientation.Vertical

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

    private val minValue by lazy {
        Paint().createText(barLabelMinValue)
    }

    private val maxValue by lazy {
        Paint().createText(barLabelMaxValue)
    }

    private val margin by lazy {
        TEXT_MARGIN * 5
    }

    private val radius by lazy {
        barHeight * 2
    }

    // この barCenter は x/y 軸からみたの距離なので
    // padding を足して padding 分の調整をしている
    private val barCenter
        get() = when (barOrientation) {
            Orientation.Horizontal -> (height - paddingTop - paddingBottom) / 2 + paddingTop
            Orientation.Vertical -> (width - paddingLeft - paddingRight) / 2 + paddingLeft
        }

    private val barLength
        get() = when (barOrientation) {
            Orientation.Horizontal -> width - paddingLeft - paddingRight
            Orientation.Vertical -> height - paddingTop - paddingBottom
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
        typedArray?.getInt(R.styleable.ValueBar_bar_orientation, 0)?.let {
            Orientation.createOrNull(it)?.let {
                debugLog("attrs: orientation=$it")
                barOrientation = it
            }
        }
        typedArray?.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        debugLog("onMeasure")
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec))
    }

    private fun measureWidth(measureSpec: Int): Int {
        debugLog("measureWidth")
        var size = paddingLeft + paddingRight

        when (barOrientation) {
            Orientation.Horizontal -> {
                size += barLength
                size += getRect(minValue, barLabelMinValue).width()
            }
            Orientation.Vertical -> {
                size += radius.toInt()
            }
        }
        size += getRect(maxValue, barLabelMaxValue).width()

        return resolveSizeAndState(size, measureSpec, 0)
    }

    private fun measureHeight(measureSpec: Int): Int {
        debugLog("measureHeight")
        var size = paddingTop + paddingBottom

        when (barOrientation) {
            Orientation.Horizontal -> {
                size += minValue.fontSpacing.toInt()
                size += Math.max(maxValue.fontSpacing, barHeight).toInt()
            }
            Orientation.Vertical -> {
                size += minValue.fontSpacing.toInt()
                size += maxValue.fontSpacing.toInt()
                size += barLength
            }
        }

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
        debugLog("onDraw")
        canvas ?: return
        drawBar(canvas)
        drawMinValue(canvas)
        drawMaxValue(canvas)
    }

    private fun drawBar(canvas: Canvas) {
        debugLog("drawBar")
        when (barOrientation) {
            Orientation.Horizontal -> {
                val startx = paddingLeft + margin
                val stopx = width - paddingRight - margin
                val starty = barCenter.toFloat()
                val stopy = barCenter.toFloat()
                canvas.drawLine(startx, starty, stopx, stopy, baseBar)
                canvas.drawCircle(startx, starty, radius, point)
                canvas.drawCircle(stopx, stopy, radius, point)
            }
            Orientation.Vertical -> {
                val rect = getRect(maxValue, barLabelMaxValue)
                val startx = width - paddingRight - barHeight
                val stopx = width - paddingRight - barHeight
                val starty = (height - paddingBottom - rect.height() / 2).toFloat()
                val stopy = (paddingTop + rect.height() / 2).toFloat()
                canvas.drawLine(startx, starty, stopx, stopy, baseBar)
                canvas.drawCircle(startx, starty, radius, point)
                canvas.drawCircle(stopx, stopy, radius, point)
            }
        }
    }

    private fun drawMinValue(canvas: Canvas) {
        debugLog("drawMinValue")
        val rect = getRect(minValue, barLabelMinValue)
        val text = barLabelMinValue.toString()

        val (x, y) = when (barOrientation) {
            Orientation.Horizontal -> Pair(
                paddingLeft + margin - rect.width() / 2,
                (barCenter - rect.height()).toFloat()
            )
            Orientation.Vertical -> Pair(
                width - paddingRight - barHeight - rect.width() - margin,
                (height - paddingBottom).toFloat()
            )
        }

        canvas.drawText(text, x, y, minValue)
    }

    private fun drawMaxValue(canvas: Canvas) {
        debugLog("drawMaxValue")
        val rect = getRect(maxValue, barLabelMaxValue)
        val text = barLabelMaxValue.toString()

        val (x, y) = when (barOrientation) {
            Orientation.Horizontal -> Pair(
                width - paddingRight - margin - rect.width() / 2,
                (barCenter - rect.height()).toFloat()
            )
            Orientation.Vertical -> Pair(
                width - paddingRight - barHeight - rect.width() - margin,
                (paddingTop + rect.height()).toFloat()
            )
        }

        canvas.drawText(text, x, y, maxValue)
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