package at.rtr.rmbt.android.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import at.rtr.rmbt.android.R
import kotlin.math.log10

class SpeedLineChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LineChart(context, attrs) {

    private var pathStroke: Path
    private var paintStroke: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var pathFill: Path
    private var paintFill: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val lineDataList: ArrayList<LineData> = ArrayList()
    private var firstPoint: PointF? = null

    private var startTime: Long = -1

    init {

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SpeedLineChart)

        paintStroke.color = typedArray.getColor(R.styleable.SpeedLineChart_progress_line_color, context.getColor(R.color.colorAccent))
        paintStroke.style = Paint.Style.STROKE
        paintStroke.strokeWidth = 3.0f
        paintStroke.strokeCap = Paint.Cap.ROUND
        paintStroke.strokeJoin = Paint.Join.ROUND
        paintStroke.isAntiAlias = true

        pathStroke = Path()

        paintFill.color = typedArray.getColor(R.styleable.SpeedLineChart_progress_fill_color, context.getColor(R.color.speed_chart_progress_fill_color))
        paintFill.style = Paint.Style.FILL
        paintFill.isAntiAlias = true

        pathFill = Path()

        typedArray.recycle()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawPath(pathStroke, paintStroke)
        canvas?.drawPath(pathFill, paintFill)
    }

    fun addValue(value: Long) {

        val relTime: Long

        if (startTime <= 0) {
            startTime = System.nanoTime()
            relTime = 0
        } else {
            relTime = (System.nanoTime() - startTime)
        }
        if (relTime >= GRAPH_MAX_NSECS) return

        val time: Float = relTime.toFloat() / GRAPH_MAX_NSECS
        addValue(toLog(value), time)
    }

    private fun addValue(value: Float, time: Float) {

        lineDataList.add(LineData(value, time))
        val averageValue = getAverageValue(lineDataList)

        val x = getChartWidth() * time
        val y = (getChartHeight() * (1 - averageValue)).toFloat()

        if (firstPoint == null) {

            pathStroke.moveTo(x, y)
            firstPoint = PointF(x, y)
        }

        pathStroke.lineTo(x, y)

        firstPoint?.x?.let {

            pathFill.rewind()
            pathFill.addPath(pathStroke)
            pathFill.lineTo(x, getChartHeight())
            pathFill.lineTo(it, getChartHeight())
        }
        invalidate()
    }

    fun reset() {

        lineDataList.clear()
        pathStroke.rewind()
        pathFill.rewind()
        startTime = -1
        firstPoint = null
        invalidate()
    }

    /**
     * Return average value from last 3 values
     */
    private fun getAverageValue(lineData: List<LineData>): Double {

        val startIndex = if (lineData.size >= 3) lineData.size - 3 else 0
        val endIndex = lineData.size

        var valueTotal = 0.0
        for (index in startIndex until endIndex) {
            valueTotal += lineData[index].value
        }
        return valueTotal / if (lineData.size >= 3) 3 else lineData.size
    }

    /**
     * This function is used for convert download and upload speed into graph Y value
     */
    private fun toLog(value: Long): Float {
        return (if (value < 1e5) 0.0 else (2.0 + log10(value / 1e7)) / 4.0).toFloat()
    }

    override fun onDetachedFromWindow() {
        reset()
        super.onDetachedFromWindow()
    }

    companion object {
        private const val GRAPH_MAX_NSECS: Long = 8000000000L
    }

    data class LineData(val value: Float, val time: Float)
}