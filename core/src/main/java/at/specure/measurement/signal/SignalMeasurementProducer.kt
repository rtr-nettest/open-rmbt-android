package at.specure.measurement.signal

import androidx.lifecycle.LiveData

interface SignalMeasurementProducer {

    val isActive: Boolean
    val isPaused: Boolean
    val activeStateLiveData: LiveData<Boolean>
    val pausedStateLiveData: LiveData<Boolean>

    fun setEndAlarm()
    fun startMeasurement(unstoppable: Boolean)
    fun stopMeasurement(unstoppable: Boolean)
    fun pauseMeasurement(unstoppable: Boolean)
    fun resumeMeasurement(unstoppable: Boolean)
}