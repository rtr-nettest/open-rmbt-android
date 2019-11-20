package at.rtr.rmbt.android.ui.view.curve

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.core.content.ContextCompat
import at.rtr.rmbt.android.R
import at.specure.measurement.MeasurementState

class BottomCurvePart(context: Context) : CurvePart() {

    override var sectionStartAngle: Float = -45f
    override var currentCanvas: Canvas? = null

    override var pathForText = Path()

    override var progressPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorAccent)
        style = Paint.Style.STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
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

    override fun getTopOffset() = (viewHeight - viewSize) / 2 + viewSize.toFloat() / 3
    override fun getLeftOffset() = (viewWidth.toFloat() - viewSize) / 2

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
            val scaleRadius = smallRadius * SCALE_RADIUS_COEF
            val halfTextLength = (QUARTER_CIRCLE * bounds.width()) / (Math.PI.toFloat() * (scaleRadius - textPaint.textSize))

            if (section.isUpside) {
                pathForText.addArc(
                    cX - scaleRadius, cY - scaleRadius, cX + scaleRadius, cY + scaleRadius,
                    section.startAngle + halfTextLength,
                    -SCALE_SWEEP_ANGLE
                )
                canvas.drawTextOnPath(section.text, pathForText, 0f, -textPaint.textSize / 2, textPaint)
            } else {
                pathForText.addArc(
                    cX - scaleRadius, cY - scaleRadius, cX + scaleRadius, cY + scaleRadius,
                    section.startAngle - halfTextLength,
                    SCALE_SWEEP_ANGLE
                )
                canvas.drawTextOnPath(section.text, pathForText, 0f, textPaint.textSize * 1.25f, textPaint)
            }
        }
    }

    /**
     * Draw the scale lines
     */
    private fun drawScale(canvas: Canvas) {
        val scaleRadius = smallRadius - SCALE_RADIUS_COEF * (largeRadius - smallRadius)
        val scaleLargeRadius = smallRadius - (largeRadius - smallRadius)

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

    override fun updateProgress(progress: Int) {
        progressPaint.strokeWidth = ANGLE_STEP_MULTIPLIER * (largeRadius - smallRadius)
        val progressAngle = (sectionStartAngle - sectionEndAngle) / 100 * progress

        currentCanvas?.let { currentCanvas ->
            drawSections(currentCanvas)
            drawText(currentCanvas)

            currentCanvas.drawArc(
                cX - mediumRadius,
                cY - mediumRadius,
                cX + mediumRadius,
                cY + mediumRadius,
                sectionEndAngle,
                progressAngle,
                false,
                progressPaint
            )
        }
    }

    companion object {
        private const val SMALL_SCALE_ANGLE = 1f
        private const val SCALE_RADIUS_COEF = 0.8f
    }
}