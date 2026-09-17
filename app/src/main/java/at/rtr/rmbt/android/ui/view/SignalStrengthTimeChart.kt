package at.rtr.rmbt.android.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.res.ResourcesCompat
import at.rtr.rmbt.android.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A line chart of the combined mobile signal strength (dBm) over time, used by the signal (coverage)
 * measurement view.
 *
 * The plotted value is the same combined signal recorded for the fences (for 5G NSA the weaker of the
 * LTE anchor and the NR secondary).
 *
 * Two modes:
 *  - **Live (follow)** - the default. Fed via [setSamples]/[addSample] from the recorded buffer. The
 *    window starts at [MIN_WINDOW_MILLIS] (10 s) wide, grows with elapsed time up to
 *    [MAX_WINDOW_MILLIS] (5 min), then slides to always show the most recent 5 min.
 *  - **Browse** - entered by dragging (pan) or pinching (zoom). The visible window is user-controlled
 *    and data is loaded on demand via [onWindowRequested] (from the persisted session history), so any
 *    period of the session can be inspected. Panning back to "now" (the right edge reaching the current
 *    time) snaps back to Live, where the chart resumes auto-scrolling with new samples.
 *
 * Each segment is drawn in the technology colour of its sample; a null-signal sample is a gap.
 */
class SignalStrengthTimeChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Sample(val timeMillis: Long, val signalDbm: Int?, val color: Int)

    /** One sample for [setSamples]/[setBrowseData]: time, combined signal (dBm, null = gap) and colour. */
    data class ChartSample(val timeMillis: Long, val signalDbm: Int?, val color: Int)

    private val samples = ArrayDeque<Sample>()
    // Timestamps at which the serving cell changed; drawn as small grey X markers on the time axis.
    private val cellChangeMarkers = ArrayDeque<Long>()
    private var startTimeMillis: Long = -1L

    // --- Browse (scroll/zoom) state -------------------------------------------------------------
    // While true the chart follows live data (auto-slide, last 5 min); false while the user browses.
    private var followLive = true
    // The user-controlled window when browsing: [browseEndTime - browseDurationMillis, browseEndTime].
    private var browseEndTime = 0L
    private var browseDurationMillis = MAX_WINDOW_MILLIS
    // Earliest persisted sample time, used to clamp how far back the user can pan/zoom. Null = unknown.
    private var minTimeMillis: Long? = null

    // Geometry of the last draw, so the gesture handlers can convert pixels <-> time.
    private var lastChartLeft = 0f
    private var lastChartWidth = 0f
    private var lastWindowMillis = MAX_WINDOW_MILLIS
    private var lastLeftTime = 0L

    /** Called when the browse window changes; the host should load persisted samples for [from, to]. */
    var onWindowRequested: ((fromMillis: Long, toMillis: Long) -> Unit)? = null
    /** Called when the chart snaps back to live (window right edge reached "now"); host resumes live feed. */
    var onReturnedToLive: (() -> Unit)? = null

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

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (lastChartWidth <= 0f) return false
            enterBrowseIfLive()
            // distanceX is (previous - current) focus x: dragging the finger right (revealing earlier
            // data) is negative, so this moves browseEndTime back in time. Natural "drag to scrub".
            browseEndTime += (distanceX / lastChartWidth * browseDurationMillis).toLong()
            clampBrowseWindow()
            requestBrowseData()
            invalidate()
            return true
        }
    })

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (lastChartWidth <= 0f) return false
            enterBrowseIfLive()
            val leftTime = browseEndTime - browseDurationMillis
            // Time under the pinch focus, kept fixed while the duration changes (zoom around the finger).
            val focalFraction = ((detector.focusX - lastChartLeft) / lastChartWidth).coerceIn(0f, 1f)
            val focalTime = leftTime + (focalFraction * browseDurationMillis).toLong()
            val newDuration = (browseDurationMillis / detector.scaleFactor).toLong()
                .coerceIn(MIN_WINDOW_MILLIS, maxBrowseDuration())
            browseDurationMillis = newDuration
            browseEndTime = focalTime + ((1f - focalFraction) * newDuration).toLong()
            clampBrowseWindow()
            requestBrowseData()
            invalidate()
            return true
        }
    })

    // Redraws while following live so the window keeps sliding even between samples.
    private val tickRunnable = object : Runnable {
        override fun run() {
            if (followLive && samples.isNotEmpty()) {
                invalidate()
                postDelayed(this, TICK_INTERVAL_MILLIS)
            }
        }
    }

    // Debounced browse-data request so a fast drag/pinch doesn't fire a query per pixel.
    private val browseRequestRunnable = Runnable {
        if (!followLive) {
            val leftTime = browseEndTime - browseDurationMillis
            // Widen slightly on the left so the segment entering the window from off-screen is drawn.
            val margin = browseDurationMillis / 10
            onWindowRequested?.invoke(leftTime - margin, browseEndTime)
        }
    }

    /** True while the chart is following live data (so the host should keep feeding [setSamples]). */
    fun isFollowingLive(): Boolean = followLive

    /** Sets the earliest known sample time of the session, to clamp how far back browsing can go. */
    fun setSessionMinTime(minMillis: Long) {
        minTimeMillis = minMillis
    }

    /**
     * Appends a new signal sample at the current time (live mode only). [signalDbm] may be null (gap).
     */
    fun addSample(signalDbm: Int?, technologyColor: Int) {
        if (!followLive) return
        val now = System.currentTimeMillis()
        if (startTimeMillis < 0) startTimeMillis = now
        samples.addLast(Sample(now, signalDbm, technologyColor))
        pruneOldSamples(now)
        invalidate()
        removeCallbacks(tickRunnable)
        postDelayed(tickRunnable, TICK_INTERVAL_MILLIS)
    }

    /**
     * Marks the current time on the chart with a small grey X (serving-cell change). Live overlay only.
     */
    fun addCellChangeMarker() {
        val now = System.currentTimeMillis()
        if (startTimeMillis < 0) startTimeMillis = now
        cellChangeMarkers.addLast(now)
        pruneOldMarkers(now)
        invalidate()
    }

    /**
     * Replaces the whole series at once for the LIVE view (from the recorded buffer). This is what
     * fills the gap after the screen was off. Ignored while browsing so it doesn't clobber the user's
     * scrolled/zoomed window.
     */
    fun setSamples(newSamples: List<ChartSample>) {
        if (!followLive) return
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

    /**
     * Provides the samples for the current BROWSE window (loaded from the persisted history). Ignored
     * once the chart has snapped back to live, so a late query result can't overwrite the live view.
     */
    fun setBrowseData(newSamples: List<ChartSample>) {
        if (followLive) return
        samples.clear()
        for (s in newSamples) {
            samples.addLast(Sample(s.timeMillis, s.signalDbm, s.color))
        }
        invalidate()
    }

    fun reset() {
        samples.clear()
        cellChangeMarkers.clear()
        startTimeMillis = -1L
        followLive = true
        minTimeMillis = null
        removeCallbacks(tickRunnable)
        removeCallbacks(browseRequestRunnable)
        invalidate()
    }

    private fun enterBrowseIfLive() {
        if (followLive) {
            browseDurationMillis = lastWindowMillis.coerceIn(MIN_WINDOW_MILLIS, maxBrowseDuration())
            browseEndTime = System.currentTimeMillis()
            followLive = false
            removeCallbacks(tickRunnable)
        }
    }

    private fun returnToLive() {
        if (!followLive) {
            followLive = true
            removeCallbacks(browseRequestRunnable)
            onReturnedToLive?.invoke()
            removeCallbacks(tickRunnable)
            postDelayed(tickRunnable, TICK_INTERVAL_MILLIS)
        }
    }

    private fun maxBrowseDuration(): Long {
        val min = minTimeMillis
        val span = if (min != null) System.currentTimeMillis() - min else MAX_WINDOW_MILLIS
        return span.coerceAtLeast(MAX_WINDOW_MILLIS)
    }

    private fun clampBrowseWindow() {
        val now = System.currentTimeMillis()
        browseDurationMillis = browseDurationMillis.coerceIn(MIN_WINDOW_MILLIS, maxBrowseDuration())
        val minEnd = minTimeMillis ?: (now - maxBrowseDuration())
        browseEndTime = browseEndTime.coerceIn(minEnd, now)
        // Reaching (near) "now" on the right edge snaps back to the live, auto-scrolling view.
        if (now - browseEndTime <= LIVE_SNAP_THRESHOLD_MILLIS) {
            returnToLive()
        }
    }

    private fun requestBrowseData() {
        removeCallbacks(browseRequestRunnable)
        postDelayed(browseRequestRunnable, BROWSE_REQUEST_DEBOUNCE_MILLIS)
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
        removeCallbacks(browseRequestRunnable)
        super.onDetachedFromWindow()
    }

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            // Keep our parent (if any is scrollable) from stealing the horizontal drag.
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        var handled = scaleDetector.onTouchEvent(event)
        handled = gestureDetector.onTouchEvent(event) || handled
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            parent?.requestDisallowInterceptTouchEvent(false)
        }
        return handled || super.onTouchEvent(event)
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

        // Nothing to place a time axis against yet in live mode.
        if (samples.isEmpty() && followLive) return

        val now = System.currentTimeMillis()
        val windowMillis: Long
        val leftTime: Long
        if (followLive) {
            val elapsed = now - startTimeMillis
            windowMillis = elapsed.coerceIn(MIN_WINDOW_MILLIS, MAX_WINDOW_MILLIS)
            leftTime = if (elapsed <= MAX_WINDOW_MILLIS) startTimeMillis else now - MAX_WINDOW_MILLIS
        } else {
            windowMillis = browseDurationMillis.coerceAtLeast(MIN_WINDOW_MILLIS)
            leftTime = browseEndTime - windowMillis
        }
        // Cache geometry so the gesture handlers can convert between pixels and time.
        lastChartLeft = chartLeft
        lastChartWidth = chartWidth
        lastWindowMillis = windowMillis
        lastLeftTime = leftTime

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
        // Right edge within this of "now" counts as "at the live edge" -> snap back to live.
        private const val LIVE_SNAP_THRESHOLD_MILLIS = 1_500L
        private const val BROWSE_REQUEST_DEBOUNCE_MILLIS = 120L
    }
}
