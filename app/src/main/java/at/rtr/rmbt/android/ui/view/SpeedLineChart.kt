package at.rtr.rmbt.android.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
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
import androidx.core.content.withStyledAttributes


private const val RESULT_GRAPH_MISSING_SPEED_TIME_GAP_MILLISECONDS = 250L

@SuppressLint("CustomViewStyleable")
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

    private var rowCount = DEFAULT_NUMBER_OF_ROWS_IN_GRID
    init {

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SpeedLineChart)

        val lineChartTypedArray = context.obtainStyledAttributes(attrs, R.styleable.LineChart)
        try {
            // Access LineChart specific attributes
            rowCount = lineChartTypedArray.getInt(R.styleable.LineChart_grid_row, DEFAULT_NUMBER_OF_ROWS_IN_GRID)  // default value is 0
        } finally {
            lineChartTypedArray.recycle()
        }
        paintStroke.color = typedArray.getColor(
            R.styleable.SpeedLineChart_progress_line_color,
            context.getColor(R.color.colorAccent)
        )
        paintStroke.style = Paint.Style.STROKE
        paintStroke.strokeWidth = STROKE_WIDTH
        paintStroke.strokeCap = Paint.Cap.ROUND
        paintStroke.strokeJoin = Paint.Join.ROUND
        paintStroke.isAntiAlias = true

        pathStroke = Path()

        paintFill.color = typedArray.getColor(
            R.styleable.SpeedLineChart_progress_fill_color,
            context.getColor(R.color.speed_chart_progress_fill_color)
        )
        paintFill.style = Paint.Style.FILL
        paintFill.isAntiAlias = true

        pathFill = Path()

        typedArray.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (pathStroke.isEmpty && chartPoints.isNotEmpty()) {
            calculatePath()
        }
        canvas.drawPath(pathStroke, paintStroke)
        canvas.drawPath(pathFill, paintFill)
        circlePoint?.let {
            canvas.drawCircle(
                getChartWidth() * it.x - STROKE_WIDTH / 2.0f,
                getChartHeight() - (getChartHeight() * it.y),
                STROKE_WIDTH / 2,
                paintStroke
            )
        }
    }

    private fun removeLeadingZeroValues(graphItems: List<GraphItemRecord>?): List<GraphItemRecord>? {
        var foundNonZero = false
        return graphItems?.filter { graphItem ->
            if (foundNonZero) {
                true
            } else if (graphItem.value != 0L) {
                foundNonZero = true
                true
            } else {
                false
            }
        }
    }

    private fun removeLeadingZeroValuesForResult(graphItems: List<TestResultGraphItemRecord>?): List<TestResultGraphItemRecord>? {
        var foundNonZero = false
        return graphItems?.filter { graphItem ->
            if (foundNonZero) {
                true
            } else if (graphItem.value != 0L) {
                foundNonZero = true
                true
            } else {
                false
            }
        }
    }

    fun TestResultGraphItemRecord.toGraphItemRecord(graphItem: TestResultGraphItemRecord): GraphItemRecord {
        return GraphItemRecord(
            id = 0,
            testUUID = graphItem.testUUID,
            progress = graphItem.time.toInt(),
            value = graphItem.value,
            type = when (graphItem.type) {
                TestResultGraphItemRecord.Type.DOWNLOAD -> GraphItemRecord.GRAPH_ITEM_TYPE_DOWNLOAD
                TestResultGraphItemRecord.Type.UPLOAD -> GraphItemRecord.GRAPH_ITEM_TYPE_UPLOAD
                else -> throw IllegalArgumentException("Unknown graph item type: ${graphItem.type}")
            })
    }

    fun List<TestResultGraphItemRecord>.toSpeedGraphItems(): List<GraphItemRecord> {
        return this.map { it.toGraphItemRecord(it) }
    }

    fun addGraphItems(graphItems: List<GraphItemRecord>?) {

        pathStroke.rewind()
        pathFill.rewind()

        val filteredGraphItems = removeLeadingZeroValues(graphItems)
        Timber.d("Filtered item: ${(graphItems?.count() ?: 0) - (filteredGraphItems?.count() ?: 0)}")
        if (!filteredGraphItems.isNullOrEmpty()) {

            chartPoints = ArrayList()

            if (filteredGraphItems[0].progress > 0) {
                chartPoints.add(PointF(0.0f, toLog(filteredGraphItems[0].value)))
            }
            for (index in filteredGraphItems.indices) {

                val x = filteredGraphItems[index].progress / 100.0f
                val y = toLog(filteredGraphItems[index].value)

                chartPoints.add(PointF(x, y))
            }
        }
        invalidate()
    }

    override fun addServerResultGraphItems(graphItems: List<TestResultGraphItemRecord>?, networkType: NetworkTypeCompat) {

        pathStroke.rewind()
        pathFill.rewind()

        val filteredGraphItems = removeLeadingZeroValuesForResult(graphItems)?.addMissingDataBorderPoints()
        filteredGraphItems.let { items ->
            val maxTime = items?.maxByOrNull { it.time }?.time
            if (maxTime != null) {
                val differences: List<TestResultGraphItemRecord> = items.zipWithNext { prev, next ->
                    TestResultGraphItemRecord(
                        id = 0,
                        testUUID = next.testUUID,
                        time = ((next.time.toDouble() / maxTime.toDouble()) * 100).toLong(),          // time
                        value = if (next.value == -1L || prev.value == -1L) 0 else ((next.value - prev.value) * 8000) / (next.time - prev.time),       // speed Mbits / seconds
                        type = next.type,


                        isLocal = false
                    )
                }.groupBy {
                    it.time
                }.map {(time, items) ->
                    val avgValue = items.map { it.value }.average().toLong() // average of values
                    TestResultGraphItemRecord(
                        id = 0,
                        testUUID = items.first().testUUID, // keep the same testUUID
                        time = time,
                        value = avgValue,
                        type = items.first().type,         // keep the same type,
                        isLocal = false
                    )
                }
                val averages = differences.mapIndexed { index, record ->
                    record.copy(
                        value = averageAtIndex(differences, index, 3).toLong()
                    )
                }

                addGraphItems(averages.toSpeedGraphItems())
            }
        }

    }

    override fun addLocalResultGraphItems(
        graphItems: List<TestResultGraphItemRecord>?,
        networkType: NetworkTypeCompat
    ) {
        pathStroke.rewind()
        pathFill.rewind()

        val filteredGraphItems = removeLeadingZeroValuesForResult(graphItems)
        filteredGraphItems.let { items ->

            chartPoints = ArrayList()

            val maxValue = items?.maxByOrNull { it.time }?.time
            if (maxValue != null) {

                if (((items[0].time / maxValue.toFloat()) * 100.0f) > 0) {
                    chartPoints.add(PointF(0.0f, toLog(items[0].value * 8000 / items[0].time)))
                }

                for (index in items.indices) {
                    val x = items[index].time / maxValue.toFloat()
                    val y = toLog(items[index].value * 8000 / items[index].time)
                    chartPoints.add(PointF(x, y))
                    Timber.d("itemsdisplaytest x $x y $y width ${getChartWidth()} height ${getChartHeight()}")
                }
            }
        }
        invalidate()
    }

    fun averageAtIndex(data: List<TestResultGraphItemRecord>, index: Int, windowSize: Int): Double {
        require(index in data.indices) { "Index out of bounds" }

        val buffer = ArrayDeque<Long>()

        for (i in 0..index) {
            val value = data[i].value
            if (value == -1L) {
                buffer.clear() // reset buffer on sentinel
            } else {
                buffer.addLast(value)
                if (buffer.size > windowSize) {
                    buffer.removeFirst()
                }
            }
        }

        return if (buffer.isEmpty()) Double.NaN else buffer.average()
    }

    private fun List<TestResultGraphItemRecord>.addMissingDataBorderPoints(step: Long = RESULT_GRAPH_MISSING_SPEED_TIME_GAP_MILLISECONDS): List<TestResultGraphItemRecord> {
        val result = mutableListOf<TestResultGraphItemRecord>()

        for (i in indices) {
            val current = this[i]
            result.add(current)

            if (i < lastIndex) {
                val next = this[i + 1]
                val gap = next.time - current.time

                if (gap > step) {
                    var t = current.time + step
                    while (t < next.time) {
                        result.add(
                            current.copy(
                                id = 123,
                                time = t,
//                                value = current.value
                                value = -1
                            )
                        )
                        t += step
                    }
                }
            }
        }

        return result.sortedBy { it.time }
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
        return (if (value < 1e5) 0.0 else (2.0 + log10(value / 1e7)) / rowCount).toFloat()
    }

    override fun onDetachedFromWindow() {
        reset()
        super.onDetachedFromWindow()
    }

    companion object {

        private const val STROKE_WIDTH: Float = 3.0f
    }
}