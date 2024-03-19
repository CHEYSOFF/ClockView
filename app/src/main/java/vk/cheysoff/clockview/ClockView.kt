package vk.cheysoff.clockview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.icu.util.Calendar
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

    enum class Shape {
        ROUND,
        SQUARE
    }

    enum class NumeralsType {
        ARABIC,
        ROMAN,
        NONE
    }

    enum class UpdateType {
        EVERY_SECOND,
        EVERY_MINUTE,
        STATIC
    }

    private var currentHour: Int = 0
    private var currentMinute: Int = 0
    private var currentSecond: Int = 0

    private var shape: Shape = Shape.ROUND
    private var borderWidth: Float = 0f

    private var numeralsType: NumeralsType = NumeralsType.ARABIC
    private var numeralsFontSize: Float = resources.getDimension(R.dimen.default_numerals_font_size)
    private var numeralsColor: Int = Color.BLACK

    private var showSecondHand: Boolean = true
    private var showMinuteHand: Boolean = true
    private var showHourHand: Boolean = true

    private var secondHandColor: Int = Color.RED
    private var minuteHandColor: Int = Color.BLACK
    private var hourHandColor: Int = Color.BLACK

    private var secondHandLengthPercentage: Float = 0.8f
    private var minuteHandLengthPercentage: Float = 0.6f
    private var hourHandLengthPercentage: Float = 0.4f

    private var secondHandWidthPercentage: Float = 0.02f
    private var minuteHandWidthPercentage: Float = 0.03f
    private var hourHandWidthPercentage: Float = 0.05f

    private var backgroundColor: Int = Color.WHITE
    private var borderColor: Int = Color.BLACK

    private var updateType: UpdateType = UpdateType.EVERY_SECOND
    // TODO option for static

    private lateinit var timeRunnable: Runnable

    private val paint: Paint = Paint()

    private val handler = Handler(Looper.getMainLooper())

    init {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.ClockView,
            defStyleAttr,
            defStyleRes
        ).apply {
            try {
                shape = getInt(R.styleable.ClockView_shape, Shape.ROUND.ordinal).let { index ->
                    Shape.entries.toTypedArray().getOrElse(index) { Shape.ROUND }
                }
                borderWidth = getDimension(R.styleable.ClockView_borderWidth, 0f)

                numeralsType = getInt(
                    R.styleable.ClockView_numeralsType,
                    NumeralsType.ARABIC.ordinal
                ).let { index ->
                    NumeralsType.entries.toTypedArray().getOrElse(index) { NumeralsType.ARABIC }
                }
                numeralsFontSize =
                    getDimension(R.styleable.ClockView_numeralsFontSize, numeralsFontSize)
                numeralsColor = getColor(R.styleable.ClockView_numeralsColor, Color.BLACK)

                showSecondHand = getBoolean(R.styleable.ClockView_showSecondHand, true)
                showMinuteHand = getBoolean(R.styleable.ClockView_showMinuteHand, true)
                showHourHand = getBoolean(R.styleable.ClockView_showHourHand, true)


                secondHandColor = getColor(R.styleable.ClockView_secondHandColor, Color.RED)
                minuteHandColor = getColor(R.styleable.ClockView_minuteHandColor, Color.BLACK)
                hourHandColor = getColor(R.styleable.ClockView_hourHandColor, Color.BLACK)

                backgroundColor = getColor(R.styleable.ClockView_backgroundColor, Color.WHITE)
                borderColor = getColor(R.styleable.ClockView_borderColor, Color.BLACK)

                updateType = getInt(R.styleable.ClockView_updateType, 0).let { index ->
                    UpdateType.entries.toTypedArray().getOrElse(index) { UpdateType.EVERY_SECOND }
                }

                secondHandLengthPercentage =
                    getFloat(R.styleable.ClockView_secondHandLengthPercentage, 0.8f)
                secondHandLengthPercentage = secondHandLengthPercentage.coerceIn(0.1f, 1f)

                minuteHandLengthPercentage =
                    getFloat(R.styleable.ClockView_minuteHandLengthPercentage, 0.6f)
                minuteHandLengthPercentage = minuteHandLengthPercentage.coerceIn(0.1f, 1f)

                hourHandLengthPercentage =
                    getFloat(R.styleable.ClockView_hourHandLengthPercentage, 0.4f)
                hourHandLengthPercentage = hourHandLengthPercentage.coerceIn(0.1f, 1f)

                secondHandWidthPercentage =
                    getFloat(R.styleable.ClockView_secondHandWidthPercentage, 0.02f)
                secondHandWidthPercentage = secondHandWidthPercentage.coerceIn(0.01f, 0.2f)

                minuteHandWidthPercentage =
                    getFloat(R.styleable.ClockView_minuteHandWidthPercentage, 0.03f)
                minuteHandWidthPercentage = minuteHandWidthPercentage.coerceIn(0.01f, 0.2f)

                hourHandWidthPercentage =
                    getFloat(R.styleable.ClockView_hourHandWidthPercentage, 0.05f)
                hourHandWidthPercentage = hourHandWidthPercentage.coerceIn(0.01f, 0.2f)
            } finally {
                recycle()
            }
        }

        startCounting()
    }

    private fun startCounting() {
        timeRunnable = object : Runnable {
            override fun run() {
                val currentTime = Calendar.getInstance()

                currentHour = currentTime.get(Calendar.HOUR)
                currentMinute = currentTime.get(Calendar.MINUTE)
                currentSecond = currentTime.get(Calendar.SECOND)

                invalidate()

                if (updateType == UpdateType.STATIC) {
                    return
                }
                handler.postDelayed(
                    this, when (updateType) {
                        UpdateType.EVERY_MINUTE -> 60000
                        UpdateType.EVERY_SECOND -> 1000
                        else -> return
                    }
                )
            }
        }

        handler.post(timeRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(timeRunnable)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val size = when {
            widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY -> minOf(
                heightSize,
                widthSize
            )

            widthMode == MeasureSpec.EXACTLY -> widthSize
            heightMode == MeasureSpec.EXACTLY -> heightSize
            else -> minOf(heightSize, widthSize)
        }

        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(centerX, centerY)

        drawBackground(centerX, centerY, radius, canvas)
        drawBorder(centerX, centerY, radius, canvas)
        drawNumerals(centerX, centerY, radius, canvas)

        val availableRadius = radius - borderWidth

        drawHourHand(centerX, centerY, availableRadius, canvas)
        drawMinuteHand(centerX, centerY, availableRadius, canvas)
        drawSecondHand(centerX, centerY, availableRadius, canvas)

    }

    private fun drawHourHand(
        centerX: Float,
        centerY: Float,
        availableRadius: Float,
        canvas: Canvas
    ) {
        if (!showHourHand) {
            return
        }
        val hourDegree = (currentHour + currentMinute / 60f + currentSecond / 3600f) * 30f
        val hourStrokeWidth = availableRadius * hourHandWidthPercentage
        val hourHandLength = availableRadius * hourHandLengthPercentage
        drawHand(
            canvas,
            hourDegree,
            hourHandLength,
            hourHandColor,
            hourStrokeWidth,
            centerX,
            centerY,
            paint
        )
    }

    private fun drawMinuteHand(
        centerX: Float,
        centerY: Float,
        availableRadius: Float,
        canvas: Canvas
    ) {
        if (!showMinuteHand) {
            return
        }
        val minuteDegree = (currentMinute + currentSecond / 60f) * 6f
        val minuteStrokeWidth = availableRadius * minuteHandWidthPercentage
        val minuteHandLength = availableRadius * minuteHandLengthPercentage
        drawHand(
            canvas,
            minuteDegree,
            minuteHandLength,
            minuteHandColor,
            minuteStrokeWidth,
            centerX,
            centerY,
            paint
        )
    }

    private fun drawSecondHand(
        centerX: Float,
        centerY: Float,
        availableRadius: Float,
        canvas: Canvas
    ) {
        if (!showSecondHand) {
            return
        }
        val secondDegree = (currentSecond) * 6f
        val secondStrokeWidth = availableRadius * secondHandWidthPercentage
        val secondHandLength = availableRadius * secondHandLengthPercentage
        drawHand(
            canvas,
            secondDegree,
            secondHandLength,
            secondHandColor,
            secondStrokeWidth,
            centerX,
            centerY,
            paint
        )
    }

    private fun drawHand(
        canvas: Canvas,
        degree: Float,
        length: Float,
        color: Int,
        strokeWidth: Float,
        centerX: Float,
        centerY: Float,
        paint: Paint
    ) {
        paint.reset()
        paint.isAntiAlias = true
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = color
        paint.strokeWidth = strokeWidth

        val handRadians = Math.toRadians(degree.toDouble())
        val endX = centerX + sin(handRadians) * length
        val endY = centerY - cos(handRadians) * length

        canvas.drawLine(centerX, centerY, endX.toFloat(), endY.toFloat(), paint)
    }

    private fun drawNumerals(centerX: Float, centerY: Float, radius: Float, canvas: Canvas) {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = numeralsColor
        paint.textSize = numeralsFontSize
        paint.textAlign = Paint.Align.CENTER

        val numerals = when (numeralsType) {
            NumeralsType.ARABIC -> arabicNumerals
            NumeralsType.ROMAN -> romanNumerals
            NumeralsType.NONE -> return
        }

        val radiusCoefficient = (radius - borderWidth - numeralsFontSize)
        val numeralsOffset = numeralsFontSize / 2

        if (shape == Shape.SQUARE) {

            val borderWidthOffset = borderWidth
            val squareSide = (radius - borderWidth) * 2
            val sideNumeralsGap =
                (squareSide - numeralsFontSize * 3) / 4

            for (i in 1..3) {
                val x =
                    centerX - radius + borderWidthOffset + numeralsFontSize / 2 + sideNumeralsGap * i + numeralsFontSize * (i - 1)
                val y = centerY - radius + borderWidthOffset + numeralsFontSize * 1.5f
                canvas.drawText(numerals[(i + 9) % 12], x, y, paint)
            }

            for (i in 1..3) {
                val x = centerX + radius - borderWidthOffset - numeralsFontSize
                val y =
                    centerY - radius + borderWidthOffset + numeralsFontSize / 2 + sideNumeralsGap * i + numeralsFontSize * (i - 1)
                canvas.drawText(numerals[i], x, y, paint)
            }

            for (i in 1..3) {
                val x =
                    centerX + radius - borderWidthOffset - numeralsFontSize / 2 - sideNumeralsGap * i - numeralsFontSize * (i - 1)
                val y = centerY + radius - borderWidthOffset - numeralsFontSize
                canvas.drawText(numerals[i + 1], x, y, paint)
            }

            for (i in 1..3) {
                val x = centerX - radius + borderWidthOffset + numeralsFontSize
                val y =
                    centerY + radius - borderWidthOffset - numeralsFontSize / 2 - sideNumeralsGap * i - numeralsFontSize * (i - 1)
                canvas.drawText(numerals[i + 5], x, y, paint)
            }
        } else {
            numerals.forEachIndexed { index, numeral ->
                val angle = Math.toRadians((index - 2) * 30.0)
                val x = (centerX + radiusCoefficient * cos(angle)).toFloat()
                val y = (centerY + radiusCoefficient * sin(angle)).toFloat() + numeralsOffset
                canvas.drawText(numeral, x, y, paint)
            }
        }
    }

    private fun drawBackground(centerX: Float, centerY: Float, radius: Float, canvas: Canvas) {
        paint.reset()
        paint.isAntiAlias = true
        paint.color = backgroundColor
        paint.style = Paint.Style.FILL
        when (shape) {
            Shape.ROUND -> canvas.drawCircle(centerX, centerY, radius, paint)
            Shape.SQUARE -> canvas.drawRect(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
                paint
            )
        }
    }

    private fun drawBorder(
        centerX: Float,
        centerY: Float,
        radius: Float,
        canvas: Canvas
    ) {
        if (borderWidth == 0f) {
            return
        }
        paint.reset()
        paint.isAntiAlias = true
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = borderWidth
        when (shape) {
            Shape.ROUND -> canvas.drawCircle(centerX, centerY, radius - borderWidth / 2, paint)
            Shape.SQUARE -> canvas.drawRect(
                centerX - radius + borderWidth / 2,
                centerY - radius + borderWidth / 2,
                centerX + radius - borderWidth / 2,
                centerY + radius - borderWidth / 2,
                paint
            )
        }
    }

    companion object {
        val arabicNumerals = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12")
        val romanNumerals =
            arrayOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII")
    }
}