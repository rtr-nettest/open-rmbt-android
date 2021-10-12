package at.rtr.rmbt.android.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.LayoutMeasurementProgressSquareBinding
import at.rtr.rmbt.android.util.format
import at.specure.measurement.MeasurementState

class MeasurementProgressSquareView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val inflater = LayoutInflater.from(context)
    private val binding = LayoutMeasurementProgressSquareBinding.inflate(inflater, this, false)
        .also { addView(it.root) }
        .also {
            it.background.setBackgroundResource(R.drawable.bg_progress_square_empty)
            val color = ContextCompat.getColor(context, R.color.dark)
            it.progressUnits.setTextColor(color)
            it.progressPhase.setTextColor(color)
            it.progressText.setTextColor(color)
        }

    private val phaseNames = mapOf(
        MeasurementState.INIT to context.getString(R.string.text_phase_init),
        MeasurementState.PING to context.getString(R.string.text_phase_ping),
        MeasurementState.DOWNLOAD to context.getString(R.string.text_phase_download),
        MeasurementState.UPLOAD to context.getString(R.string.text_phase_upload),
        MeasurementState.JITTER_AND_PACKET_LOSS to context.getString(R.string.test_bottom_test_status_jitter),
        MeasurementState.QOS to context.getString(R.string.text_phase_qos)
    )

    private val unitsNames = mapOf(
        MeasurementState.PING to context.getString(R.string.text_unit_ping),
        MeasurementState.DOWNLOAD to context.getString(R.string.text_unit_mbps),
        MeasurementState.UPLOAD to context.getString(R.string.text_unit_mbps),
        MeasurementState.JITTER_AND_PACKET_LOSS to context.getString(R.string.text_unit_ping),
        MeasurementState.QOS to context.getString(R.string.unit_percents)
    )

    fun setMeasurementState(state: MeasurementState) {
        binding.root.visibility =
            if (state != MeasurementState.IDLE && state != MeasurementState.FINISH) VISIBLE else GONE
        binding.spinner.visibility =
            if (state == MeasurementState.INIT) VISIBLE else GONE
        binding.progressText.visibility =
            if (state != MeasurementState.INIT) VISIBLE else GONE
        binding.progressUnits.visibility =
            if (state != MeasurementState.INIT) VISIBLE else GONE

        binding.progressPhase.text = phaseNames[state]
        binding.progressUnits.text = unitsNames[state]
        setProgress(-1)
        setSpeed(-1f)
    }

    fun setProgress(percents: Int) {
        if (percents == 100) {
            binding.spinner.visibility = GONE
            binding.background.setBackgroundResource(R.drawable.bg_progress_square)
            setTextColor(android.R.color.white)
        } else if (percents >= 0) {
            binding.background.setBackgroundResource(R.drawable.bg_progress_square_empty)
            setTextColor(R.color.dark)
        }

        binding.root.requestLayout()
    }

    fun setSpeed(speed: Float) {
        binding.progressText.text = if (speed < 0f) "-" else speed.format()
        binding.progressText.requestLayout()
    }

    private fun setTextColor(@ColorRes resId: Int) {
        val color = ContextCompat.getColor(context, resId)
        binding.progressUnits.setTextColor(color)
        binding.progressPhase.setTextColor(color)
        binding.progressText.setTextColor(color)
    }
}