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
import at.specure.measurement.MeasurementState
import kotlin.math.roundToInt

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
    private var curveCircleSize = 0

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
        put(MeasurementState.UPLOAD, 0.25f)
        put(MeasurementState.QOS, 0.25f)
        // todo check QoS part
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
        put(MeasurementState.PING, 15)
        put(MeasurementState.DOWNLOAD, 29)
        put(MeasurementState.UPLOAD, 50)
        put(MeasurementState.QOS, 75)
        // todo check QoS part
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

            curveCircleSize = viewSize
            (curveBinding.curveView.layoutParams as LayoutParams).apply {
                topMargin = viewSize / TOP_MARGIN_DIVIDER
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

    /**
     * Update the top part UI according to progress changing
     */
    fun setTopProgress(currentProgress: Int) {
        if (topCenterX != 0 && topCenterY != 0 && currentProgress != 0) {
            val progress = prepareProgressValueByPhase(currentProgress)
            curveBinding.curveView.setTopProgress(currentProgress)
            percentageLayout.percentage.text = progress.toString()
            percentageLayout.units.text = context.getString(R.string.measurement_progress_units)
            percentageLayout.percentage.requestLayout()
            percentageLayout.root.post {
                with(percentageLayout.root) {
                    (layoutParams as LayoutParams).apply {
                        leftMargin = topCenterX - percentageLayout.percentage.measuredWidth / LEFT_MARGIN_DIVIDER
                        topMargin = topCenterY - (this@with.measuredHeight - curveCircleSize / 2) / TOP_MARGIN_DIVIDER
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
            val progressInKbit = (progress * 1e-3).toInt()
            curveBinding.curveView.setBottomProgress(progressInKbit)
            if (progressInKbit <= 1000) { // kbit/s
                speedLayout.value.text = (progressInKbit).toString()
                speedLayout.units.text = context.getString(R.string.speed_progress_units_kbit)
            } else { // Mbit/s
                speedLayout.value.text = ((progress * 1e-6).roundToInt()).toString()
                speedLayout.units.text = context.getString(R.string.speed_progress_units_mbit)
            }
            speedLayout.value.requestLayout()
            with(speedLayout.root) {
                (layoutParams as LayoutParams).apply {
                    leftMargin = bottomCenterX - speedLayout.value.measuredWidth / LEFT_MARGIN_DIVIDER
                    topMargin = bottomCenterY - (this@with.measuredHeight - curveCircleSize / 2) / TOP_MARGIN_DIVIDER
                }
            }
            requestLayout()
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

    companion object {
        private const val LEFT_MARGIN_DIVIDER = 2
        private const val TOP_MARGIN_DIVIDER = 8

        private const val VALUE_SIZE_DIVIDER = 10
        private const val UNITS_SIZE_DIVIDER = 25
    }
}