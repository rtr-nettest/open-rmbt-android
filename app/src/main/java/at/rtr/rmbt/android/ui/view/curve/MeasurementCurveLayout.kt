package at.rtr.rmbt.android.ui.view.curve

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.LayoutDashBinding
import at.rtr.rmbt.android.databinding.LayoutMeasurementCurveBinding
import at.rtr.rmbt.android.databinding.LayoutPercentageBinding
import at.rtr.rmbt.android.databinding.LayoutSpeedBinding
import at.rtr.rmbt.android.ui.getBigDownloadIconAccordingToSpeed
import at.rtr.rmbt.android.ui.getBigUploadIconAccordingToSpeed
import at.rtr.rmbt.android.util.format
import at.specure.data.entity.LoopModeState
import at.specure.info.strength.SignalStrengthInfo
import at.specure.measurement.MeasurementState
import timber.log.Timber
import kotlin.math.min

class MeasurementCurveLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    private var lastSignalStrength: SignalStrengthInfo? = null
    private var downloadAlreadyMeasured: Boolean = false
    private var uploadAlreadyMeasured: Boolean = false
    private lateinit var speedLayout: LayoutSpeedBinding
    private lateinit var percentageLayout: LayoutPercentageBinding
    private lateinit var dashUpperLayout: LayoutDashBinding
    private lateinit var dashBottomLayout: LayoutDashBinding
    private lateinit var curveBinding: LayoutMeasurementCurveBinding
    private var inflater = LayoutInflater.from(context)

    private var topCenterX = 0
    private var topCenterY = 0
    private var bottomCenterX = 0
    private var bottomCenterY = 0

    private var isQoSEnabled = false

    private var currentTopProgress = 0
    private var currentBottomProgress = 0L

    private var loopState: LoopModeState = LoopModeState.RUNNING

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
        dashBottomLayout = LayoutDashBinding.inflate(inflater)
        dashUpperLayout = LayoutDashBinding.inflate(inflater)
        curveBinding.curveView.setSquareSizeCallback { squareSize, viewSize ->
            curveBinding.layoutStrength.strength.squareSize = squareSize
            (curveBinding.curveView.layoutParams as LayoutParams).apply {
                topMargin = (squareSize * 10).toInt()
                requestLayout()
            }

            // to prevent overlapping text size should be depent on curve circle size
            speedLayout.value.setTextSize(TypedValue.COMPLEX_UNIT_PX, (viewSize / VALUE_SIZE_DIVIDER).toFloat())
            speedLayout.units.setTextSize(TypedValue.COMPLEX_UNIT_PX, (viewSize / UNITS_SIZE_DIVIDER).toFloat())
            speedLayout.value.requestLayout()
            speedLayout.units.requestLayout()

            with(speedLayout.root) {
                (layoutParams as LayoutParams).apply {
                    leftMargin = (bottomCenterX * 0.875f).toInt()
                    topMargin = bottomCenterY + this@with.measuredHeight / TOP_MARGIN_DIVIDER
                }
                requestLayout()
            }

            // to prevent overlapping text size should be depent on curve circle size
            percentageLayout.percentage.setTextSize(TypedValue.COMPLEX_UNIT_PX, (viewSize / VALUE_SIZE_DIVIDER).toFloat())
            percentageLayout.units.setTextSize(TypedValue.COMPLEX_UNIT_PX, (viewSize / UNITS_SIZE_DIVIDER).toFloat())
            percentageLayout.percentage.requestLayout()
            percentageLayout.units.requestLayout()
        }

        curveBinding.curveView.setBottomCenterCallback { x, y ->
            bottomCenterX = x
            bottomCenterY = y

            with(dashBottomLayout.root) {
                (layoutParams as LayoutParams).apply {
                    leftMargin = bottomCenterX
                    topMargin = bottomCenterY + this@with.measuredHeight / TOP_MARGIN_DIVIDER
                }
                requestLayout()
            }

            setBottomProgress(currentBottomProgress)
        }
        curveBinding.curveView.setTopCenterCallback { x, y ->
            topCenterX = x
            topCenterY = y

            dashUpperLayout.root.post {
                with(dashUpperLayout.root) {
                    (layoutParams as LayoutParams).apply {
                        leftMargin = topCenterX - dashUpperLayout.root.measuredWidth / (2 * LEFT_MARGIN_DIVIDER)
                        topMargin = topCenterY + this@with.measuredHeight / TOP_MARGIN_DIVIDER
                    }
                }
                requestLayout()
            }

            setTopProgress(currentTopProgress)
        }

        addView(speedLayout.root, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        addView(percentageLayout.root, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        addView(dashUpperLayout.root, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        addView(dashBottomLayout.root, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        updateLoopRelatedData()
        setTopProgress(currentTopProgress)
        setBottomProgress(currentBottomProgress)
    }

    /**
     * Update the top part UI according to progress changing
     */
    fun setTopProgress(currentProgress: Int) {
        if (topCenterX != 0 && topCenterY != 0) {
            setSignalStrength(lastSignalStrength)
            currentTopProgress = currentProgress
            val progress = prepareProgressValueByPhase(currentProgress)
            curveBinding.curveView.setTopProgress(phase, currentProgress, isQoSEnabled)
            if (progress != progressOffsets[phase] && progress != 0) {
                percentageLayout.percentage.text = min(progress, 100).toString()
                percentageLayout.units.text = context.getString(R.string.measurement_progress_units)
                percentageLayout.percentage.requestLayout()
                percentageLayout.root.post {
                    with(percentageLayout.root) {
                        (layoutParams as LayoutParams).apply {
                            leftMargin = topCenterX - percentageLayout.percentage.measuredWidth / (2 * LEFT_MARGIN_DIVIDER)
                            topMargin = topCenterY + this@with.measuredHeight / TOP_MARGIN_DIVIDER
                        }
                    }
                    requestLayout()
                    if (currentProgress != 0) {
                        percentageLayout.root.visibility = View.VISIBLE
                        updateLoopRelatedData()
                    }
                }
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

    private fun getDownloadSpeedIconOrUnknown(progressInMbps: Float): Int {
        downloadAlreadyMeasured = (downloadAlreadyMeasured || progressInMbps > 0f)
        return if (downloadAlreadyMeasured) {
            getDownloadSpeedIconResId(progressInMbps)
        } else {
            R.drawable.ic_speed_download_gray
        }
    }

    private fun getUploadSpeedIconOrUnknown(progressInMbps: Float): Int {
        uploadAlreadyMeasured = (uploadAlreadyMeasured || progressInMbps > 0f)
        return if (uploadAlreadyMeasured) {
            getUploadSpeedIconResId(progressInMbps)
        } else {
            R.drawable.ic_speed_upload_gray
        }
    }

    /**
     * Update the bottom part UI according to progress changing
     */
    fun setBottomProgress(progress: Long) {
        if (phase == MeasurementState.DOWNLOAD || phase == MeasurementState.UPLOAD) {
            currentBottomProgress = progress
            curveBinding.curveView.setBottomProgress(phase, (progress * 1e-3).toInt(), isQoSEnabled)
            val progressInMbps: Float = progress / 1000000.0f
            speedLayout.icon.setImageResource(
                if (phase == MeasurementState.DOWNLOAD)
                    getDownloadSpeedIconOrUnknown(progressInMbps)
                else {
                    getUploadSpeedIconOrUnknown(progressInMbps)
                }
            )
            speedLayout.value.text = progressInMbps.format()
            speedLayout.units.text = context.getString(R.string.speed_progress_units)
            if (progress != 0L) {
                speedLayout.root.visibility = View.VISIBLE
                updateLoopRelatedData()
            }
        } else {
            currentBottomProgress = 0
            speedLayout.units.text = ""
            curveBinding.curveView.setBottomProgress(phase, 0, isQoSEnabled)
        }
    }

    fun getUploadSpeedIconResId(progressInMbps: Float): Int {
        return getBigUploadIconAccordingToSpeed((progressInMbps * 1_000_000L).toLong())
    }

    fun getDownloadSpeedIconResId(progressInMbps: Float): Int {
        return getBigDownloadIconAccordingToSpeed((progressInMbps * 1_000_000L).toLong())
    }

    /**
     * Update the signal strength bar UI according to progress changing
     */
    fun setSignalStrength(signalStrengthInfo: SignalStrengthInfo?) {
        lastSignalStrength = signalStrengthInfo
        if (signalStrengthInfo?.value != null && signalStrengthInfo.value != 0 && signalStrengthInfo.min != signalStrengthInfo.max) {
            with(curveBinding.layoutStrength) {
                root.visibility = View.VISIBLE
                strength.setSignalData(signalStrengthInfo.value ?: 0, signalStrengthInfo.min, signalStrengthInfo.max)
                strengthValue.text = context.getString(R.string.strength_signal_value, signalStrengthInfo.value ?: 0)
            }
        } else {
            with(curveBinding.layoutStrength) {
                root.visibility = View.INVISIBLE
                strength.setSignalData(-140, -140, -60)
                strengthValue.text = context.getString(R.string.strength_signal_value, -60)
                root.requestLayout()
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

    fun setLoopState(loopModeState: LoopModeState) {
        loopState = loopModeState
        Timber.d("update loop mode state $loopState")
        updateLoopRelatedData()
    }

    private fun clearPercentage() {
        percentageLayout.percentage.text = ""
        percentageLayout.units.text = ""
    }

    private fun clearSpeed() {
        speedLayout.icon.setImageResource(android.R.color.transparent)
        speedLayout.value.text = ""
        speedLayout.units.text = ""
    }

    private fun updateLoopRelatedData() {
        if (loopState == LoopModeState.IDLE) {
            setTopProgress(0)
            setBottomProgress(0)
            percentageLayout.root.visibility = View.INVISIBLE
            speedLayout.root.visibility = View.INVISIBLE
            dashBottomLayout.root.visibility = View.VISIBLE
            dashUpperLayout.root.visibility = View.VISIBLE
        } else {
            percentageLayout.root.visibility = View.VISIBLE
            if (phase == MeasurementState.DOWNLOAD || phase == MeasurementState.UPLOAD) {
                speedLayout.root.visibility = View.VISIBLE
            }
            dashBottomLayout.root.visibility = View.INVISIBLE
            dashUpperLayout.root.visibility = View.INVISIBLE
        }
    }

    companion object {
        private const val LEFT_MARGIN_DIVIDER = 2
        private const val TOP_MARGIN_DIVIDER = 8

        private const val VALUE_SIZE_DIVIDER = 10
        private const val UNITS_SIZE_DIVIDER = 25
    }
}