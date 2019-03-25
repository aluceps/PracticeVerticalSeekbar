package me.aluceps.practiceverticalseekbar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.SeekBar

enum class DirectionTo(val degree: Float) {
    Top(270f),
    Right(0f),
    Bottom(90f),
    Left(180f);

    companion object {
        fun createOrNull(index: Int): DirectionTo? = when (index) {
            Top.ordinal -> Top
            Right.ordinal -> Right
            Bottom.ordinal -> Bottom
            Left.ordinal -> Left
            else -> null
        }
    }
}

/**
 * via: http://android-note.open-memo.net/sub/seek_bar__vertical_seek_bar.html
 */
class VerticalSeekBar @JvmOverloads constructor(
    context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : SeekBar(context, attrs, defStyleAttr) {

    var direction = DirectionTo.Left

    init {
        val typedArray = context?.obtainStyledAttributes(attrs, R.styleable.VerticalSeekBar, defStyleAttr, 0)
        typedArray?.getInt(R.styleable.VerticalSeekBar_direction_to, DirectionTo.Left.ordinal)?.let {
            DirectionTo.createOrNull(it)?.let { direction = it }
        }
        typedArray?.getInt(R.styleable.VerticalSeekBar_max, 100)?.let {
            max = it
        }
        typedArray?.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = 0
        var height = 0

        when (direction) {
            DirectionTo.Top,
            DirectionTo.Bottom -> {
                width = MeasureSpec.getSize(heightMeasureSpec)
                height = MeasureSpec.getSize(widthMeasureSpec)
            }
            DirectionTo.Right,
            DirectionTo.Left -> {
                width = MeasureSpec.getSize(widthMeasureSpec)
                height = MeasureSpec.getSize(heightMeasureSpec)
            }
        }

        debugLog("onMeasure: width=$width height=$height")
        super.onMeasure(width, height)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        val (d, w, h) = when (direction) {
            DirectionTo.Right -> Triple(0f, 0, 0)
            DirectionTo.Bottom -> Triple(90f, 0, width * -1)
            DirectionTo.Left -> Triple(180f, height * -1, width * -1)
            DirectionTo.Top -> Triple(270f, height * -1, 0)
        }
        canvas?.rotate(d)
        canvas?.translate(w.toFloat(), h.toFloat())
        debugLog("onDraw: d=$d w=$w h=$h")
        super.onDraw(canvas)
    }

    private var onChangeListener: OnSeekBarChangeListener? = null

    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
        this.onChangeListener = l
    }

    private var lastProgress = 0

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                onChangeListener?.onStartTrackingTouch(this)
                isPressed = true
                isSelected = true
            }
            MotionEvent.ACTION_MOVE -> {
                var progress = when (direction) {
                    DirectionTo.Right -> (max * event.x / width).toInt()
                    DirectionTo.Bottom -> (max * event.y / height).toInt()
                    DirectionTo.Left -> max - (max * event.x / width).toInt()
                    DirectionTo.Top -> max - (max * event.y / height).toInt()
                }

                if (progress < 0) progress = 0
                if (progress > max) progress = max
                setProgress(progress)

                if (progress != lastProgress) {
                    lastProgress = progress
                    onChangeListener?.onProgressChanged(this, progress, true)
                }

                onSizeChanged(width, height, 0, 0)
                onChangeListener?.onProgressChanged(this, progress, true)
                isPressed = true
                isSelected = true

                debugLog("onTouchEvent: ACTION_MOVE: progress=$progress")
            }
            MotionEvent.ACTION_UP -> {
                onChangeListener?.onStopTrackingTouch(this)
                isPressed = false
                isSelected = false
            }
            MotionEvent.ACTION_CANCEL -> {
                super.onTouchEvent(event)
                isPressed = false
                isSelected = false
            }
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        var width = 0
        var height = 0
        var oldWidth = 0
        var oldHeight = 0

        when (direction) {
            DirectionTo.Top,
            DirectionTo.Bottom -> {
                width = h
                height = w
                oldWidth = oldh
                oldHeight = oldw
            }
            DirectionTo.Right,
            DirectionTo.Left -> {
                width = w
                height = h
                oldWidth = oldw
                oldHeight = oldh
            }
        }

        debugLog("onSizeChanged: width=$width height=$height oldWidth=$oldWidth oldHeight=$oldHeight")
        super.onSizeChanged(width, height, oldWidth, oldHeight)
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) Log.d("VerticalSeekBar", message)
    }
}
