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
import kotlin.math.roundToInt

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
        CurveSection(17, context.getString(R.string.scale_value_10), true),
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

    private fun calculateProgressAngle(currentProgress: Int): Float {
        var angle = 0f
        when {
            currentProgress < 1e3 -> { // up to 1 mbit
                val sectionAngle = sections[3].startAngle - sections[3].endAngle
                val kbits = currentProgress - 100
                angle = 0.1f * kbits * sectionAngle / 100
            }
            currentProgress < 1e4 -> { // up to 10 mbit
                val sectionAngle = sections[2].startAngle - sections[2].endAngle
                val mbits = (currentProgress / 1e3).roundToInt() - 1
                angle = (sections[2].endAngle - sections[3].endAngle) + 10 * mbits * sectionAngle / 100
            }
            currentProgress < 1e5 -> { // up to 100 mbit
                val sectionAngle = sections[1].startAngle - sections[1].endAngle
                val mbits = (currentProgress / 1e3).roundToInt() - 10
                angle = (sections[1].endAngle - sections[3].endAngle) + mbits * sectionAngle / 100
            }
            else -> { // up to 1000 mbit
                val sectionAngle = sections[0].startAngle - sections[0].endAngle
                val mbits = (currentProgress / 1e3).roundToInt()
                angle = (sections[0].endAngle - sections[3].endAngle) + 0.1f * mbits * sectionAngle / 100
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