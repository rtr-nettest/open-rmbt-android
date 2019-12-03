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
import kotlin.math.cos
import kotlin.math.sin

class TopCurvePart(context: Context) : CurvePart() {

    override var sectionStartAngle: Float = 135f

    override lateinit var bitmap: Bitmap
    override var currentCanvas: Canvas? = null

    override var pathForText = Path()
    override var sections = listOf(
        CurveSection(16, context.getString(R.string.label_qos), true, MeasurementState.QOS),
        CurveSection(16, context.getString(R.string.label_upload), true, MeasurementState.UPLOAD),
        CurveSection(16, context.getString(R.string.label_download), false, MeasurementState.DOWNLOAD),
        CurveSection(8, context.getString(R.string.label_ping), false, MeasurementState.PING),
        CurveSection(8, context.getString(R.string.label_init), false, MeasurementState.INIT)
    )

    override var progressOuterPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        isAntiAlias = true
    }

    override var progressInnerPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.measurement_progress_inner)
        style = Paint.Style.STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        isAntiAlias = true
    }

    override var phase: MeasurementState = MeasurementState.IDLE
        set(value) {
            field = value
            previousProgress = 0
        }

    private var dividerPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.measurement_text)
        style = Paint.Style.FILL_AND_STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        isAntiAlias = true
    }

    private var previousProgress = 0

    override fun getCenterX() = viewSize.toFloat() / 3 + (largeRadius - smallRadius) / 2 + angleStep / 2
    override fun getCenterY() = viewSize.toFloat() / 3

    override fun getTopOffset() = viewHeight.toFloat() - viewSize
    override fun getLeftOffset() = (viewWidth - viewSize) + viewSize.toFloat() / 3

    override fun drawSections(canvas: Canvas) {
        dividerPaint.strokeWidth = 5f
        // todo check drawing logic
        val dividerAngle = 135.0 * Math.PI / 180f
        canvas.drawLine(
            (cX - (largeRadius - smallRadius) / 2 - angleStep / 2 + (smallRadius - 30) * cos(dividerAngle)).toFloat(),
            (cY + (smallRadius - 30) * sin(dividerAngle)).toFloat(),
            (cX - (largeRadius - smallRadius) / 2 + (largeRadius) * cos(dividerAngle)).toFloat(),
            (cY + (largeRadius) * sin(dividerAngle)).toFloat(),
            dividerPaint
        )

        var angle = sectionStartAngle - ANGLE_STEP_MULTIPLIER * angleStep
        for (section in sections) {
            section.startAngle = angle
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

                section.endAngle = angle
                section.length = (Math.PI * smallRadius * (section.startAngle - section.endAngle) / 180).toFloat()
            }

            angle -= angleStep
        }
        sectionEndAngle = angle + ANGLE_STEP_MULTIPLIER * angleStep
    }

    override fun drawText(canvas: Canvas) {
        for (section in sections) {
            pathForText.reset()
            val bounds = Rect()
            textPaint.getTextBounds(section.text, 0, section.text.length, bounds)

            if (section.isUpside) {
                pathForText.addArc(
                    cX - smallRadius, cY - smallRadius, cX + smallRadius, cY + smallRadius,
                    section.startAngle + angleStep * TEXT_SIZE_MULTIPLIER * 3,
                    section.endAngle - section.startAngle - angleStep * ANGLE_STEP_MULTIPLIER
                )
                canvas.drawTextOnPath(
                    section.text,
                    pathForText,
                    section.length - bounds.width(),
                    -textPaint.textSize * TEXT_SIZE_MULTIPLIER,
                    textPaint
                )
            } else {
                pathForText.addArc(
                    cX - smallRadius, cY - smallRadius, cX + smallRadius, cY + smallRadius,
                    section.endAngle + angleStep,
                    section.length
                )
                canvas.drawTextOnPath(section.text, pathForText, 0f, textPaint.textSize * 2 * TEXT_SIZE_MULTIPLIER, textPaint)
            }
        }
    }

    override fun updateProgress(progress: Int) {
        progressInnerPaint.strokeWidth = (mediumRadius - smallRadius)
        progressOuterPaint.strokeWidth = 3 * (largeRadius - mediumRadius)
        val progressAngle = calculateProgressAngle(if (progress > previousProgress) progress else previousProgress)
        previousProgress = progress

        currentCanvas?.let { currentCanvas ->

            currentCanvas.drawColor(
                Color.TRANSPARENT,
                PorterDuff.Mode.CLEAR
            )

            drawSections(currentCanvas)
            drawText(currentCanvas)

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
        }
    }

    /**
     * Calculate the angle which should be drawn according to current phase and progress
     */
    private fun calculateProgressAngle(progress: Int): Float {
        return when (phase) {
            MeasurementState.IDLE -> 0f
            MeasurementState.INIT -> {
                val initSection = sections.find { it.state == MeasurementState.INIT }!!
                (initSection.startAngle - initSection.endAngle) * progress.toFloat() / 100
            }
            MeasurementState.PING -> {
                val pingSection = sections.find { it.state == MeasurementState.PING }!!
                val initSection = sections.find { it.state == MeasurementState.INIT }!!
                (pingSection.endAngle - initSection.endAngle) + (pingSection.startAngle - pingSection.endAngle) * progress.toFloat() / 100
            }
            MeasurementState.DOWNLOAD -> {
                val downloadSection = sections.find { it.state == MeasurementState.DOWNLOAD }!!
                val initSection = sections.find { it.state == MeasurementState.INIT }!!
                (downloadSection.endAngle - initSection.endAngle) + (downloadSection.startAngle - downloadSection.endAngle) * progress.toFloat() / 100
            }
            MeasurementState.UPLOAD -> {
                val uploadSection = sections.find { it.state == MeasurementState.UPLOAD }!!
                val initSection = sections.find { it.state == MeasurementState.INIT }!!
                (uploadSection.endAngle - initSection.endAngle) + (uploadSection.startAngle - uploadSection.endAngle) * progress.toFloat() / 100
            }
            // todo add QoS part
            else -> (sectionStartAngle - sectionEndAngle) / 100 * progress
        }
    }
}