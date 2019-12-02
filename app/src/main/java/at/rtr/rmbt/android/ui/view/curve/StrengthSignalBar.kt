package at.rtr.rmbt.android.ui.view.curve

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import at.rtr.rmbt.android.R
import kotlin.math.min

class StrengthSignalBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setWillNotDraw(false)
    }

    private var emptySquarePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.measurement_not_progressed)
        style = Paint.Style.FILL_AND_STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        isAntiAlias = true
    }

    private var linePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.measurement_text)
        style = Paint.Style.FILL_AND_STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        strokeWidth = 3f
        isAntiAlias = true
    }

    private var progressPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.measurement_green)
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }

    private var progressInnerPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.measurement_green_dark)
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }

    private var textPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.measurement_text)
        style = Paint.Style.FILL
        textSize = 20f
        isAntiAlias = true
    }

    /**
     * Contains the string values of signal graph legend
     */
    private var values = listOf<String>()

    /**
     * Defines count of squares in graph
     */
    private var verticalCount = 15
    private var horizontalCount = 3

    private var currentCanvas: Canvas? = null
    private var bitmap: Bitmap? = null

    private var minValue: Int = 0
    private var maxValue: Int = 0
    private var currentValue: Int = 0

    private var topMargin = resources.getDimension(R.dimen.margin_medium)

    var squareSize: Float = 0f
        set(value) {
            field = value
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val viewWidth = MeasureSpec.getSize(min(widthMeasureSpec, heightMeasureSpec))
        textPaint.textSize = resources.getDimension(R.dimen.signal_bar_scale_value)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(viewWidth, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec((SQUARE_MULTIPLIER * squareSize * (verticalCount + 1) + topMargin).toInt(), MeasureSpec.AT_MOST)
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (bottom != top && !changed) {
            createBitmap()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let { canvas.drawBitmap(it, 0f, 0f, emptySquarePaint) }
    }

    /**
     * Called when the data for the signal bar is updated, calculated the graph legend, updating current progress
     */
    fun setSignalData(currentSignal: Int, minSignal: Int, maxSignal: Int) {
        currentValue = currentSignal
        minValue = minSignal
        maxValue = maxSignal

        val diff = minSignal - maxSignal
        values = listOf(
            maxSignal.toString(),
            (maxSignal + 0.33 * diff).toInt().toString(),
            (maxSignal + 0.67 * diff).toInt().toString(),
            minSignal.toString()
        )
        updateProgress()
        invalidate()
    }

    private fun createBitmap() {
        if (measuredHeight > 0 && measuredWidth > 0) {
            with(Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)) {
                bitmap = this
                val canvas = Canvas(this)
                currentCanvas = canvas
                drawBackground(canvas)
                updateProgress()
            }
        }
    }

    /**
     * Draw gray squares for the background
     */
    private fun drawBackground(canvas: Canvas) {
        var y = squareSize + topMargin / 2
        for (i in 0 until verticalCount) {
            var x = squareSize
            for (j in 0 until horizontalCount) {
                canvas.drawRect(x, y, x + squareSize, y + squareSize, emptySquarePaint)
                x += squareSize * SQUARE_MULTIPLIER
            }
            y += squareSize * SQUARE_MULTIPLIER
        }

        val legendHeight = measuredHeight - 2 * topMargin
        val count = values.count()
        for (pos in 0 until count) {
            if (pos == 0 || pos == count - 1) {
                canvas.drawLine(
                    6.5f * squareSize,
                    squareSize + pos * legendHeight / (count - 1) + topMargin / 2,
                    8.5f * squareSize,
                    squareSize + pos * legendHeight / (count - 1) + topMargin / 2,
                    linePaint
                )
            } else {
                canvas.drawLine(
                    6.5f * squareSize,
                    squareSize + pos * legendHeight / (count - 1) + topMargin / 2,
                    7.5f * squareSize,
                    squareSize + pos * legendHeight / (count - 1) + topMargin / 2,
                    linePaint
                )
            }
            canvas.drawText(values[pos], 9.5f * squareSize, 2 * squareSize + pos * legendHeight / (count - 1) + topMargin / 2, textPaint)
        }
    }

    /**
     * Calculate the size of filled graph of part and draw it
     */
    private fun updateProgress() {
        currentCanvas?.let {
            it.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            drawBackground(it)

            val currentLevel = (currentValue - maxValue).toFloat() / (minValue - maxValue) * measuredHeight
            it.drawRect(0f, currentLevel, 4 * squareSize, measuredHeight.toFloat(), progressPaint)
            it.drawRect(4 * squareSize, currentLevel, 6 * squareSize, measuredHeight.toFloat(), progressInnerPaint)
        }
    }

    companion object {
        private const val SQUARE_MULTIPLIER = 1.75f
    }
}