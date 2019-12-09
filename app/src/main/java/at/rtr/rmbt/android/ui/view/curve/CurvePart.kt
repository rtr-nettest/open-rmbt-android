package at.rtr.rmbt.android.ui.view.curve

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.core.content.res.ResourcesCompat
import at.rtr.rmbt.android.R
import at.specure.measurement.MeasurementState
import kotlin.math.min

/**
 * Base class for the measurement curve circles. Contains main curve drawing logic
 */
abstract class CurvePart {

    /**
     * Defines the distances from center of curve part to row of squares
     */
    var mediumRadius: Float = 0f
    var smallRadius: Float = 0f
    var largeRadius: Float = 0f

    /**
     * Defines the size of squares and distances between them
     */
    var angleStep: Float = 0f

    /**
     * Defines the size of view
     */
    var viewSize = 0
    var viewWidth: Int = 0
    var viewHeight: Int = 0

    /**
     * used for progress drawing
     */
    var sectionEndAngle: Float = 0f

    /**
     * x and y coordinates of curve part's circle center
     */
    var cX: Float = 0f
    var cY: Float = 0f

    /**
     * Defines the size of curve square
     */
    var strokeWidth: Float = 0f

    var emptySquarePaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        isAntiAlias = true
    }

    var textPaint = Paint().apply {
        style = Paint.Style.FILL
        textSize = 40f
        letterSpacing = 0.15f
        isAntiAlias = true
    }

    /**
     * Callback should be called when the center of curve part is calculated and used for another views in [MeasurementCurveLayout]
     */
    var centerKnownCallback: ((Int, Int) -> Unit)? = null

    /**
     * Defines the start angle for drawing
     */
    abstract var sectionStartAngle: Float

    abstract var currentCanvas: Canvas?
    abstract var bitmap: Bitmap

    abstract var pathForText: Path
    abstract var progressOuterPaint: Paint
    abstract var progressInnerPaint: Paint

    /**
     * Defines the current [MeasurementState]
     */
    abstract var phase: MeasurementState

    /**
     * Defines the curve's sections
     */
    abstract var sections: List<CurveSection>

    /**
     * Drawing of the gray background for each [CurveSection] from [sections]
     */
    abstract fun drawSections(canvas: Canvas)

    /**
     * Drawing the text labels for each [CurveSection] from [sections]
     */
    abstract fun drawText(canvas: Canvas)

    /**
     * Updating the curve part according to current progress
     */
    abstract fun updateProgress(phase: MeasurementState, progress: Int, qosEnabled: Boolean)

    /**
     * Get the x coordinate of curve part
     */
    abstract fun getCenterX(): Float

    /**
     * Get the y coordinate of curve part
     */
    abstract fun getCenterY(): Float

    /**
     * In case of non-square screen curve should be centered. Method calculates the offset from top
     */
    abstract fun getTopOffset(): Float

    /**
     * In case of non-square screen curve should be centered. Method calculates the offset from left side
     */
    abstract fun getLeftOffset(): Float

    /**
     * Called when the parent layout was changed and recalculate the measures necessary for the drawing of curve part
     */
    fun updateScreenSizeRelatedData(resources: Resources, viewWidth: Int, viewHeight: Int) {
        this.viewWidth = viewWidth
        this.viewHeight = viewHeight

        viewSize = min(viewWidth, viewHeight)

        angleStep = CURVE_ANGLE / MAX_CURVE_PIECES_COUNT
        mediumRadius = viewSize.toFloat() / MIDDLE_RADIUS_DIVIDER

        strokeWidth = (Math.PI * mediumRadius * angleStep / 180).toFloat()
        emptySquarePaint.strokeWidth = strokeWidth
        textPaint.textSize = mediumRadius / TEXT_SIZE_DIVIDER

        smallRadius = mediumRadius - ANGLE_STEP_MULTIPLIER * strokeWidth
        largeRadius = mediumRadius + ANGLE_STEP_MULTIPLIER * strokeWidth

        cX = getCenterX()
        cY = getCenterY()
        centerKnownCallback?.invoke((cX + getLeftOffset()).toInt(), (cY + getTopOffset()).toInt())

        textPaint.color = ResourcesCompat.getColor(resources, R.color.measurement_text, null)
        emptySquarePaint.color = ResourcesCompat.getColor(resources, R.color.measurement_not_progressed, null)
        createBitmap((viewSize * BITMAP_SIZE_MULTIPLIER).toInt())
    }

    /**
     * Creates the bitmap with curve background and labels
     */
    private fun createBitmap(size: Int) {
        bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        bitmap.let {
            val canvas = Canvas(it)
            currentCanvas = canvas
            drawSections(canvas)
            drawText(canvas)
        }
    }

    companion object {
        protected const val MIDDLE_RADIUS_DIVIDER = 4
        protected const val TEXT_SIZE_DIVIDER = 8
        protected const val BITMAP_SIZE_MULTIPLIER = 2f / 3
        private const val MAX_CURVE_PIECES_COUNT = 128
        private const val CURVE_ANGLE = 270f

        const val ANGLE_STEP_MULTIPLIER = 1.9f
        const val TEXT_SIZE_MULTIPLIER = 0.8f

        const val SCALE_SWEEP_ANGLE = 45f
        const val QUARTER_CIRCLE = 90f
    }
}