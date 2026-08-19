package at.rtr.rmbt.android.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import at.specure.util.map.OFFLINE_GRAY
import kotlin.math.roundToInt

/**
 * Draws one vertical rounded bar per dB of the current signal range [minSignal]..[maxSignal].
 * Bar height grows from the leftmost (weakest) to the rightmost (strongest) bar. Bar color is
 * interpolated between [minColor] (weakest, grey as on the fences map) and [maxColor] (strongest,
 * current technology color). Bars stronger than the current [signalValue] are shown in white.
 */
class SignalLevelBarsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val minBarHeightFraction = 0.3f
    private val barSpacing = context.resources.displayMetrics.density * 1f
    private val cornerRadius = context.resources.displayMetrics.density * 1f
    private val borderWidth = context.resources.displayMetrics.density * 1f

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
    }
    private val rect = RectF()

    // Default to the common mobile (LTE/NR) range so the full set of segments is already shown at
    // startup, before the first network/signal sample arrives. setRange() keeps this default when it
    // is handed an unknown range (e.g. NetworkTypeCompat.TYPE_UNKNOWN), so the number of segments
    // does not visibly change once measurement begins.
    private var minSignal = -130
    private var maxSignal = -70

    var minColor: Int = OFFLINE_GRAY.toColorInt()
    var maxColor: Int = OFFLINE_GRAY.toColorInt()

    var signalValue: Int? = null
        set(value) {
            field = value
            invalidate()
        }

    fun setRange(min: Int, max: Int) {
        // Ignore an unknown/degenerate range (e.g. before any network is available, when both bounds
        // are Int.MIN_VALUE): keep the sensible default so the segment count stays stable instead of
        // collapsing to the minimum of two bars.
        if (max <= min) return
        minSignal = min
        maxSignal = max
        invalidate()
    }

    private fun barCount(): Int = (maxSignal - minSignal + 1).coerceAtLeast(2)

    private fun currentBarIndex(barCount: Int): Int? {
        val value = signalValue ?: return null
        if (maxSignal <= minSignal) return null
        val clamped = value.coerceIn(minSignal, maxSignal)
        val t = (clamped - minSignal).toFloat() / (maxSignal - minSignal)
        return (t * (barCount - 1)).roundToInt()
    }

    private fun interpolateColor(fraction: Float): Int {
        val r = Color.red(minColor) + fraction * (Color.red(maxColor) - Color.red(minColor))
        val g = Color.green(minColor) + fraction * (Color.green(maxColor) - Color.green(minColor))
        val b = Color.blue(minColor) + fraction * (Color.blue(maxColor) - Color.blue(minColor))
        return Color.rgb(r.roundToInt(), g.roundToInt(), b.roundToInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val barCount = barCount()
        val barWidth = (width - barSpacing * (barCount - 1)) / barCount
        val currentIndex = currentBarIndex(barCount)
        val step = 1f / (barCount - 1)

        for (i in 0 until barCount) {
            // Last bar is drawn half a step shorter so it doesn't stand out from the ramp.
            val effectiveIndex = if (i == barCount - 1) i - 0.5f else i.toFloat()
            val fraction = i * step
            val barHeight = height * (minBarHeightFraction + (1f - minBarHeightFraction) * effectiveIndex * step)
            val left = i * (barWidth + barSpacing)
            val top = height - barHeight

            val gradientColor = interpolateColor(fraction)
            rect.set(left, top, left + barWidth, height.toFloat())

            if (currentIndex != null && i > currentIndex) {
                // Bars stronger than the current signal are unfilled, bordered in the tech gradient color.
                fillPaint.color = Color.WHITE
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint)
                borderPaint.color = gradientColor
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
            } else {
                fillPaint.color = gradientColor
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint)
            }
        }
    }
}
