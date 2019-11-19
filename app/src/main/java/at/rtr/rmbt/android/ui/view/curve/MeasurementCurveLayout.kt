package at.rtr.rmbt.android.ui.view.curve

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.LayoutMeasurementCurveBinding
import at.rtr.rmbt.android.databinding.LayoutPercentageBinding
import at.rtr.rmbt.android.databinding.LayoutSpeedBinding

class MeasurementCurveLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var speedLayout: LayoutSpeedBinding
    private lateinit var percentageLayout: LayoutPercentageBinding
    private lateinit var curveBinding: LayoutMeasurementCurveBinding
    private var inflater = LayoutInflater.from(context)

    private var topCenterX = 0
    private var topCenterY = 0
    private var bottomCenterX = 0
    private var bottomCenterY = 0

    override fun onFinishInflate() {
        super.onFinishInflate()
        curveBinding = LayoutMeasurementCurveBinding.inflate(inflater)
        addView(curveBinding.root)

        speedLayout = LayoutSpeedBinding.inflate(inflater)
        percentageLayout = LayoutPercentageBinding.inflate(inflater)
        curveBinding.curveView.setSquareSizeCallback {
            curveBinding.layoutStrength.strength.squareSize = it
            (curveBinding.curveView.layoutParams as LayoutParams).apply {
                topMargin = (SIGNAL_BAR_OFFSET_MULTIPLIER * it).toInt()
                requestLayout()
            }
        }
        curveBinding.curveView.setBottomCenterCallback { x, y ->
            bottomCenterX = x
            bottomCenterY = y - curveBinding.curveView.paddingTop
        }
        curveBinding.curveView.setTopCenterCallback { x, y ->
            topCenterX = x
            topCenterY = y - curveBinding.curveView.paddingTop
        }
        addView(speedLayout.root, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        addView(percentageLayout.root, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }

    fun setTopProgress(progress: Int) {
        if (topCenterX != 0 && topCenterY != 0 && progress != 0) {
            curveBinding.curveView.setTopProgress(progress)
            percentageLayout.percentage.text = progress.toString()
            percentageLayout.units.text = context.getString(R.string.measurement_progress_units)
            percentageLayout.percentage.requestLayout()
            percentageLayout.root.post {
                with(percentageLayout.root) {
                    (layoutParams as LayoutParams).apply {
                        leftMargin = topCenterX - percentageLayout.percentage.measuredWidth / LEFT_MARGIN_DIVIDER
                        topMargin = topCenterY - this@with.measuredHeight / TOP_MARGIN_DIVIDER
                    }
                }
                requestLayout()
                percentageLayout.root.visibility = View.VISIBLE
            }
        }
    }

    fun setBottomProgress(progress: Long) {
        curveBinding.curveView.setBottomProgress((progress * 1e-6).toInt())
        if (bottomCenterX != 0 && bottomCenterY != 0) {
            speedLayout.value.text = ((progress * 1e-6).toInt()).toString()
            speedLayout.value.requestLayout()
            speedLayout.units.text = context.getString(R.string.speed_progress_units) // todo calculate units while set progress
            with(speedLayout.root) {
                (layoutParams as LayoutParams).apply {
                    leftMargin = bottomCenterX - speedLayout.value.measuredWidth / LEFT_MARGIN_DIVIDER
                    topMargin = bottomCenterY - this@with.measuredHeight / TOP_MARGIN_DIVIDER
                }
            }
            requestLayout()
            speedLayout.root.visibility = View.VISIBLE
        }
    }

    fun setSignalStrength(signalLevel: Int, minValue: Int, maxValue: Int) {
        if (minValue != maxValue) {
            with(curveBinding.layoutStrength) {
                root.visibility = View.VISIBLE
                strength.setSignalData(signalLevel, minValue, maxValue)
                strengthValue.text = context.getString(R.string.strength_signal_value, signalLevel)
            }
        }
    }

    companion object {
        private const val LEFT_MARGIN_DIVIDER = 2
        private const val TOP_MARGIN_DIVIDER = 5
        private const val SIGNAL_BAR_OFFSET_MULTIPLIER = 25
    }
}