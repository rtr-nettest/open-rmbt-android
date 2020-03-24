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
    private var maxValue: Int? = 0
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
                    value = abs(it.value),
                    type = it.type,
                    testOpenUUID = it.testOpenUUID
                )
            }

            this.graphItems = items

            setYLabels(getYLabels(items))
        }

        invalidate()
    }

    private fun getYLabels(graphItems: List<TestResultGraphItemRecord>): Array<Int> {

        val gap = graphItems.let { list ->
            list.maxBy { it.value }?.let { item ->
                (ceil(item.value * 5 / 100.0) * 5).toInt()
            }
        }
        val gapList = Array(5) { i -> if (gap != null) (i * gap) else 0 }
        maxValue = gapList.maxBy { it } ?: 0
        return gapList
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
                        strokePath.moveTo(0f, this.value.toFloat())
                        strokePath.lineTo(time / widthMultiplier, this.value.toFloat())
                        fillPath.moveTo(0f, this.value.toFloat())
                        fillPath.lineTo(time / widthMultiplier, this.value.toFloat())
                        fillPath.lineTo(time / widthMultiplier, getChartHeight())
                        fillPath.lineTo(0f, getChartHeight())
                        fillPath.close()
                    }
                }
                else -> {
                    strokePath.moveTo(0f, it.first().value.toFloat())
                    fillPath.moveTo(0f, it.first().value.toFloat())
                    for (index in 1 until it.size) {
                        strokePath.quadTo(
                            (it[index - 1].time + (it[index].time - it[index - 1].time) / 2) / widthMultiplier,
                            it[index].value - (it[index].value - it[index - 1].value).toFloat() / 2,
                            it[index].time / widthMultiplier,
                            it[index].value.toFloat()
                        )
                        fillPath.quadTo(
                            (it[index - 1].time + (it[index].time - it[index - 1].time) / 2) / widthMultiplier,
                            it[index].value - (it[index].value - it[index - 1].value).toFloat() / 2,
                            it[index].time / widthMultiplier,
                            it[index].value.toFloat()
                        )
                    }
                    fillPath.lineTo(it.last().time / widthMultiplier, getChartHeight())
                    fillPath.lineTo(0f, getChartHeight())
                    fillPath.close()
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        invalidate()
        super.onDetachedFromWindow()
    }
}