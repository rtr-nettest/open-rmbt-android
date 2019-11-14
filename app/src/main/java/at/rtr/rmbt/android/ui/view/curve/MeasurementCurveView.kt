package at.rtr.rmbt.android.ui.view.curve

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.View

class MeasurementCurveView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setWillNotDraw(false)
    }

    private var squareSizeCallback: ((Float) -> Unit)? = null

    private var bitmapOverlapPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    private var topPart: CurvePart = TopCurvePart(context)
    private var bottomPart: CurvePart = BottomCurvePart(context)

    private var viewWidth: Int = 0
    private var viewHeight: Int = 0

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (changed) {
            topPart.updateScreenSizeRelatedData(resources, viewWidth, viewHeight)
            bottomPart.updateScreenSizeRelatedData(resources, viewWidth, viewHeight)
            squareSizeCallback?.invoke(topPart.angleStep)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            canvas.drawBitmap(bottomPart.bitmap, bottomPart.getLeftOffset(), bottomPart.getTopOffset(), bitmapOverlapPaint)
            canvas.drawBitmap(topPart.bitmap, topPart.getLeftOffset(), topPart.getTopOffset(), bitmapOverlapPaint)
        }
    }

    fun setTopProgress(progress: Int) {
        topPart.updateProgress(progress)
        invalidate()
    }

    fun setBottomProgress(progress: Int) {
        bottomPart.updateProgress(progress)
        invalidate()
    }

    fun setTopCenterCallback(centerKnownCallback: (Int, Int) -> Unit) {
        topPart.centerKnownCallback = centerKnownCallback
    }

    fun setBottomCenterCallback(centerKnownCallback: (Int, Int) -> Unit) {
        bottomPart.centerKnownCallback = centerKnownCallback
    }

    fun setSquareSizeCallback(squareSizeCallback: (Float) -> Unit) {
        this.squareSizeCallback = squareSizeCallback
    }
}