package at.rtr.rmbt.android.ui.view

import android.content.Context
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Canvas
import android.util.AttributeSet
import at.rtr.rmbt.android.R
import at.specure.data.entity.GraphItemRecord
import timber.log.Timber
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.min
import kotlin.math.max

class SignalStrengthChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SignalStrengthChartView(context, attrs) {

    private var pathStroke: Path
    private var paintStroke: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var pathFill: Path
    private var paintFill: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var startTime: Long = -1

    var downloadTimeWindowMin: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var downloadTimeWindowMax: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var uploadTimeWindowMin: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var uploadTimeWindowMax: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    init {

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SpeedLineChart)

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

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawPath(pathStroke, paintStroke)
        canvas?.drawPath(pathFill, paintFill)

        canvas?.drawRect(downloadTimeWindowMin*getChartWidth(), 0.0f,
               downloadTimeWindowMax*getChartWidth(), getChartHeight(), paintFill);

        canvas?.drawRect(uploadTimeWindowMin*getChartWidth(), 0.0f,
            uploadTimeWindowMax*getChartWidth(), getChartHeight(), paintFill);
    }

    /**
     * This function is use for calculate path
     */
    fun addGraphItems(graphItems: List<GraphItemRecord>?) {

        pathStroke.rewind()
        pathFill.rewind()
        if (graphItems != null && graphItems.isNotEmpty()) {

            val points: ArrayList<PointF> = ArrayList()
            if (graphItems[0].progress > 0) {
                points.add(PointF(0.0f, getChartHeight() - (getChartHeight() * getRelativeValue(graphItems[0].value))))
            }
            for (index in 0 until graphItems.size) {

                val x = getChartWidth() * graphItems[index].progress / 100.0f
                val y = getChartHeight() - (getChartHeight() * getRelativeValue(graphItems[index].value))
                points.add(PointF(x, y))
                Timber.d("valuetest x $x y $y relativeValue ")
            }

            var lX = 0f
            var lY = 0f
            pathStroke.moveTo(points[0].x, points[0].y)
            for (index in 1 until points.size) {
                val currentPoint = points[index]
                val previousPoint = points[index - 1]
                // Distance between currentPoint and previousPoint
                val firstDistance = sqrt((currentPoint.x - previousPoint.x).toDouble().pow(2.0) + (currentPoint.y - previousPoint.y).toDouble().pow(2.0)).toFloat()

                // Minimum is used to avoid going too much right
                val firstX = min(previousPoint.x + lX * firstDistance, (previousPoint.x + currentPoint.x) / 2)
                val firstY = previousPoint.y + lY * firstDistance

                val nextPoint = points[if (index + 1 < points.size) index + 1 else index]
                // Distance between nextPoint and previousPoint (length of reference line)
                val secondDistance = sqrt((nextPoint.x - previousPoint.x).toDouble().pow(2.0) + (nextPoint.y - previousPoint.y).toDouble().pow(2.0)).toFloat()
                // (lX,lY) is the slope of the reference line
                lX = (nextPoint.x - previousPoint.x) / secondDistance * 0.3f
                lY = (nextPoint.y - previousPoint.y) / secondDistance * 0.3f

                // Maximum is used to avoid going too much left
                val secondX = max(currentPoint.x - lX * firstDistance, (previousPoint.x + currentPoint.x) / 2)
                val secondY = currentPoint.y - lY * firstDistance

                pathStroke.cubicTo(firstX, firstY, secondX, secondY, currentPoint.x, currentPoint.y)
            }

            pathFill.addPath(pathStroke)
            pathFill.lineTo(points[points.size - 1].x, getChartHeight())
            pathFill.lineTo(points[0].x, getChartHeight())
        }
        invalidate()
    }

    fun reset() {

        pathStroke.rewind()
        pathFill.rewind()
        startTime = -1
        invalidate()
    }

    /**
     * This function is used for convert signal value to 0..1
     */

    private fun getRelativeValue(value: Long): Float {
        return 1.0f - ((value-yMax.toFloat()) / (yMin.toFloat()-yMax.toFloat()))
    }


    override fun onDetachedFromWindow() {
        reset()
        super.onDetachedFromWindow()
    }

    companion object {
        private const val STROKE_WIDTH: Float = 3.0f
    }

}