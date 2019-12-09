package at.rtr.rmbt.android.ui.view.curve

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.View
import at.specure.measurement.MeasurementState
import kotlin.math.min

class MeasurementCurveView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setWillNotDraw(false)
    }

    private var squareSizeCallback: ((Float, Int) -> Unit)? = null

    private var bitmapOverlapPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    private var topPart: CurvePart = TopCurvePart(context)
    private var bottomPart: CurvePart = BottomCurvePart(context)

    private var viewWidth: Int = 0
    private var viewHeight: Int = 0

    private var currentPhase: MeasurementState = MeasurementState.IDLE
    private var currentTopProgress = 0
    private var currentBottomProgress = 0
    private var isQoSEnabled = false

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (changed) {
            viewWidth = right - left
            viewHeight = bottom - top

            topPart.updateScreenSizeRelatedData(resources, viewWidth, viewHeight)
            bottomPart.updateScreenSizeRelatedData(resources, viewWidth, viewHeight)
            if (currentPhase != MeasurementState.IDLE) {
                topPart.updateProgress(currentPhase, currentTopProgress, isQoSEnabled)
                bottomPart.updateProgress(currentPhase, currentBottomProgress, isQoSEnabled)
            }
            squareSizeCallback?.invoke(topPart.strokeWidth, min(topPart.viewHeight, topPart.viewWidth))
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            canvas.drawBitmap(bottomPart.bitmap, bottomPart.getLeftOffset(), bottomPart.getTopOffset(), bitmapOverlapPaint)
            canvas.drawBitmap(topPart.bitmap, topPart.getLeftOffset(), topPart.getTopOffset(), bitmapOverlapPaint)
        }
    }

    fun setTopProgress(phase: MeasurementState, progress: Int, qosEnabled: Boolean) {
        currentPhase = phase
        currentTopProgress = progress
        isQoSEnabled = qosEnabled
        topPart.updateProgress(phase, progress, qosEnabled)
        invalidate()
    }

    fun setBottomProgress(phase: MeasurementState, progress: Int, qosEnabled: Boolean) {
        currentPhase = phase
        currentBottomProgress = progress
        isQoSEnabled = qosEnabled
        bottomPart.updateProgress(phase, progress, qosEnabled)
        invalidate()
    }

    fun setTopCenterCallback(centerKnownCallback: (Int, Int) -> Unit) {
        topPart.centerKnownCallback = centerKnownCallback
    }

    fun setBottomCenterCallback(centerKnownCallback: (Int, Int) -> Unit) {
        bottomPart.centerKnownCallback = centerKnownCallback
    }

    fun setSquareSizeCallback(squareSizeCallback: (Float, Int) -> Unit) {
        this.squareSizeCallback = squareSizeCallback
    }

    fun setMeasurementState(state: MeasurementState) {
        topPart.phase = state
    }
}