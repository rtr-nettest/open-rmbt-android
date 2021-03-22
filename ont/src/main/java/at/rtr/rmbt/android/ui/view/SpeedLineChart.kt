package at.rtr.rmbt.android.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Shader
import android.util.AttributeSet
import at.rtr.rmbt.android.R
import at.specure.data.NetworkTypeCompat
import at.specure.data.entity.GraphItemRecord
import at.specure.data.entity.TestResultGraphItemRecord
import timber.log.Timber
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class SpeedLineChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LineChart(context, attrs), ResultChart {

    private var pathStroke: Path
    private var paintStroke: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var pathFill: Path
    private var paintFill: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var circlePoint: PointF? = null
    private var startTime: Long = -1

    private var chartPoints: ArrayList<PointF> = ArrayList()

    init {

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SpeedLineChart)

        paintStroke.color = typedArray.getColor(
            R.styleable.SpeedLineChart_progress_line_color,
            context.getColor(R.color.colorAccent)
        )

        val endColor = context.getColor(R.color.measurement_progress_end)
        paintStroke.style = Paint.Style.STROKE
        paintStroke.strokeWidth = STROKE_WIDTH
        paintStroke.strokeCap = Paint.Cap.ROUND
        paintStroke.strokeJoin = Paint.Join.ROUND
        paintStroke.isAntiAlias = true
        paintFill.setShader(LinearGradient(0F, 0F, getChartWidth(), getChartHeight(), paintStroke.color, endColor, Shader.TileMode.CLAMP))

        pathStroke = Path()

        paintFill.color = typedArray.getColor(
            R.styleable.SpeedLineChart_progress_fill_color,
            context.getColor(R.color.speed_chart_progress_fill_color)
        )
        paintFill.style = Paint.Style.FILL
        paintFill.isAntiAlias = true
        paintFill.setShader(
            LinearGradient(
                0F,
                0F,
                getChartWidth(),
                getChartHeight().toFloat(),
                paintFill.color,
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        )

        pathFill = Path()

        typedArray.recycle()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (pathStroke.isEmpty && chartPoints.isNotEmpty()) {
            calculatePath()
        }
        canvas?.drawPath(pathStroke, paintStroke)
        canvas?.drawPath(pathFill, paintFill)
        circlePoint?.let {
            canvas?.drawCircle(
                getChartWidth() * it.x - STROKE_WIDTH / 2.0f,
                getChartHeight() - (getChartHeight() * it.y),
                STROKE_WIDTH / 2,
                paintStroke
            )
        }
    }

    fun addGraphItems(graphItems: List<GraphItemRecord>?) {

        pathStroke.rewind()
        pathFill.rewind()
        if (graphItems != null && graphItems.isNotEmpty()) {

            chartPoints = ArrayList()

            if (graphItems[0].progress > 0) {
                chartPoints.add(PointF(0.0f, toLog(graphItems[0].value)))
            }
            for (index in graphItems.indices) {

                val x = graphItems[index].progress / 100.0f
                val y = toLog(graphItems[index].value)

                Timber.d("speedtest speed ${graphItems[index].value}")
                chartPoints.add(PointF(x, y))
            }
        }
        invalidate()
    }

    override fun addResultGraphItems(graphItems: List<TestResultGraphItemRecord>?, networkType: NetworkTypeCompat) {

        pathStroke.rewind()
        pathFill.rewind()

        graphItems?.let { items ->

            chartPoints = ArrayList()

            val maxValue = items.maxBy { it.time }?.time
            if (maxValue != null) {

                if (((items[0].time / maxValue.toFloat()) * 100.0f) > 0) {
                    chartPoints.add(PointF(0.0f, toLog(graphItems[0].value * 8000 / graphItems[0].time)))
                }

                for (index in items.indices) {
                    val x = items[index].time / maxValue.toFloat()
                    val y = toLog(graphItems[index].value * 8000 / graphItems[index].time)
                    chartPoints.add(PointF(x, y))
                    Timber.d("itemsdisplaytest x $x y $y width ${getChartWidth()} height ${getChartHeight()}")
                }
            }
        }
        invalidate()
    }

    /**
     * This function is use for calculate path
     */

    private fun calculatePath() {
        circlePoint = chartPoints[chartPoints.size - 1]
        var lX = 0f
        var lY = 0f
        pathStroke.moveTo(getChartWidth() * chartPoints[0].x, getChartHeight() - (getChartHeight() * chartPoints[0].y))
        for (index in 1 until chartPoints.size) {
            val currentPointX = getChartWidth() * chartPoints[index].x
            val currentPointY = getChartHeight() - (getChartHeight() * chartPoints[index].y)
            val previousPointX = getChartWidth() * chartPoints[index - 1].x
            val previousPointY = getChartHeight() - (getChartHeight() * chartPoints[index - 1].y)

            // Distance between currentPoint and previousPoint
            val firstDistance =
                sqrt((currentPointX - previousPointX).toDouble().pow(2.0) + (currentPointY - previousPointY).toDouble().pow(2.0)).toFloat()

            // Minimum is used to avoid going too much right
            val firstX = min(previousPointX + lX * firstDistance, (previousPointX + currentPointX) / 2)
            val firstY = previousPointY + lY * firstDistance

            val nextPointX = getChartWidth() * chartPoints[if (index + 1 < chartPoints.size) index + 1 else index].x
            val nextPointY = getChartHeight() - (getChartHeight() * chartPoints[if (index + 1 < chartPoints.size) index + 1 else index].y)

            // Distance between nextPoint and previousPoint (length of reference line)
            val secondDistance = sqrt((nextPointX - previousPointX).toDouble().pow(2.0) + (nextPointY - previousPointY).toDouble().pow(2.0)).toFloat()
            // (lX,lY) is the slope of the reference line
            lX = (nextPointX - previousPointX) / secondDistance * 0.3f
            lY = (nextPointY - previousPointY) / secondDistance * 0.3f

            // Maximum is used to avoid going too much left
            val secondX = max(currentPointX - lX * firstDistance, (previousPointX + currentPointX) / 2)
            val secondY = currentPointY - lY * firstDistance

            pathStroke.cubicTo(firstX, firstY, secondX, secondY, currentPointX, currentPointY)
        }

        pathFill.addPath(pathStroke)
        pathFill.lineTo(getChartWidth() * chartPoints[chartPoints.size - 1].x, getChartHeight())
        pathFill.lineTo(getChartWidth() * chartPoints[0].x, getChartHeight())
    }

    fun reset() {

        chartPoints.clear()
        pathStroke.rewind()
        pathFill.rewind()
        startTime = -1
        circlePoint = null
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

        private const val STROKE_WIDTH: Float = 3.0f
    }
}