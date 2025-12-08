package at.specure.data

import android.content.Context
import timber.log.Timber
import javax.inject.Inject
import androidx.core.content.edit
import javax.inject.Singleton

private const val KEY_SIGNAL_MEASUREMENT_RUNNING = "KEY_SIGNAL_MEASUREMENT_RUNNING"
private const val KEY_SIGNAL_MEASUREMENT_CONTINUE_LAST_SESSION = "KEY_SIGNAL_MEASUREMENT_CONTINUE_LAST_SESSION"
private const val KEY_SIGNAL_MEASUREMENT_LAST_MEASUREMENT_ID = "KEY_SIGNAL_MEASUREMENT_LAST_SESSION_ID"

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

    fun onStopMeasurementSession() {
        signalMeasurementLastMeasurementId = null
        signalMeasurementShouldContinueInLastSession = false
        signalMeasurementIsRunning = false
    }

    fun onStartMeasurementSession(lastMeasurementId: String) {
        signalMeasurementLastMeasurementId = lastMeasurementId
        signalMeasurementShouldContinueInLastSession = true
        signalMeasurementIsRunning = true
    }
}