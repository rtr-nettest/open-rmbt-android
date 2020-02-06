package at.rtr.rmbt.android.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import at.rtr.rmbt.android.R
import at.specure.data.NetworkTypeCompat
import at.specure.data.entity.TestResultGraphItemRecord
import kotlin.math.abs
import kotlin.math.ceil

class SignalBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : PingChartView(context, attrs), ResultChart {

    private var paintFill: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var maxValue: Int? = 0
    private var graphItems: List<TestResultGraphItemRecord>? = null

    override val paddingStringStub: String
        get() = "-100 dBmm"

    override val chartValueResource: Int
        get() = R.string.graph_signal_value

    init {

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SignalBarChartView)

        paintFill.color = typedArray.getColor(
            R.styleable.SignalBarChartView_bar_color,
            context.getColor(R.color.ping_bar_color)
        )
        paintFill.style = Paint.Style.FILL
        paintFill.isAntiAlias = true

        typedArray.recycle()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        graphItems?.let { items ->
            maxValue?.let {

                val barWidth = getChartWidth() / items.size
                val padding = barWidth / 4.0f
                for (index in items.indices) {

                    val left = padding + (barWidth * index)
                    val right = left + (barWidth / 2)
                    val top = getChartHeight() * (1.0f - (items[index].value.toFloat() / it.toFloat()))
                    val bottom = getChartHeight()

                    canvas?.drawRect(left, top, right, bottom, paintFill)
                }
            }
        }
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

    fun reset() {
        invalidate()
    }

    override fun onDetachedFromWindow() {
        reset()
        super.onDetachedFromWindow()
    }
}