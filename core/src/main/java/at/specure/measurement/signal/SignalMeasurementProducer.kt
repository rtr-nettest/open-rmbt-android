package at.specure.measurement.signal

import androidx.lifecycle.LiveData
import at.specure.test.SignalMeasurementType

interface SignalMeasurementProducer {

    val isActive: Boolean
    val isPaused: Boolean
    val activeStateLiveData: LiveData<Boolean>
    val pausedStateLiveData: LiveData<Boolean>
    val signalMeasurementSessionIdLiveData: LiveData<String?>

    fun setEndAlarm()
    fun startMeasurement(unstoppable: Boolean, signalMeasurementType: SignalMeasurementType)
    fun stopMeasurement(unstoppable: Boolean)
    fun pauseMeasurement(unstoppable: Boolean)
    fun resumeMeasurement(unstoppable: Boolean)
}