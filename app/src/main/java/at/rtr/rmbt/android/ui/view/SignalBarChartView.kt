package at.rtr.rmbt.android.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import at.rtr.rmbt.android.R
import at.specure.data.NetworkTypeCompat
import at.specure.data.entity.TestResultGraphItemRecord
import kotlin.math.abs
import kotlin.math.ceil

@SuppressLint("CustomViewStyleable")
class SignalBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : PingChartView(context, attrs), ResultChart {

    private var linePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private var fillPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
    }

    /**
     *  distance in dBm between signal values shown in the graph
     */
    private val gap = 30

    /**
     *  Values of dBm shown in the graph
     */
    private val labelsYAxis = Array(5) { i -> -(5 - i) * gap + 10 }

    /**
     * value of X-axis at bottom of the graph
     */
    private var minValue: Int = labelsYAxis.minBy { it }!!
    private var maxValue: Int = labelsYAxis.maxBy { it }!!
    private var strokePath: Path = Path()
    private var fillPath: Path = Path()
    private var widthMultiplier: Float = 0f
    private var graphItems: List<TestResultGraphItemRecord>? = null
        set(value) {
            field = value
            calculatePath()
            invalidate()
        }

    override val paddingStringStub: String
        get() = "-100 dBmm"

    override val chartValueResource: Int
        get() = R.string.graph_signal_value

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SpeedLineChart)
        fillPaint.color = typedArray.getColor(R.styleable.SpeedLineChart_progress_fill_color, context.getColor(R.color.ping_bar_color))
        linePaint.color = typedArray.getColor(R.styleable.SpeedLineChart_progress_line_color, context.getColor(R.color.ping_bar_color))

        typedArray.recycle()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawPath(fillPath, fillPaint)
        canvas?.drawPath(strokePath, linePaint)
    }

    /**
     * This function is use for calculate path
     */
    override fun addResultGraphItems(graphItems: List<TestResultGraphItemRecord>?, networkType: NetworkTypeCompat) {
        if (!graphItems.isNullOrEmpty()) {
            val items = graphItems.map {
                TestResultGraphItemRecord(
                    id = it.id,
                    time = it.time,
                    value = it.value,
                    type = it.type,
                    testUUID = it.testUUID
                )
            }

            this.graphItems = items
/*            val fix: Long = 140
            var multiplicator = 1
            this.graphItems = mutableListOf<TestResultGraphItemRecord>()
            this.graphItems?.add(TestResultGraphItemRecord(
                id = 0,
                time = items[0].time + multiplicator++ * 1000,
                value = -140 + fix,
                type = items[0].type,
                testUUID = items[0].testUUID
            ))
            this.graphItems?.add(TestResultGraphItemRecord(
                id = 0,
                time = items[0].time + multiplicator++ * 1000,
                value = -110 + fix,
                type = items[0].type,
                testUUID = items[0].testUUID
            ))

            this.graphItems?.add(TestResultGraphItemRecord(
                id = 0,
                time = items[0].time + multiplicator++ * 1000,
                value = -80 + fix,
                type = items[0].type,
                testUUID = items[0].testUUID
            ))

            this.graphItems?.add(TestResultGraphItemRecord(
                id = 0,
                time = items[0].time + multiplicator++ * 1000,
                value = -50 + fix,
                type = items[0].type,
                testUUID = items[0].testUUID
            ))
            this.graphItems?.add(TestResultGraphItemRecord(
                id = 0,
                time = items[0].time + multiplicator++ * 1000,
                value = -20 + fix,
                type = items[0].type,
                testUUID = items[0].testUUID
            ))

            calculatePath()*/

            setYLabels(labelsYAxis)
        }

        invalidate()
    }

    private fun calculatePath() {
        graphItems?.let {
            widthMultiplier = it.last().time / getChartWidth()
            strokePath.reset()
            fillPath.reset()
            when {
                it.isEmpty() -> {
                }
                it.size == 1 -> {
                    with(it.first()) {
                        val correctHeight = countCorrectHeight(this.value)
                        strokePath.moveTo(0f, correctHeight)
                        strokePath.lineTo(time / widthMultiplier, correctHeight)
                        fillPath.moveTo(0f, correctHeight)
                        fillPath.lineTo(time / widthMultiplier, correctHeight)
                        fillPath.lineTo(time / widthMultiplier, getChartHeight())
                        fillPath.lineTo(0f, getChartHeight())
                        fillPath.close()
                    }
                }
                else -> {
                    strokePath.moveTo(0f, it.first().value.toFloat())
                    fillPath.moveTo(0f, it.first().value.toFloat())
                    for (index in 1 until it.size) {
                        val currentValue = countCorrectHeight(it[index].value)
                        val previousValue = countCorrectHeight(it[index - 1].value)
                        strokePath.quadTo(
                            (it[index - 1].time + (it[index].time - it[index - 1].time) / 2) / widthMultiplier,
                            currentValue - (currentValue - previousValue) / 2,
                            it[index].time / widthMultiplier,
                            currentValue
                        )
                        fillPath.quadTo(
                            (it[index - 1].time + (it[index].time - it[index - 1].time) / 2) / widthMultiplier,
                            currentValue - (currentValue - previousValue) / 2,
                            it[index].time / widthMultiplier,
                            currentValue
                        )
                    }
                    fillPath.lineTo(it.last().time / widthMultiplier, getChartHeight())
                    fillPath.lineTo(0f, getChartHeight())
                    fillPath.close()
                }
            }
        }
    }

    private fun countCorrectHeight(value: Long): Float {
        return ((((abs(value) - abs(maxValue))) / (maxValue - minValue).toFloat()) * getChartHeight())
    }

    override fun onDetachedFromWindow() {
        invalidate()
        super.onDetachedFromWindow()
    }
}