package at.rtr.rmbt.android.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.BindingAdapter
import at.rtr.rmbt.android.R
import at.specure.measurement.MeasurementState

@SuppressLint("ViewConstructor")
class MeasurementProgressLineView(context: Context, attrs: AttributeSet? = null) :
    AppCompatImageView(context, attrs) {

    private val paintFilled = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
    }
    private val paintEmpty = Paint().apply {
        color = ResourcesCompat.getColor(resources, R.color.measurement_progress_empty, null)
    }

    private var qosEnabled: Boolean = false
    private var filledPercents: Int = 0
    private var phase: MeasurementState = MeasurementState.IDLE

    /**
     * Defines coefficients to calculation progress value for each phase of measurement when QoS is disabled
     */
    private var progressCoefficients = LinkedHashMap<MeasurementState, Float>().apply {
        put(MeasurementState.INIT, 0.2f)
        put(MeasurementState.JITTER_AND_PACKET_LOSS, 0.1f)
        put(MeasurementState.PING, 0.1f)
        put(MeasurementState.DOWNLOAD, 0.3f)
        put(MeasurementState.UPLOAD, 0.3f)
        put(MeasurementState.FINISH, 0f)
    }

    /**
     * Defines coefficients to calculation progress value for each phase of measurement when QoS is enabled
     */
    private var progressCoefficientsQoS = LinkedHashMap<MeasurementState, Float>().apply {
        put(MeasurementState.INIT, 0.1f)
        put(MeasurementState.JITTER_AND_PACKET_LOSS, 0.1f)
        put(MeasurementState.PING, 0.1f)
        put(MeasurementState.DOWNLOAD, 0.2f)
        put(MeasurementState.UPLOAD, 0.24f)
        put(MeasurementState.QOS, 0.25f)
        put(MeasurementState.FINISH, 0f)
    }

    /**
     * Defines offsets according to previous measurement phases when QoS is disabled
     */
    private var progressOffsets = LinkedHashMap<MeasurementState, Float>().apply {
        put(MeasurementState.INIT, 0f)
        put(MeasurementState.JITTER_AND_PACKET_LOSS, 0.2f)
        put(MeasurementState.PING, 0.3f)
        put(MeasurementState.DOWNLOAD, 0.4f)
        put(MeasurementState.UPLOAD, 0.7f)
        put(MeasurementState.FINISH, 1f)
    }

    /**
     * Defines offsets according to previous measurement phases when QoS is enabled
     */
    private var progressOffsetsQoS = LinkedHashMap<MeasurementState, Float>().apply {
        put(MeasurementState.INIT, 0f)
        put(MeasurementState.JITTER_AND_PACKET_LOSS, 0.1f)
        put(MeasurementState.PING, 0.2f)
        put(MeasurementState.DOWNLOAD, 0.3f)
        put(MeasurementState.UPLOAD, 0.5f)
        put(MeasurementState.QOS, 0.75f)
        put(MeasurementState.FINISH, 1f)
    }

    @SuppressLint("DrawAllocation")
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            val lineGradientBitmap = ResourcesCompat.getDrawable(resources, R.drawable.bg_line_chart_gradient_path, null)
                ?.let { convertToBitmap(it, right - left, bottom - top) }
            paintFilled.shader = lineGradientBitmap?.let { BitmapShader(it, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(
            0f,
            0f,
            measuredWidth.toFloat(),
            measuredHeight.toFloat(),
            paintEmpty
        )

        val width = if (qosEnabled) {
            ((progressOffsetsQoS[phase] ?: 0f) +
                    (progressCoefficientsQoS[phase] ?: 0f) * filledPercents / 100f) * measuredWidth
        } else {
            ((progressOffsets[phase] ?: 0f) +
                    (progressCoefficients[phase] ?: 0f) * filledPercents / 100f) * measuredWidth
        }
        canvas.drawRect(0f, 0f, width, measuredHeight.toFloat(), paintFilled)
    }

    private fun convertToBitmap(drawable: Drawable, widthPixels: Int, heightPixels: Int): Bitmap? {
        val mutableBitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mutableBitmap)
        drawable.setBounds(0, 0, widthPixels, heightPixels)
        drawable.draw(canvas)
        return mutableBitmap
    }

    companion object {
        @JvmStatic
        @BindingAdapter("app:percentage")
        fun setPercents(lineView: MeasurementProgressLineView, percents: Int) {
            lineView.filledPercents = percents
            lineView.invalidate()
        }

        @JvmStatic
        @BindingAdapter("app:phase")
        fun setMeasurementPhase(lineView: MeasurementProgressLineView, state: MeasurementState) {
            lineView.phase = state
            lineView.invalidate()
        }

        @JvmStatic
        @BindingAdapter("app:qosEnabled")
        fun setQosEnabled(lineView: MeasurementProgressLineView, qosEnabled: Boolean) {
            lineView.qosEnabled = qosEnabled
            lineView.invalidate()
        }
    }
}