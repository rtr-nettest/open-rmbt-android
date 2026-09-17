package at.rtr.rmbt.android.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import at.rtr.rmbt.android.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A rolling line chart of the combined mobile signal strength (dBm) over time, used by the signal
 * (coverage) measurement expert view.
 *
 * The plotted value is the same combined signal recorded for the fences (for 5G NSA the weaker of the
 * LTE anchor and the NR secondary), fed via [addSample].
 *
 * Time window behaviour (as requested):
 *  - starts at [MIN_WINDOW_MILLIS] (10 s) wide,
 *  - grows with the elapsed measurement time up to [MAX_WINDOW_MILLIS] (5 min) - the existing data
 *    visibly compresses as the window widens,
 *  - once 5 min have elapsed the window stays 5 min wide and slides so it always shows the most
 *    recent 5 min.
 *
 * Each segment is drawn in the technology colour of its sample, so a technology change shows up as a
 * colour change in the line. A sample with no signal produces a gap.
 */
class SignalStrengthTimeChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Sample(val timeMillis: Long, val signalDbm: Int?, val color: Int)

    /** One sample for [setSamples]: time, combined signal (dBm, null = gap) and technology colour. */
    data class ChartSample(val timeMillis: Long, val signalDbm: Int?, val color: Int)

    private val samples = ArrayDeque<Sample>()
    // Timestamps at which the serving cell changed; drawn as small grey X markers on the time axis.
    private val cellChangeMarkers = ArrayDeque<Long>()
    private var startTimeMillis: Long = -1L

    private val density = resources.displayMetrics.density

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = LINE_STROKE_WIDTH_DP * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = context.getColor(R.color.chart_grid_line_color)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = context.resources.getDimension(R.dimen.chart_label_text_size)
        color = context.getColor(R.color.text_dark_gray)
        typeface = ResourcesCompat.getFont(context, R.font.roboto_regular)
    }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        strokeCap = Paint.Cap.ROUND
        color = Color.GRAY
    }

    private val clockFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // Preallocated and reused inside onDraw to avoid per-frame allocations (DrawAllocation lint).
    private val reusableDate = Date()
    private val maxDbmLabel = SIGNAL_MAX_DBM.toString()
    private val minDbmLabel = SIGNAL_MIN_DBM.toString()

    // Redraws while data is present so the window keeps sliding even between samples.
    private val tickRunnable = object : Runnable {
        override fun run() {
            if (samples.isNotEmpty()) {
                invalidate()
                postDelayed(this, TICK_INTERVAL_MILLIS)
            }
        }
    }

    /**
     * Appends a new signal sample at the current time. [signalDbm] may be null (no signal - a gap is
     * drawn); [technologyColor] is the current technology's colour used to draw the segment ending at
     * this sample.
     */
    fun addSample(signalDbm: Int?, technologyColor: Int) {
        val now = System.currentTimeMillis()
        if (startTimeMillis < 0) startTimeMillis = now
        samples.addLast(Sample(now, signalDbm, technologyColor))
        pruneOldSamples(now)
        invalidate()
        removeCallbacks(tickRunnable)
        postDelayed(tickRunnable, TICK_INTERVAL_MILLIS)
    }

    /**
     * Marks the current time on the chart with a small grey X, to indicate that the serving cell
     * changed. Kept in sync with the same 5-minute window as the samples.
     */
    fun addCellChangeMarker() {
        val now = System.currentTimeMillis()
        if (startTimeMillis < 0) startTimeMillis = now
        cellChangeMarkers.addLast(now)
        pruneOldMarkers(now)
        invalidate()
    }

    /**
     * Replaces the whole series at once (used when the chart is driven by the recorded signal buffer
     * rather than appended live sample-by-sample). This is what fills the gap after the screen was
     * off: the buffer kept growing while the Activity's live feed was paused, so re-loading it here
     * redraws the missing period instead of a straight line.
     *
     * Cell-change markers are left untouched (they are added separately by the caller).
     */
    fun setSamples(newSamples: List<ChartSample>) {
        samples.clear()
        for (s in newSamples) {
            samples.addLast(Sample(s.timeMillis, s.signalDbm, s.color))
        }
        startTimeMillis = samples.firstOrNull()?.timeMillis ?: -1L
        invalidate()
        if (samples.isNotEmpty()) {
            removeCallbacks(tickRunnable)
            postDelayed(tickRunnable, TICK_INTERVAL_MILLIS)
        }
    }

    fun reset() {
        samples.clear()
        cellChangeMarkers.clear()
        startTimeMillis = -1L
        removeCallbacks(tickRunnable)
        invalidate()
    }

    private fun pruneOldSamples(now: Long) {
        val cutoff = now - MAX_WINDOW_MILLIS
        // Keep one sample past the cutoff so the segment entering the visible window is still drawn.
        while (samples.size > 2 && samples[1].timeMillis < cutoff) {
            samples.removeFirst()
        }
        pruneOldMarkers(now)
    }

    private fun pruneOldMarkers(now: Long) {
        val cutoff = now - MAX_WINDOW_MILLIS
        while (cellChangeMarkers.isNotEmpty() && cellChangeMarkers.first() < cutoff) {
            cellChangeMarkers.removeFirst()
        }
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(tickRunnable)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // The unit ("dBm") is stacked under the number, so only the number width is reserved.
        val yLabelWidth = labelPaint.measureText("-124") + 6f * density
        val xLabelHeight = labelPaint.textSize + 6f * density

        val chartLeft = paddingLeft.toFloat()
        val chartRight = width - paddingRight - yLabelWidth
        val chartTop = paddingTop.toFloat() + labelPaint.textSize / 2f
        val chartBottom = height - paddingBottom - xLabelHeight
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop
        if (chartWidth <= 0f || chartHeight <= 0f) return

        // Grid.
        for (i in 0..GRID_ROWS) {
            val y = chartTop + chartHeight * i / GRID_ROWS
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
        }

        // Signal scale (dBm): strongest at the top, weakest at the bottom of the axis. The unit is
        // stacked under the number to save horizontal space.
        labelPaint.textAlign = Paint.Align.LEFT
        val dbmLabelX = chartRight + 4f * density
        canvas.drawText(maxDbmLabel, dbmLabelX, chartTop + labelPaint.textSize / 3f, labelPaint)
        canvas.drawText(SIGNAL_UNIT, dbmLabelX, chartTop + labelPaint.textSize / 3f + labelPaint.textSize, labelPaint)
        canvas.drawText(minDbmLabel, dbmLabelX, chartBottom + labelPaint.textSize / 3f, labelPaint)
        canvas.drawText(SIGNAL_UNIT, dbmLabelX, chartBottom + labelPaint.textSize / 3f + labelPaint.textSize, labelPaint)

        if (samples.isEmpty()) return

        val now = System.currentTimeMillis()
        val elapsed = now - startTimeMillis
        val windowMillis = elapsed.coerceIn(MIN_WINDOW_MILLIS, MAX_WINDOW_MILLIS)
        val leftTime = if (elapsed <= MAX_WINDOW_MILLIS) startTimeMillis else now - MAX_WINDOW_MILLIS

        fun xFor(t: Long): Float = chartLeft + chartWidth * (t - leftTime).toFloat() / windowMillis.toFloat()
        fun yFor(dbm: Int): Float {
            val clamped = dbm.coerceIn(SIGNAL_MIN_DBM, SIGNAL_MAX_DBM)
            val frac = (clamped - SIGNAL_MIN_DBM).toFloat() / (SIGNAL_MAX_DBM - SIGNAL_MIN_DBM).toFloat()
            return chartBottom - chartHeight * frac
        }

        var prev: Sample? = null
        for (cur in samples) {
            val prevSignal = prev?.signalDbm
            val curSignal = cur.signalDbm
            if (prev != null && prevSignal != null && curSignal != null) {
                linePaint.color = cur.color
                canvas.drawLine(
                    xFor(prev.timeMillis), yFor(prevSignal),
                    xFor(cur.timeMillis), yFor(curSignal), linePaint
                )
            } else if (curSignal != null && (prev == null || prevSignal == null)) {
                // Isolated point (start, or first sample after a gap): draw a dot so it is visible.
                linePaint.color = cur.color
                canvas.drawCircle(xFor(cur.timeMillis), yFor(curSignal), linePaint.strokeWidth / 2f, linePaint)
            }
            prev = cur
        }

        // Cell-change markers: a small grey X near the top of the plot at each change time.
        val markerHalf = 4f * density
        val markerY = chartTop + markerHalf + 1f * density
        for (t in cellChangeMarkers) {
            val x = xFor(t)
            if (x < chartLeft || x > chartRight) continue
            canvas.drawLine(x - markerHalf, markerY - markerHalf, x + markerHalf, markerY + markerHalf, markerPaint)
            canvas.drawLine(x - markerHalf, markerY + markerHalf, x + markerHalf, markerY - markerHalf, markerPaint)
        }

        // Time scale: clock time at the left and right edges of the visible window.
        val rightEdgeTime = leftTime + windowMillis
        val baseline = height.toFloat() - 2f * density
        labelPaint.textAlign = Paint.Align.LEFT
        reusableDate.time = leftTime
        canvas.drawText(clockFormat.format(reusableDate), chartLeft, baseline, labelPaint)
        labelPaint.textAlign = Paint.Align.RIGHT
        reusableDate.time = rightEdgeTime
        canvas.drawText(clockFormat.format(reusableDate), chartRight, baseline, labelPaint)
    }

    companion object {
        private const val MIN_WINDOW_MILLIS = 10_000L
        private const val MAX_WINDOW_MILLIS = 5 * 60_000L
        private const val TICK_INTERVAL_MILLIS = 1_000L
        private const val LINE_STROKE_WIDTH_DP = 2f
        private const val GRID_ROWS = 4
        private const val SIGNAL_MIN_DBM = -125
        private const val SIGNAL_MAX_DBM = -65
        private const val SIGNAL_UNIT = "dBm"
    }
}
