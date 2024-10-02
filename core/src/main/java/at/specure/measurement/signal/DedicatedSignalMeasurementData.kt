package at.specure.measurement.signal

import at.specure.data.SignalMeasurementSettings
import at.specure.data.entity.SignalMeasurementPointRecord
import at.specure.data.entity.SignalMeasurementSession

data class DedicatedSignalMeasurementData(
    val signalMeasurementSession: SignalMeasurementSession,
    val signalMeasurementSettings: SignalMeasurementSettings,
    val points: List<SignalMeasurementPointRecord> = mutableListOf()
)