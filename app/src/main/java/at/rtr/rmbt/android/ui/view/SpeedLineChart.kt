package at.rtr.rmbt.android.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import at.rtr.rmbt.android.R
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.min
import kotlin.math.max

class SpeedLineChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LineChart(context, attrs) {

    private var pathStroke: Path
    private var paintStroke: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var pathFill: Path
    private var paintFill: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val lineDataList: ArrayList<LineData> = ArrayList()

    private var startTime: Long = -1

    private var lX = 0f
    private var lY = 0f

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

    fun addValue(value: Long, measurementProgress: Int) {

        if (lineDataList.none { it.time == 1.0f })
            addValue(toLog(value), measurementProgress / 100.0f)
    }

    private fun addValue(value: Float, time: Float) {

        if (lineDataList.isEmpty()) {
            lineDataList.add(LineData(value, 0.0f))
            lineDataList.add(LineData(value, time))

            val x = getChartWidth() * lineDataList[0].time
            val y = getChartHeight() - (getChartHeight() * lineDataList[0].value)
            pathStroke.moveTo(x, y)
        } else {
            lineDataList.add(LineData(value, time))
        }
        if (lineDataList.size >= 3) {
            val currentIndex = lineDataList.size - 2
            // current point
            val currentX = getChartWidth() * lineDataList.get(currentIndex).time
            val currentY = getChartHeight() - (getChartHeight() * lineDataList.get(currentIndex).value)

            // previous point
            val previousX = getChartWidth() * lineDataList.get(currentIndex - 1).time
            val previousY = getChartHeight() - (getChartHeight() * lineDataList.get(currentIndex - 1).value)

            // Next point
            val nextX = getChartWidth() * lineDataList.get(if (currentIndex + 1 < lineDataList.size) currentIndex + 1 else currentIndex).time
            val nextY = getChartHeight() - (getChartHeight() * lineDataList.get(if (currentIndex + 1 < lineDataList.size) currentIndex + 1 else currentIndex).value)

            val d0 = sqrt((currentX - previousX).toDouble().pow(2.0) + (currentY - previousY).toDouble().pow(2.0)).toFloat() // distance between p and p0
            val x1 = min(previousX + lX * d0, (previousX + currentX) / 2) // min is used to avoid going too much right
            val y1 = previousY + lY * d0

            val d1 = sqrt((nextX - previousX).toDouble().pow(2.0) + (nextY - previousY).toDouble().pow(2.0)).toFloat() // distance between next and previous (length of reference line)
            lX = (nextX - previousX) / d1 * 0.3f // (lX,lY) is the slope of the reference line
            lY = (nextY - previousY) / d1 * 0.3f

            val x2 = max(currentX - lX * d0, (previousX + currentX) / 2) // max is used to avoid going too much left
            val y2 = currentY - lY * d0

            // add line
            pathStroke.cubicTo(x1, y1, x2, y2, currentX, currentY)

            // Fill path draw
            pathFill.rewind()
            pathFill.addPath(pathStroke)
            pathFill.lineTo(currentX, getChartHeight())
            pathFill.lineTo(getChartWidth() * lineDataList[0].time, getChartHeight())
        }
        invalidate()
    }

    fun reset() {

        lineDataList.clear()
        pathStroke.rewind()
        pathFill.rewind()
        startTime = -1
        lX = 0f
        lY = 0f
        invalidate()
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