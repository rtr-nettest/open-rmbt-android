package at.specure.data

import at.specure.info.TransportType
import at.specure.info.cell.CellTechnology

enum class NetworkTypeCompat(val stringValue: String) {

    TYPE_2G("2G"),
    TYPE_3G("3G"),
    TYPE_4G("4G"),
    TYPE_WLAN("WLAN");

    companion object {

        fun fromString(value: String): NetworkTypeCompat {
            values().forEach {
                if (it.stringValue == value) return it
            }
            throw IllegalArgumentException("Failed to find NetworkTypeCompat for value $value")
        }

        fun fromType(transportType: TransportType, cellTechnology: CellTechnology? = null): NetworkTypeCompat {
            return when (transportType) {
                TransportType.WIFI -> TYPE_WLAN
                TransportType.CELLULAR -> {
                    when (cellTechnology) {
                        CellTechnology.CONNECTION_2G -> TYPE_2G
                        CellTechnology.CONNECTION_3G -> TYPE_3G
                        CellTechnology.CONNECTION_4G -> TYPE_4G
                        CellTechnology.CONNECTION_5G -> throw IllegalArgumentException("5G not supported to send to the server")
                        else -> throw java.lang.IllegalArgumentException("Incorrect cell technology value or null ${cellTechnology?.name}")
                    }
                }
                else -> throw IllegalArgumentException("Unsupported transport type ${transportType.name}")
            }
        }
    }
}