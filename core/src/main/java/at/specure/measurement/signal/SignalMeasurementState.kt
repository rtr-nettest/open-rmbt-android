package at.specure.measurement.signal

enum class SignalMeasurementState(val intValue: Int) {

    SUCCESS(0),
    RUNNING(1),
    ERROR(2);

    companion object {

        fun fromValue(value: Int): SignalMeasurementState {
            values().forEach {
                if (it.intValue == value) return it
            }

            throw IllegalArgumentException("signal measurement with value $value not found")
        }
    }
}