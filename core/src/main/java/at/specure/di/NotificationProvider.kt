package at.specure.di

import android.app.Notification
import at.specure.measurement.MeasurementState

interface NotificationProvider {

    /**
     * Returns notification object for foreground measurement service [at.specure.measurement.MeasurementService]
     * [progress] - intermediate progress of all current state
     * [state] - current test state
     * [skipQoSTests] - is QoS test skipped or not
     */
    fun measurementServiceNotification(progress: Int, state: MeasurementState, skipQoSTests: Boolean): Notification
}