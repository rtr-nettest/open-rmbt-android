package at.rtr.rmbt.android.ui.view.curve

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.LayoutMeasurementCurveBinding
import at.rtr.rmbt.android.databinding.LayoutPercentageBinding
import at.rtr.rmbt.android.databinding.LayoutSpeedBinding
import at.rtr.rmbt.android.util.format
import at.specure.measurement.MeasurementState

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

    private var isQoSEnabled = false

    /**
     * Defines the current phase of measurement
     */
    private var phase: MeasurementState = MeasurementState.IDLE
        set(value) {
            field = value
            if (field != MeasurementState.IDLE && field != MeasurementState.FINISH) {
                speedLayout.root.visibility = View.INVISIBLE
            }
        }

    /**
     * Defines coefficients to calculation progress value for each phase of measurement when QoS is disabled
     */
    private var progressCoefficients = LinkedHashMap<MeasurementState, Float>().apply {
        put(MeasurementState.INIT, 0.2f)
        put(MeasurementState.PING, 0.2f)
        put(MeasurementState.DOWNLOAD, 0.3f)
        put(MeasurementState.UPLOAD, 0.3f)
    }

    /**
     * Defines coefficients to calculation progress value for each phase of measurement when QoS is enabled
     */
    private var progressCoefficientsQoS = LinkedHashMap<MeasurementState, Float>().apply {
        put(MeasurementState.INIT, 0.15f)
        put(MeasurementState.PING, 0.15f)
        put(MeasurementState.DOWNLOAD, 0.2f)
        put(MeasurementState.UPLOAD, 0.24f)
        put(MeasurementState.QOS, 0.25f)
    }

    /**
     * Defines offsets according to previous measurement phases when QoS is disabled
     */
    private var progressOffsets = LinkedHashMap<MeasurementState, Int>().apply {
        put(MeasurementState.INIT, 0)
        put(MeasurementState.PING, 20)
        put(MeasurementState.DOWNLOAD, 40)
        put(MeasurementState.UPLOAD, 70)
    }

    /**
     * Defines offsets according to previous measurement phases when QoS is enabled
     */
    private var progressOffsetsQoS = LinkedHashMap<MeasurementState, Int>().apply {
        put(MeasurementState.INIT, 0)
        put(MeasurementState.PING, 16)
        put(MeasurementState.DOWNLOAD, 30)
        put(MeasurementState.UPLOAD, 51)
        put(MeasurementState.QOS, 76)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        curveBinding = LayoutMeasurementCurveBinding.inflate(inflater)
        addView(curveBinding.root)

        speedLayout = LayoutSpeedBinding.inflate(inflater)
        percentageLayout = LayoutPercentageBinding.inflate(inflater)
        curveBinding.curveView.setSquareSizeCallback { squareSize, viewSize ->
            curveBinding.layoutStrength.strength.squareSize = squareSize

            // to prevent overlapping text size should be depent on curve circle size
            speedLayout.value.setTextSize(TypedValue.COMPLEX_UNIT_PX, (viewSize / VALUE_SIZE_DIVIDER).toFloat())
            speedLayout.units.setTextSize(TypedValue.COMPLEX_UNIT_PX, (viewSize / UNITS_SIZE_DIVIDER).toFloat())
            speedLayout.value.requestLayout()
            speedLayout.units.requestLayout()

            // to prevent overlapping text size should be depent on curve circle size
            percentageLayout.percentage.setTextSize(TypedValue.COMPLEX_UNIT_PX, (viewSize / VALUE_SIZE_DIVIDER).toFloat())
            percentageLayout.units.setTextSize(TypedValue.COMPLEX_UNIT_PX, (viewSize / UNITS_SIZE_DIVIDER).toFloat())
            percentageLayout.percentage.requestLayout()
            percentageLayout.units.requestLayout()
        }

        curveBinding.curveView.setBottomCenterCallback { x, y ->
            bottomCenterX = x
            bottomCenterY = y

            // update bottom circle center Y coordinate with signal bar margin
            curveBinding.layoutStrength.root.post {
                (curveBinding.curveView.layoutParams as LayoutParams).apply {
                    topMargin = curveBinding.layoutStrength.root.height / SIGNAL_BAR_OFFSET_DIVIDER
                    bottomCenterY = y + topMargin / 3
                    requestLayout()
                }
            }
        }
        curveBinding.curveView.setTopCenterCallback { x, y ->
            topCenterX = x
            topCenterY = y

            // update top circle center Y coordinate with signal bar margin
            curveBinding.layoutStrength.root.post {
                (curveBinding.curveView.layoutParams as LayoutParams).apply {
                    topMargin = curveBinding.layoutStrength.root.height / SIGNAL_BAR_OFFSET_DIVIDER
                    topCenterY = y + topMargin / 3
                    requestLayout()
                }
            }
        }

        addView(speedLayout.root, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        addView(percentageLayout.root, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }

    /**
     * Update the top part UI according to progress changing
     */
    fun setTopProgress(currentProgress: Int) {
        if (topCenterX != 0 && topCenterY != 0 && currentProgress != 0) {
            val progress = prepareProgressValueByPhase(currentProgress)
            curveBinding.curveView.setTopProgress(currentProgress, isQoSEnabled)
            percentageLayout.percentage.text = progress.toString()
            percentageLayout.units.text = context.getString(R.string.measurement_progress_units)
            percentageLayout.percentage.requestLayout()
            percentageLayout.root.post {
                with(percentageLayout.root) {
                    (layoutParams as LayoutParams).apply {
                        leftMargin = topCenterX - percentageLayout.percentage.measuredWidth / (2 * LEFT_MARGIN_DIVIDER)
                        topMargin = topCenterY - this@with.measuredHeight / TOP_MARGIN_DIVIDER
                    }
                }
                requestLayout()
                percentageLayout.root.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Calculate value for label with percents
     */
    private fun prepareProgressValueByPhase(progress: Int): Int =
        if (isQoSEnabled) {
            (progressOffsetsQoS[phase] ?: 0) + ((progressCoefficientsQoS[phase] ?: 0f) * progress).toInt()
        } else {
            (progressOffsets[phase] ?: 0) + ((progressCoefficients[phase] ?: 0f) * progress).toInt()
        }

    /**
     * Update the bottom part UI according to progress changing
     */
    fun setBottomProgress(progress: Long) {
        if (phase == MeasurementState.DOWNLOAD || phase == MeasurementState.UPLOAD) {
            speedLayout.icon.setImageResource(if (phase == MeasurementState.DOWNLOAD) R.drawable.ic_speed_download else R.drawable.ic_speed_upload)
            curveBinding.curveView.setBottomProgress((progress * 1e-3).toInt(), isQoSEnabled)
            val progressInMbps: Float = progress / 1000000.0f
            speedLayout.value.text = progressInMbps.format()

            /*when {
                progress >= 1e7 -> speedLayout.value.text = ((progress * 1e-6).roundToInt()).toString()
                progress >= 1e6 -> speedLayout.value.text = (BigDecimal(progress * 1e-6).setScale(1, RoundingMode.HALF_EVEN)).toPlainString()
                else -> { // up to 1 mbit
                    var scale = 1
                    var divider = 1
                    var tmpProgress = progress
                    while (tmpProgress > 100) {
                        tmpProgress /= 10
                        divider *= 10
                        scale++
                    }
                    speedLayout.value.text =
                        (BigDecimal(tmpProgress * divider * 1e-6).setScale(scale + 2, RoundingMode.HALF_EVEN)).stripTrailingZeros().toPlainString()
                }
            }*/
            speedLayout.value.requestLayout()
            with(speedLayout.root) {
                (layoutParams as LayoutParams).apply {
                    leftMargin = bottomCenterX - speedLayout.value.measuredWidth / LEFT_MARGIN_DIVIDER
                    topMargin = bottomCenterY - this@with.measuredHeight / TOP_MARGIN_DIVIDER
                }
                requestLayout()
            }
            speedLayout.root.visibility = View.VISIBLE
        }
    }

    /**
     * Update the signal strength bar UI according to progress changing
     */
    fun setSignalStrength(signalLevel: Int, minValue: Int, maxValue: Int) {
        if (minValue != maxValue) {
            with(curveBinding.layoutStrength) {
                root.visibility = View.VISIBLE
                strength.setSignalData(signalLevel, minValue, maxValue)
                strengthValue.text = context.getString(R.string.strength_signal_value, signalLevel)
            }
        }
    }

    fun setMeasurementState(state: MeasurementState) {
        phase = state
        curveBinding.curveView.setMeasurementState(state)
    }

    fun setQoSEnabled(enabled: Boolean) {
        isQoSEnabled = enabled
    }

    companion object {
        private const val LEFT_MARGIN_DIVIDER = 2
        private const val TOP_MARGIN_DIVIDER = 8
        private const val SIGNAL_BAR_OFFSET_DIVIDER = 6

        private const val VALUE_SIZE_DIVIDER = 10
        private const val UNITS_SIZE_DIVIDER = 25
    }
}