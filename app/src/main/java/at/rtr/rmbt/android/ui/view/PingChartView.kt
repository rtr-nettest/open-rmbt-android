package at.rtr.rmbt.android.ui.view

import android.content.Context
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.util.calcTextHeight
import at.rtr.rmbt.android.util.calcTextWidth
import timber.log.Timber

open class PingChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var gridPaint: Paint
    private var yLabelPaint: Paint

    private var gridDottedLinePaint: Paint
    private var gridDottedLinePath: Path

    private var startPadding: Float = 0.0f
    private var endPadding: Float = 0.0f
    private var textHeight: Float = 0.0f

    private var yLabels: Array<Int>? = null
    private var numberOfRows: Int = DEFAULT_NUMBER_OF_ROWS_IN_GRID


    init {

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.LineChart)


        gridPaint = Paint()
        gridPaint.color = typedArray.getColor(R.styleable.LineChart_grid_color, context.getColor(R.color.chart_grid_line_color))
        gridPaint.strokeWidth = 2.0f
        gridPaint.style = Paint.Style.STROKE
        gridPaint.isAntiAlias = true

        gridDottedLinePaint = Paint()
        gridDottedLinePaint.color = typedArray.getColor(R.styleable.LineChart_grid_color, context.getColor(R.color.chart_grid_line_color))
        gridDottedLinePaint.style = Paint.Style.STROKE
        gridDottedLinePaint.pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
        gridDottedLinePaint.strokeWidth = 2f

        gridDottedLinePath = Path()

        val robotoRegularTypeface = ResourcesCompat.getFont(context, R.font.roboto_regular)

        yLabelPaint = Paint()
        yLabelPaint.isAntiAlias = true
        yLabelPaint.typeface = robotoRegularTypeface
        yLabelPaint.textSize =
            typedArray.getDimension(R.styleable.LineChart_ylabel_text_size, context.resources.getDimension(R.dimen.chart_label_text_size))
        yLabelPaint.textAlign = Paint.Align.LEFT
        yLabelPaint.color = typedArray.getColor(R.styleable.LineChart_ylabel_color, context.getColor(R.color.chart_labels_color))

        numberOfRows =
            typedArray.getInteger(R.styleable.LineChart_grid_row, DEFAULT_NUMBER_OF_ROWS_IN_GRID)

        typedArray.recycle()

        endPadding = yLabelPaint.calcTextWidth(DEFAULT_Y_LABEL_TEXT)
        textHeight = yLabelPaint.calcTextHeight(DEFAULT_Y_LABEL_TEXT)
    }

    /**
     * Return size of chart width without y labels text size
     */
    fun getChartWidth(): Float {
        return width - endPadding
    }

    /**
     * Return size of chart width without x labels text size
     */
    fun getChartHeight(): Float {
        return height.toFloat()
    }

    fun setYLabels(yLabels: Array<Int>) {
        this.yLabels = yLabels
        invalidate()
    }


    override fun onDraw(canvas: Canvas?) {

        val endX = width - endPadding
        val endY = height.toFloat()
        val rowHeight = endY / numberOfRows

        // Draw line of grid
        for (index in 0..numberOfRows) {

            val positionY = endY - rowHeight * index
            canvas?.drawLine(startPadding, positionY, endX, positionY, gridPaint)
        }

        // Draw vertical dotted line
        gridDottedLinePath.moveTo(endX, 0.0f)
        gridDottedLinePath.lineTo(endX, endY)
        canvas?.drawPath(gridDottedLinePath, gridDottedLinePaint)

        // Draw Y Labels text

        yLabels?.let {
            for (index in it.indices) {

                val positionY = when (index) {
                    0 -> {
                        (endY - rowHeight * index)
                    }
                    it.size-1 -> {
                        (endY - rowHeight * index)+ (textHeight)
                    }
                    else -> {
                        (endY - rowHeight * index) + (textHeight/2)
                    }
                }
                canvas?.drawText(context.getString(R.string.measurement_ping_value, it[index]), endX + endPadding / 8,
                    positionY, yLabelPaint)
            }
        }
    }

    companion object {

        private const val DEFAULT_NUMBER_OF_ROWS_IN_GRID: Int = 4
        private const val DEFAULT_Y_LABEL_TEXT: String = "1000 ms"
    }
}