package at.specure.info.network

import android.os.Build
import android.telephony.TelephonyManager

enum class NRConnectionState(val stringValue: String) {

    NSA("NSA"),
    SA("SA"),
    AVAILABLE("AVAILABLE"),
    NOT_AVAILABLE("NOT_AVAILABLE");

    companion object {

        fun fromString(value: String): NRConnectionState {
            values().forEach {
                if (it.stringValue == value) return it
            }
            return NOT_AVAILABLE
        }

        fun getNRConnectionState(telephonyManager: TelephonyManager): NRConnectionState {

            // only for android 9 and up
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                return NOT_AVAILABLE
            }

            try {
                val obj = Class.forName(telephonyManager.javaClass.name)
                    .getDeclaredMethod("getServiceState", *arrayOfNulls(0)).invoke(telephonyManager, *arrayOfNulls(0))
                val methods = Class.forName(obj.javaClass.name).declaredMethods

                // try extracting from string
                // source: https://github.com/mroczis/netmonster-core/blob/master/library/src/main/java/cz/mroczis/netmonster/core/feature/detect/DetectorLteAdvancedNrServiceState.kt#L69
                val serviceState = obj.toString()
                val is5gActive = serviceState.contains("nrState=CONNECTED") ||
                        serviceState.contains("nsaState=5") ||
                        serviceState.contains("EnDc=true") &&
                        serviceState.contains("5G Allocated=true")
                if (is5gActive) {
                    return NSA
                }
                val is5gAvailable = serviceState.contains("isNrAvailable=true") ||
                        serviceState.contains("isNrAvailable = true")
                if (is5gAvailable) {
                    return AVAILABLE
                }
                for (method in methods) {
                    if (method.name == "getNrStatus" || method.name == "getNrState") {
                        method.isAccessible = true
                        if ((method.invoke(obj, *arrayOfNulls(0)) as Int).toInt() == 3) {
                            NSA
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return NOT_AVAILABLE
        }
    }
}