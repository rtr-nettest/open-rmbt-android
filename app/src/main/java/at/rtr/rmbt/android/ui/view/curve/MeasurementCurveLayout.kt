package at.rtr.rmbt.android.ui.view.curve

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Choreographer
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
    // Displayed (eased) arc value; chases [targetBottomProgress] frame-by-frame.
    private var currentBottomProgress = 0L
    // Latest sliding-window value from the ~100ms poll; the poll only writes this (no animator churn).
    private var targetBottomProgress = 0L
    // Final-algorithm value shown as the numeric speed in the middle of the curve (not animated),
    // so it matches the bottom-view numbers instead of the sliding value driving the arc.
    private var currentBottomSpeedValue = 0L
    // Frame-synced ticker that eases the arc toward the target, decoupled from the data poll so the
    // animation setup never collides with the measurement work on the main thread. Display-only.
    private var bottomTickerRunning = false

    private var loopState: LoopModeState = LoopModeState.RUNNING

    private val isLandscape: Boolean
        get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

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

    init {
        // The curve is drawn on the canvas in absolute (left-based) coordinates and the speed /
        // percentage / dash overlays are positioned via leftMargin computed from those
        // coordinates. In RTL a FrameLayout anchors children to the right edge and ignores
        // leftMargin, which misplaces the values. Keep this gauge laid out left-to-right so the
        // overlays line up with the curve regardless of the app's layout direction.
        layoutDirection = LAYOUT_DIRECTION_LTR
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
            // In landscape the curve is height-constrained, so don't waste vertical space with a
            // large top margin (it would shrink the whole curve). Portrait keeps the offset.
            val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            (curveBinding.curveView.layoutParams as LayoutParams).apply {
                topMargin = if (isLandscape) 0 else (squareSize * 10).toInt()
                requestLayout()
            }

            // to prevent overlapping text size should be depent on curve circle size
            speedLayout.value.setTextSize(TypedValue.COMPLEX_UNIT_PX, (viewSize / VALUE_SIZE_DIVIDER).toFloat())
            speedLayout.units.setTextSize(TypedValue.COMPLEX_UNIT_PX, (viewSize / UNITS_SIZE_DIVIDER).toFloat())
            speedLayout.value.requestLayout()
            speedLayout.units.requestLayout()

            with(speedLayout.root) {
                (layoutParams as LayoutParams).apply {
                    if (isLandscape) {
                        // Center the speed value on the bottom loop center.
                        leftMargin = bottomCenterX - speedLayout.value.measuredWidth / 2
                        topMargin = bottomCenterY - speedLayout.value.measuredHeight / 2
                    } else {
                        leftMargin = (bottomCenterX * 0.875f).toInt()
                        topMargin = bottomCenterY + this@with.measuredHeight / TOP_MARGIN_DIVIDER
                    }
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
                    if (isLandscape) {
                        leftMargin = bottomCenterX - this@with.measuredWidth / 2
                        topMargin = bottomCenterY - this@with.measuredHeight / 2
                    } else {
                        leftMargin = bottomCenterX
                        topMargin = bottomCenterY + this@with.measuredHeight / TOP_MARGIN_DIVIDER
                    }
                }
                requestLayout()
            }

            setBottomProgress(currentBottomProgress)
            setBottomSpeedValue(currentBottomSpeedValue)
        }
        curveBinding.curveView.setTopCenterCallback { x, y ->
            topCenterX = x
            topCenterY = y

            dashUpperLayout.root.post {
                with(dashUpperLayout.root) {
                    (layoutParams as LayoutParams).apply {
                        if (isLandscape) {
                            leftMargin = topCenterX - dashUpperLayout.root.measuredWidth / 2
                            topMargin = topCenterY - this@with.measuredHeight / 2
                        } else {
                            leftMargin = topCenterX - dashUpperLayout.root.measuredWidth / (2 * LEFT_MARGIN_DIVIDER)
                            topMargin = topCenterY + this@with.measuredHeight / TOP_MARGIN_DIVIDER
                        }
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
        setBottomSpeedValue(currentBottomSpeedValue)
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
                            if (isLandscape) {
                                // Center the percentage number on the top loop center.
                                leftMargin = topCenterX - percentageLayout.percentage.measuredWidth / 2
                                topMargin = topCenterY - this@with.measuredHeight / 2
                            } else {
                                leftMargin = topCenterX - percentageLayout.percentage.measuredWidth / (2 * LEFT_MARGIN_DIVIDER)
                                topMargin = topCenterY + this@with.measuredHeight / TOP_MARGIN_DIVIDER
                            }
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
     * Entry point for a new speed value (called ~every 100 ms from the data poll). It only records
     * the target and makes sure the frame ticker is running - it does NOT create/cancel an animator
     * per call, so the animation no longer contends with the measurement work on the main thread.
     * A frame-synced ticker ([bottomTickerCallback]) then eases the arc toward the target.
     * Non-speed phases apply immediately.
     */
    fun setBottomProgress(progress: Long) {
        targetBottomProgress = progress
        val isSpeedPhase = phase == MeasurementState.DOWNLOAD || phase == MeasurementState.UPLOAD
        if (!isSpeedPhase) {
            stopBottomTicker()
            currentBottomProgress = progress
            applyBottomProgress(progress)
            return
        }
        if (!bottomTickerRunning && currentBottomProgress != targetBottomProgress) {
            bottomTickerRunning = true
            Choreographer.getInstance().postFrameCallback(bottomTickerCallback)
        }
    }

    /**
     * Runs once per display frame while the arc is catching up to [targetBottomProgress]. Uses simple
     * exponential easing so a jumping target produces smooth motion; stops itself once the arc reaches
     * the target (idle), and is restarted by [setBottomProgress] when a new, different value arrives.
     */
    private val bottomTickerCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val diff = targetBottomProgress - currentBottomProgress
            val step = (diff * BOTTOM_PROGRESS_SMOOTHING).toLong()
            val next = if (step == 0L) targetBottomProgress else currentBottomProgress + step
            if (next != currentBottomProgress) {
                currentBottomProgress = next
                applyBottomProgress(currentBottomProgress)
            }
            if (bottomTickerRunning && currentBottomProgress != targetBottomProgress) {
                Choreographer.getInstance().postFrameCallback(this)
            } else {
                bottomTickerRunning = false
            }
        }
    }

    private fun stopBottomTicker() {
        if (bottomTickerRunning) {
            Choreographer.getInstance().removeFrameCallback(bottomTickerCallback)
            bottomTickerRunning = false
        }
    }

    /**
     * Moves the arc of the bottom "S"-curve. Driven by the sliding-window value and animated, so the
     * arc shows how throughput changes over time. The numeric speed is set separately (and unanimated)
     * via [setBottomSpeedValue] so the two "current speed" numbers on screen agree.
     */
    private fun applyBottomProgress(progress: Long) {
        if (phase == MeasurementState.DOWNLOAD || phase == MeasurementState.UPLOAD) {
            currentBottomProgress = progress
            curveBinding.curveView.setBottomProgress(phase, (progress * 1e-3).toInt(), isQoSEnabled)
        } else {
            currentBottomProgress = 0
            curveBinding.curveView.setBottomProgress(phase, 0, isQoSEnabled)
        }
    }

    /**
     * Sets the numeric speed shown in the middle of the bottom "S"-curve. Uses the FINAL-algorithm
     * value (like the bottom-view numbers) rather than the sliding value driving the arc, so both
     * "current speed" numbers agree. Not animated - it is a stable number.
     */
    fun setBottomSpeedValue(value: Long) {
        currentBottomSpeedValue = value
        if (phase == MeasurementState.DOWNLOAD || phase == MeasurementState.UPLOAD) {
            val speedInMbps: Float = value / 1000000.0f
            speedLayout.icon.setImageResource(
                if (phase == MeasurementState.DOWNLOAD)
                    getDownloadSpeedIconOrUnknown(speedInMbps)
                else {
                    getUploadSpeedIconOrUnknown(speedInMbps)
                }
            )
            speedLayout.value.text = speedInMbps.format()
            speedLayout.units.text = context.getString(R.string.speed_progress_units)
            // In landscape, re-center the speed value on the bottom loop center as its width
            // changes with the value (portrait keeps its fixed position from the size callback).
            if (isLandscape) {
                speedLayout.root.post {
                    (speedLayout.root.layoutParams as LayoutParams).apply {
                        leftMargin = bottomCenterX - speedLayout.value.measuredWidth / 2
                        topMargin = bottomCenterY - speedLayout.value.measuredHeight / 2
                    }
                    speedLayout.root.requestLayout()
                }
            }
            if (value != 0L) {
                speedLayout.root.visibility = View.VISIBLE
                updateLoopRelatedData()
            }
        } else {
            speedLayout.units.text = ""
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
                strength.visibility = View.VISIBLE
                strength.setSignalData(signalStrengthInfo.value ?: 0, signalStrengthInfo.min, signalStrengthInfo.max)
            }
        } else {
            with(curveBinding.layoutStrength) {
                strength.visibility = View.INVISIBLE
                strength.setSignalData(-140, -140, -60)
                strength.requestLayout()
            }
        }
    }

    fun setMeasurementState(state: MeasurementState) {
        // Leaving a speed phase: stop the arc ticker so it can't keep running against a stale target.
        if (state != MeasurementState.DOWNLOAD && state != MeasurementState.UPLOAD) {
            stopBottomTicker()
        }
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
            setBottomSpeedValue(0)
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

    override fun onDetachedFromWindow() {
        stopBottomTicker()
        super.onDetachedFromWindow()
    }

    companion object {
        // Per-frame easing factor for the arc chasing the latest value: at ~60 fps the arc reaches a
        // new target in ~150-200 ms, smooth without lagging noticeably behind the ~100 ms samples.
        private const val BOTTOM_PROGRESS_SMOOTHING = 0.25f

        private const val LEFT_MARGIN_DIVIDER = 2
        private const val TOP_MARGIN_DIVIDER = 8

        private const val VALUE_SIZE_DIVIDER = 10
        private const val UNITS_SIZE_DIVIDER = 25
    }
}