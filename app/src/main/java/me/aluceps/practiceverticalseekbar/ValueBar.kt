package me.aluceps.practiceverticalseekbar

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.support.v4.content.res.ResourcesCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import java.util.*
import kotlin.math.abs

data class BarInfo(
    val startX: Int,
    val startY: Int,
    val stopX: Int,
    val stopY: Int,
    val length: Int
)

interface OnChangeListener {
    fun progress(value: Int)
    fun onDown()
    fun onUp()
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
    private var barBalloonColor = 0

    private var currentThumbY = 0
    private var currentThumbValue = 0

    private var isTouched = false

    private var thumbResOnUp: Drawable? = null
    private var thumbResOnMove: Drawable? = null
    private var myCanvas: Canvas? = null

    private val transparent by lazy {
        ResourcesCompat.getColor(resources, android.R.color.transparent, null)
    }

    private val baseBar by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = transparent
            strokeWidth = barHeight
        }
    }

    private val point by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = transparent
            strokeWidth = barHeight
        }
    }

    private val thumb by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
            color = barValueColor
            textAlign = Paint.Align.CENTER
        }
    }

    private val balloonBack by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = barBalloonColor
        }
    }

    private val balloonBackBottom by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL_AND_STROKE
            color = barBalloonColor
        }
    }

    private val balloonBackBottomPath by lazy {
        Path().apply {
            moveTo(0f, 00f)
            lineTo(60f, 00f)
            lineTo(30f, 40f)
            close()
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

    private val thumbOnUp by lazy { thumbResOnUp?.let { getBitmap(it) } }
    private val thumbOnMove by lazy { thumbResOnMove?.let { getBitmap(it) } }

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
        typedArray?.getColor(R.styleable.ValueBar_bar_balloon_color, Color.GREEN)?.let { barBalloonColor = it }
        typedArray?.getDrawable(R.styleable.ValueBar_bar_thumb_on_up)?.let { thumbResOnUp = it }
        typedArray?.getDrawable(R.styleable.ValueBar_bar_thumb_on_move)?.let { thumbResOnMove = it }
        typedArray?.recycle()

        Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    post { invalidate() }
                }
            }, 10, 10)
        }
    }

    fun reset() {
        currentThumbY = barInfo.startY
        currentThumbValue = 0
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
        color = transparent
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
            MotionEvent.ACTION_DOWN -> {
                isTouched = true
                startFadeInAnimation()
                listener?.onDown()
            }
            MotionEvent.ACTION_MOVE -> when {
                barInfo.stopY <= event.y && barInfo.startY >= event.y -> {
                    currentThumbY = event.y.toInt()
                    currentThumbValue = getProgressValue(currentThumbY, barLabelMaxValue)
                    listener?.progress(currentThumbValue)
                }
                barInfo.stopY > event.y -> {
                    currentThumbY = barInfo.stopY
                    currentThumbValue = getProgressValue(currentThumbY, barLabelMaxValue)
                    listener?.progress(currentThumbValue)
                }
                barInfo.startY < event.y -> {
                    currentThumbY = barInfo.startY
                    currentThumbValue = getProgressValue(currentThumbY, barLabelMaxValue)
                    listener?.progress(currentThumbValue)
                }
            }
            MotionEvent.ACTION_UP -> {
                isTouched = false
                startFadeOutAnimation()
                listener?.onUp()
            }
            MotionEvent.ACTION_CANCEL -> Unit
        }
        return true
    }

    private fun startFadeInAnimation() {
        ValueAnimator().apply {
            setIntValues(transparent, barColor)
            setEvaluator(ArgbEvaluator())
            addUpdateListener { anim ->
                val color = anim.animatedValue as Int
                baseBar.color = color
                point.color = color
                minValue.color = color
                maxValue.color = color
            }
            interpolator = AccelerateInterpolator()
            duration = FADE_IN_DURATION
        }.start()
    }

    private fun startFadeOutAnimation() {
        ValueAnimator().apply {
            setIntValues(barColor, transparent)
            setEvaluator(ArgbEvaluator())
            addUpdateListener { anim ->
                val color = anim.animatedValue as Int
                baseBar.color = color
                point.color = color
                minValue.color = color
                maxValue.color = color
            }
            interpolator = AccelerateInterpolator()
            duration = FADE_OUT_DURATION
        }.start()
    }

    private fun getProgressValue(value: Int, max: Int): Int {
        val percent = (barInfo.length + paddingTop - value) * 100 / barInfo.length
        return Math.ceil(max.toDouble() / 100 * percent).toInt()
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
        val thumbDrawable = if (isTouched) thumbOnMove else thumbOnUp
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

        // balloon の表示位置を計算
        val (x, y) = if (isTouched) {
            balloonText.textSize = barLabelValueSize * 2
            Pair(
                barInfo.startX - TEXT_MARGIN * 3,
                currentThumbY - textRect.height() - TEXT_MARGIN * 3
            )
        } else {
            balloonText.textSize = barLabelValueSize
            Pair(
                barInfo.startX - TEXT_MARGIN * 12,
                (currentThumbY + textRect.height() / 2).toFloat()
            )
        }

        // balloon の背景のサイズを計算
        // textValue の 1 の位が 1 の時だけ textRect.right が極端に小さくなるの
        // left と right は正負が異なるだけで値はほぼ同じだったので実際に使う時は
        // left の絶対値を使用している
        val textWidth = balloonText.measureText(textValue)
        val left = textRect.left - textWidth / 2 - TEXT_MARGIN * 6
        val top = textRect.top - TEXT_MARGIN * 3
        val right = textRect.right - textWidth / 2 + TEXT_MARGIN * 6
        val bottom = textRect.bottom + TEXT_MARGIN * 3

        if (isTouched) {
            val movePath = Path().apply { addPath(balloonBackBottomPath, x - TEXT_MARGIN, y + TEXT_MARGIN / 3) }
            canvas.drawPath(movePath, balloonBackBottom)
        }

        // offset を変更して balloonBack の表示位置を変更
        val balloonBackRect = RectF(left, top, abs(left), bottom)
        balloonBackRect.offset(x, y)

        // balloonBack は balloonText の背景なので先に描画
        canvas.drawRoundRect(balloonBackRect, BALLOON_RADIUS, BALLOON_RADIUS, balloonBack)
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

    companion object {
        private const val TEXT_MARGIN = 8f
        private const val BALLOON_RADIUS = 60f
        val FADE_IN_DURATION = 150L
        val FADE_OUT_DURATION = 300L
    }
}