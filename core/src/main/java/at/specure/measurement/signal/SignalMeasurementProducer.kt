package at.specure.measurement.signal

import androidx.lifecycle.LiveData

interface SignalMeasurementProducer {

    val isActive: Boolean
    val isPaused: Boolean
    val activeStateLiveData: LiveData<Boolean>
    val pausedStateLiveData: LiveData<Boolean>

    fun startMeasurement()
    fun stopMeasurement()
    fun pauseMeasurement()
    fun resumeMeasurement()
}