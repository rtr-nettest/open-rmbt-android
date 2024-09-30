package at.specure.data

import android.content.Context
import timber.log.Timber
import javax.inject.Inject

private const val KEY_SIGNAL_MEASUREMENT_RUNNING = "KEY_SIGNAL_MEASUREMENT_RUNNING"

class SignalMeasurementSettings @Inject constructor(context: Context) {

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
            preferences.edit().putBoolean(KEY_SIGNAL_MEASUREMENT_RUNNING, value).apply()
        }
}