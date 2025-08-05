package at.rmbt.client.control.data

enum class SignalMeasurementType(val signalTypeName: String) {
    /**
     * Original signal measurement triggered by user from UI
     */
    DEDICATED("dedicated"),

    /**
     * Signal recorded during the regular measurement
     */
    REGULAR("regular"),

    /**
     * Signal recorded during the regular measurement which is part of the loop measurement
     */
    LOOP_ACTIVE("loop_active"),

    /**
     * Signal recorded during the waiting phase between 2 regular measurements executed in loop measurement
     */
    LOOP_WAITING("loop_waiting"),

    /**
     * for unknown values
     */
    UNKNOWN("unknown"), ;

    companion object {
        fun fromString(type: String): SignalMeasurementType {
            values().forEach { x ->
                if (x.signalTypeName == type) {
                    return x
                }
            }
            return UNKNOWN
        }
    }
}