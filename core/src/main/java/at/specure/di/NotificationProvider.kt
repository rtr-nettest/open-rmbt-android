package at.specure.di

import android.app.Notification
import android.content.Intent
import at.specure.data.entity.LoopModeRecord
import at.specure.measurement.MeasurementState

interface NotificationProvider {

    /**
     * Returns notification object for foreground measurement service [at.specure.measurement.MeasurementService]
     * [progress] intermediate progress of all current state
     * [state] current test state
     * [skipQoSTests] is QoS test skipped or not
     * [loopModeRecord] loop mode record
     * [loopModeTestsCount] count of tests that should be performed during the loop measurement
     * [stopMeasurementIntent] - intent that should be called to stop signal measurement
     */
    fun signalMeasurementService(stopMeasurementIntent: Intent): Notification

    fun measurementServiceNotification(
        progress: Int,
        state: MeasurementState,
        skipQoSTests: Boolean,
        loopModeRecord: LoopModeRecord?,
        loopModeTestsCount: Int,
        cancellationIntent: Intent

    ): Notification

    /**
     * Returns notification object for the measurement service in loop mode displaying time & distance left to the next test
     * [timePassedMillis] time already passed since previous test in milliseconds
     * [metersPassed] distance already passed since previous test in meters
     * [metersRequired] distance required pass to start next text in loop in meters
     * [testsPassed] count of tests already passed
     * [testsCount] total tests count in loop
     * [cancellationIntent] intent to cancel loop measurement test
     */
    fun loopCountDownNotification(
        timePassedMillis: Long,
        metersPassed: Int,
        metersRequired: Int,
        testsPassed: Int,
        testsCount: Int,
        cancellationIntent: Intent
    ): Notification

    /**
     * Returns notification to notify that loop mode finished
     */
    fun loopModeFinishedNotification(): Notification
}