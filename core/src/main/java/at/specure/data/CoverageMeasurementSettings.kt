package at.specure.data

import android.content.Context
import timber.log.Timber
import javax.inject.Inject
import androidx.core.content.edit
import javax.inject.Singleton

private const val KEY_SIGNAL_MEASUREMENT_RUNNING = "KEY_SIGNAL_MEASUREMENT_RUNNING"
private const val KEY_SIGNAL_MEASUREMENT_CONTINUE_LAST_SESSION = "KEY_SIGNAL_MEASUREMENT_CONTINUE_LAST_SESSION"
private const val KEY_SIGNAL_MEASUREMENT_LAST_MEASUREMENT_ID = "KEY_SIGNAL_MEASUREMENT_LAST_SESSION_ID"
private const val KEY_SIGNAL_MEASUREMENT_LAST_MEASUREMENT_LOOP_ID = "KEY_SIGNAL_MEASUREMENT_LAST_MEASUREMENT_LOOP_ID"
private const val KEY_HAS_UNSYNCED_COVERAGE = "KEY_HAS_UNSYNCED_COVERAGE"
private const val KEY_PROTECTED_COVERAGE_LOOP_ID = "KEY_PROTECTED_COVERAGE_LOOP_ID"

@Singleton
class CoverageMeasurementSettings @Inject constructor(context: Context) {

    private val preferences = context.getSharedPreferences("signal_measurement_settings.pref",
        Context.MODE_PRIVATE
    )

    var signalMeasurementIsRunning: Boolean
        get() {
            val isRunning = preferences.getBoolean(KEY_SIGNAL_MEASUREMENT_RUNNING, false)
            Timber.d("Signal measurement is running: $isRunning")
            return isRunning
        }
        set(value) {
            Timber.d("Signal measurement is running set to: $value")
            preferences.edit { putBoolean(KEY_SIGNAL_MEASUREMENT_RUNNING, value) }
        }

    var signalMeasurementShouldContinueInLastSession: Boolean
        get() {
            val shouldContinue = preferences.getBoolean(KEY_SIGNAL_MEASUREMENT_CONTINUE_LAST_SESSION, false)
            Timber.d("Signal measurement should continue in last session: $shouldContinue")
            return shouldContinue
        }
        set(value) {
            Timber.d("Signal measurement should continue in last session set to: $value")
            preferences.edit { putBoolean(KEY_SIGNAL_MEASUREMENT_CONTINUE_LAST_SESSION, value) }
        }

    var signalMeasurementLastMeasurementId: String?
        get() {
            val sessionId = preferences.getString(KEY_SIGNAL_MEASUREMENT_LAST_MEASUREMENT_ID, null)
            Timber.d("Signal measurement last session ID $sessionId")
            return sessionId
        }
        set(value) {
            Timber.d("Signal measurement last session ID set to: $value")
            preferences.edit { putString(KEY_SIGNAL_MEASUREMENT_LAST_MEASUREMENT_ID, value) }
        }

    var signalMeasurementLastMeasurementLoopId: String?
        get() {
            val sessionId = preferences.getString(KEY_SIGNAL_MEASUREMENT_LAST_MEASUREMENT_LOOP_ID, null)
            Timber.d("Signal measurement last session loop ID $sessionId")
            return sessionId
        }
        set(value) {
            Timber.d("Signal measurement last session loop ID set to: $value")
            preferences.edit { putString(KEY_SIGNAL_MEASUREMENT_LAST_MEASUREMENT_LOOP_ID, value) }
        }

    var hasUnsyncedCoverage: Boolean
        get() = preferences.getBoolean(KEY_HAS_UNSYNCED_COVERAGE, false)
        set(value) {
            Timber.d("Has unsynced coverage set to: $value")
            preferences.edit { putBoolean(KEY_HAS_UNSYNCED_COVERAGE, value) }
        }

    /**
     * Loop id of the coverage measurement that is currently running OR whose result is still being
     * displayed to the user. Sessions belonging to this loop must never be purged from the database:
     * a coverage loop submits several segments (sessions/fences) while running, and the already
     * submitted ones are needed to draw the full measurement map. It is set when a measurement
     * session starts (see [onStartMeasurementSession]) and only cleared once the user has left the
     * result page (see [clearProtectedCoverageLoopId]); a purge deletes only historic loops.
     */
    var protectedCoverageLoopId: String?
        get() = preferences.getString(KEY_PROTECTED_COVERAGE_LOOP_ID, null)
        set(value) {
            Timber.d("Protected coverage loop ID set to: $value")
            preferences.edit { putString(KEY_PROTECTED_COVERAGE_LOOP_ID, value) }
        }

    val baseMinimalDistanceBetweenFenceCentersMeters = 10

    fun onStopMeasurementSession() {
        signalMeasurementLastMeasurementId = null
        signalMeasurementLastMeasurementLoopId = null
        signalMeasurementShouldContinueInLastSession = false
        signalMeasurementIsRunning = false
    }

    fun onStartMeasurementSession(lastMeasurementId: String, lastMeasurementLoopId: String) {
        signalMeasurementLastMeasurementId = lastMeasurementId
        signalMeasurementLastMeasurementLoopId = lastMeasurementLoopId
        // Protect the whole loop's data from the purge for as long as it is being measured or shown.
        // Every session in the loop shares the same loop id, so this stays stable across the loop and
        // is only replaced once the next loop starts.
        protectedCoverageLoopId = lastMeasurementLoopId
        signalMeasurementShouldContinueInLastSession = true
        signalMeasurementIsRunning = true
    }

    /**
     * Releases the current loop for purging. Call this once the user has left the result page of the
     * finished measurement - from that point on the loop is historic and may be deleted.
     */
    fun clearProtectedCoverageLoopId() {
        protectedCoverageLoopId = null
    }
}