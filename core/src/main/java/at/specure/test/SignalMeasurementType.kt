package at.specure.test

import at.specure.result.QoECategory

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
    LOOP_WAITING("loop_waiting");

    companion object {
        fun fromString(type: String): QoECategory {
            QoECategory.values().forEach { x ->
                if (x.categoryName == type) {
                    return x
                }
            }
            return QoECategory.QOE_UNKNOWN
        }
    }
}