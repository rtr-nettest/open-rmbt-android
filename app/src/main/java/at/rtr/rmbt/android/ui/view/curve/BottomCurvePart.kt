package at.rtr.rmbt.android.ui.view.curve

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.core.content.ContextCompat
import at.rtr.rmbt.android.R
import at.specure.measurement.MeasurementState
import kotlin.math.log


class BottomCurvePart(context: Context) : CurvePart() {

    override var sectionStartAngle: Float = -45f
    override var currentCanvas: Canvas? = null

    override var pathForText = Path()

    override var progressOuterPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.measurement_green)
        style = Paint.Style.STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        isAntiAlias = true
    }

    override var progressInnerPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.measurement_green_dark)
        style = Paint.Style.STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        isAntiAlias = true
    }

    override var phase: MeasurementState = MeasurementState.IDLE

    override lateinit var bitmap: Bitmap

    /**
     * Defines the distances between scale lines
     */
    private val scalePercents = listOf(70, 55, 40, 28, 20, 13, 8, 3)

    private val scalePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.measurement_scale)
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 30f
    }

    private val scaleLargePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.measurement_text)
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 50f
    }

    override var sections = listOf(
        CurveSection(17, context.getString(R.string.scale_value_1000), false),
        CurveSection(17, context.getString(R.string.scale_value_100), false),
        CurveSection(17, context.getString(R.string.scale_value_10), false),
        CurveSection(17, context.getString(R.string.scale_value_1), true),
        CurveSection(0, context.getString(R.string.scale_value_0_1), true)
    )

    override fun getCenterX() = viewSize.toFloat() / 3 - (largeRadius - smallRadius) / 2 - angleStep / 2
    override fun getCenterY() = viewSize.toFloat() / 3

    override fun getTopOffset() = (viewHeight - viewSize) + viewSize.toFloat() / 3
    override fun getLeftOffset() = viewWidth.toFloat() - viewSize

    override fun drawSections(canvas: Canvas) {
        var angle = sectionStartAngle - ANGLE_STEP_MULTIPLIER * angleStep
        for (section in sections) {
            section.startAngle = angle + angleStep
            for (i in 0 until section.count) {
                canvas.drawArc(
                    cX - smallRadius, cY - smallRadius, cX + smallRadius, cY + smallRadius,
                    angle,
                    angleStep,
                    false,
                    emptySquarePaint
                )
                canvas.drawArc(
                    cX - mediumRadius, cY - mediumRadius, cX + mediumRadius, cY + mediumRadius,
                    angle,
                    angleStep,
                    false,
                    emptySquarePaint
                )
                canvas.drawArc(
                    cX - largeRadius, cY - largeRadius, cX + largeRadius, cY + largeRadius,
                    angle,
                    angleStep,
                    false,
                    emptySquarePaint
                )

                angle -= ANGLE_STEP_MULTIPLIER * angleStep
            }
            section.endAngle = angle
            section.length = (Math.PI * smallRadius * (section.startAngle - section.endAngle) / 180).toFloat()
        }
        sectionEndAngle = angle + ANGLE_STEP_MULTIPLIER * angleStep
    }

    override fun drawText(canvas: Canvas) {
        drawScale(canvas)

        for (section in sections) {
            pathForText.reset()
            val bounds = Rect()
            textPaint.getTextBounds(section.text, 0, section.text.length, bounds)
            val scaleRadius = smallRadius - SCALE_RADIUS_COEF * (largeRadius - smallRadius)
            val halfTextLength = (QUARTER_CIRCLE * bounds.width()).toInt() / (Math.PI * (scaleRadius - bounds.height())).toFloat()

            if (section.isUpside) {
                pathForText.addArc(
                    cX - scaleRadius, cY - scaleRadius, cX + scaleRadius, cY + scaleRadius,
                    section.startAngle + (if (section.text.length > 1) halfTextLength else angleStep * ANGLE_STEP_MULTIPLIER),
                    -SCALE_SWEEP_ANGLE
                )
                canvas.drawTextOnPath(section.text, pathForText, 0f, -textPaint.textSize / 2, textPaint)
            } else {
                pathForText.addArc(
                    cX - scaleRadius, cY - scaleRadius, cX + scaleRadius, cY + scaleRadius,
                    section.startAngle - halfTextLength,
                    SCALE_SWEEP_ANGLE
                )
                canvas.drawTextOnPath(section.text, pathForText, 0f, textPaint.textSize * SCALE_OFFSET_COEF, textPaint)
            }
        }
    }

    /**
     * Draw the scale lines
     */
    private fun drawScale(canvas: Canvas) {
        val scaleRadius = smallRadius - SCALE_RADIUS_COEF * SCALE_OFFSET_COEF * (mediumRadius - smallRadius)
        val scaleLargeRadius = smallRadius - SCALE_OFFSET_COEF * (mediumRadius - smallRadius)

        scalePaint.strokeWidth = scaleRadius / 16
        scaleLargePaint.strokeWidth = scaleRadius / 7

        for (section in sections) {
            val sectionAngle = section.startAngle - section.endAngle

            canvas.drawArc(
                cX - scaleLargeRadius, cY - scaleLargeRadius, cX + scaleLargeRadius, cY + scaleLargeRadius,
                section.startAngle,
                SMALL_SCALE_ANGLE,
                false,
                scaleLargePaint
            )

            if (section.count != 0) {
                for (i in scalePercents) {
                    canvas.drawArc(
                        cX - scaleRadius, cY - scaleRadius, cX + scaleRadius, cY + scaleRadius,
                        section.startAngle - i * (sectionAngle / 100),
                        SMALL_SCALE_ANGLE,
                        false,
                        scalePaint
                    )
                }
            }
        }
    }

    override fun updateProgress(phase: MeasurementState, progress: Int, qosEnabled: Boolean) {
        progressInnerPaint.strokeWidth = (mediumRadius - smallRadius)
        progressOuterPaint.strokeWidth = 2.5f * (largeRadius - mediumRadius)
        val progressAngle = calculateProgressAngle(progress)

        currentCanvas?.let { currentCanvas ->

            currentCanvas.drawColor(
                Color.TRANSPARENT,
                PorterDuff.Mode.CLEAR
            )

            drawSections(currentCanvas)
            drawText(currentCanvas)

            currentCanvas.drawArc(
                cX - largeRadius,
                cY - largeRadius,
                cX + largeRadius,
                cY + largeRadius,
                sectionEndAngle,
                progressAngle,
                false,
                progressOuterPaint
            )

            currentCanvas.drawArc(
                cX - smallRadius,
                cY - smallRadius,
                cX + smallRadius,
                cY + smallRadius,
                sectionEndAngle,
                progressAngle,
                false,
                progressInnerPaint
            )
        }
    }

    private fun calculateProgressAngle(currentSpeedKbps: Int): Float {
        var angle : Float
        val minAngle : Float = ANGLE_STEP_MULTIPLIER
        val maxAngle : Float = sections[0].startAngle - sections[3].endAngle

        when {
            (currentSpeedKbps <= 0) -> { // start of measurement (init, ping) should not show anything
                // show empty scale (not even a single segment visible)
                angle = 0F
            }
            (currentSpeedKbps <= 1e6) -> {
                // scale from 0-100% on 0.1M to 1G
                val percent : Double = (log(currentSpeedKbps.toDouble(),10.0) - 2F)/4F
                angle = (percent * maxAngle).toFloat()-minAngle
                // make sure at least a single segment is visible
                if (angle < minAngle) {
                    angle = minAngle
                }
            }
            else -> { // overflow, above 1G
                // show full scale
                angle = maxAngle
            }
        }
        return angle
    }

    companion object {
        private const val SMALL_SCALE_ANGLE = 1f
        private const val SCALE_RADIUS_COEF = 0.8f
        private const val SCALE_OFFSET_COEF = 1.4f
    }
}