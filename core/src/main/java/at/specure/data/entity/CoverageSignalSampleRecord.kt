package at.specure.data.entity

import androidx.room.Entity
import at.specure.data.Tables
import at.specure.info.network.MobileNetworkType

/**
 * One point of the combined signal-over-time series recorded during a coverage (signal) measurement.
 *
 * Persisted so the measurement chart can be scrolled/zoomed over the whole session (not just the last
 * 5 minutes held in memory) and so the trace recorded while the screen was off is available. The
 * value is the already-combined signal (for 5G NSA the weaker of the LTE anchor and NR secondary),
 * exactly what the chart draws, so no per-cell recombination is needed at query time.
 */
@Entity(
    tableName = Tables.COVERAGE_SIGNAL_SAMPLE,
    primaryKeys = ["sessionId", "timeMillis"]
)
data class CoverageSignalSampleRecord(
    /** Local id of the coverage measurement session this sample belongs to. */
    val sessionId: String,
    /** Wall-clock time of the sample (epoch millis). */
    val timeMillis: Long,
    /** Combined signal in dBm; null means "no signal" (a gap in the chart). */
    val signalDbm: Int?,
    /** Technology at the time of the sample, used for the line colour. */
    val networkType: MobileNetworkType
)
