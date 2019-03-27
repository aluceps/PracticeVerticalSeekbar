package me.aluceps.practiceverticalseekbar

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
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

interface OnChangeListener {
    fun progress(value: Int)
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

    private var barColor = 0
    private var barValueColor = 0
    private var barLabelValueColor = 0
    private var barBalloonColor = 0

    private var thumbResOn: Drawable? = null
    private var thumbResOff: Drawable? = null

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

    private val thumb by lazy {
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

    private val balloonText by lazy {
        Paint().createText(barLabelMaxValue).apply {
            textAlign = Paint.Align.CENTER
        }
    }

    private val balloonBack by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = barBalloonColor
        }
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

    private val thumbOff by lazy {
        thumbResOff?.let { getBitmap(it) }
    }

    private val thumbOn by lazy {
        thumbResOn?.let { getBitmap(it) }
    }

    private var currentThumbY = 0
    private var currentThumbValue = 0

    private var myCanvas: Canvas? = null
    private var isTouched = false

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
        typedArray?.getColor(R.styleable.ValueBar_bar_balloon_color, Color.GREEN)?.let { barBalloonColor = it }
        typedArray?.getDrawable(R.styleable.ValueBar_bar_thumb_on)?.let { thumbResOn = it }
        typedArray?.getDrawable(R.styleable.ValueBar_bar_thumb_off)?.let { thumbResOff = it }
        typedArray?.recycle()

        Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    invalidate()
                }
            }, 10, 10)
        }
    }

    private var listener: OnChangeListener? = null
    fun setupListener(listener: OnChangeListener) {
        this.listener = listener
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
        drawMinValue(canvas)
        drawMaxValue(canvas)
        drawThumb(canvas)
        drawBalloon(canvas)
        myCanvas = canvas
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> isTouched = true
            MotionEvent.ACTION_MOVE -> when {
                barInfo.stopY <= event.y && barInfo.startY >= event.y -> {
                    currentThumbY = event.y.toInt()
                    val progress = (barInfo.length + paddingTop - currentThumbY) * 100 / barInfo.length
                    currentThumbValue = Math.ceil(barLabelMaxValue.toDouble() / 100 * progress).toInt()
                    listener?.progress(currentThumbValue)
                }
                barInfo.stopY > event.y -> {
                    currentThumbY = barInfo.stopY
                    val progress = (barInfo.length + paddingTop - currentThumbY) * 100 / barInfo.length
                    currentThumbValue = Math.ceil(barLabelMaxValue.toDouble() / 100 * progress).toInt()
                    listener?.progress(currentThumbValue)
                }
                barInfo.startY < event.y -> {
                    currentThumbY = barInfo.startY
                    val progress = (barInfo.length + paddingTop - currentThumbY) * 100 / barInfo.length
                    currentThumbValue = Math.ceil(barLabelMaxValue.toDouble() / 100 * progress).toInt()
                    listener?.progress(currentThumbValue)
                }
            }
            MotionEvent.ACTION_UP -> isTouched = false
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

    private fun drawThumb(canvas: Canvas) {
        if (currentThumbY == 0) currentThumbY = barInfo.startY
        val x = barInfo.startX
        val y = currentThumbY
        val thumbDrawable = if (isTouched) thumbOn else thumbOff
        thumbDrawable?.let { bitmap ->
            val rect = Rect(0, 0, bitmap.width, bitmap.height)
            val dest = Rect(x - bitmap.width, y - bitmap.height, x + bitmap.width, y + bitmap.height)
            canvas.drawBitmap(bitmap, rect, dest, Paint())
        } ?: canvas.drawCircle(x.toFloat(), y.toFloat(), radius, thumb)
    }

    private fun drawBalloon(canvas: Canvas) {
        if (currentThumbY == 0) currentThumbY = barInfo.startY
        val textRect = getRect(balloonText, currentThumbValue)
        val textValue = currentThumbValue.toString()

        // balloon 表示位置を計算
        val (x, y) = if (isTouched) {
            balloonText.textSize = barLabelValueSize * 2
            Pair(
                barInfo.startX - TEXT_MARGIN * 3,
                (currentThumbY - textRect.height()).toFloat()
            )
        } else {
            balloonText.textSize = barLabelValueSize
            Pair(
                barInfo.startX - TEXT_MARGIN * 12,
                (currentThumbY + textRect.height() / 2).toFloat()
            )
        }

        // balloon の背景のサイズを計算
        val textWidth = balloonText.measureText(textValue)
        val left = (textRect.left - textWidth / 2 - TEXT_MARGIN * 5)
        val top = textRect.top.toFloat() - TEXT_MARGIN * 3
        val right = (textRect.right - textWidth / 2 + TEXT_MARGIN * 5)
        val bottom = textRect.bottom.toFloat() + TEXT_MARGIN * 3

        // offset を変更して balloonBack の表示位置を変更
        val balloonBackRect = RectF(left, top, right, bottom)
        balloonBackRect.offset(x, y)

        // balloonBack は balloonText の背景なので先に描画
        canvas.drawRoundRect(balloonBackRect, 60f, 60f, balloonBack)
        canvas.drawText(textValue, x, y, balloonText)
    }

    private fun getRect(paint: Paint, value: Int): Rect = Rect().apply {
        val text = value.toString()
        paint.getTextBounds(text, 0, text.length, this)
    }

    private fun getBitmap(target: Drawable): Bitmap? = when (target) {
        is VectorDrawable -> {
            val bitmap = Bitmap.createBitmap(
                target.intrinsicWidth,
                target.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val c = Canvas(bitmap)
            target.setBounds(0, 0, c.width, c.height)
            target.draw(c)
            bitmap
        }
        else -> null
    }

    private fun debugLog(message: String) {
        Log.d("ValueBar", message)
    }

    companion object {
        private const val TEXT_MARGIN = 8f
    }
}