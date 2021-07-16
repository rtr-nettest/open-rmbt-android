package at.specure.data

import at.specure.info.TransportType
import at.specure.info.cell.CellTechnology
import at.specure.info.network.MobileNetworkType
import at.specure.info.strength.SignalStrengthInfo
import timber.log.Timber

enum class NetworkTypeCompat(val stringValue: String, val minSignalValue: Int, val maxSignalValue: Int) {

    TYPE_2G("2G", SignalStrengthInfo.CELLULAR_SIGNAL_MIN, SignalStrengthInfo.CELLULAR_SIGNAL_MAX),
    TYPE_3G("3G", SignalStrengthInfo.WCDMA_RSRP_SIGNAL_MIN, SignalStrengthInfo.WCDMA_RSRP_SIGNAL_MAX),
    TYPE_4G("4G", SignalStrengthInfo.LTE_RSRP_SIGNAL_MIN, SignalStrengthInfo.LTE_RSRP_SIGNAL_MAX),
    TYPE_5G("5G", SignalStrengthInfo.NR_RSRP_SIGNAL_MIN, SignalStrengthInfo.NR_RSRP_SIGNAL_MAX),
    TYPE_5G_NSA("5G", SignalStrengthInfo.NR_RSRP_SIGNAL_MIN, SignalStrengthInfo.NR_RSRP_SIGNAL_MAX),
    TYPE_5G_AVAILABLE("4G (+5G)", SignalStrengthInfo.LTE_RSRP_SIGNAL_MIN, SignalStrengthInfo.LTE_RSRP_SIGNAL_MAX),
    TYPE_WLAN("WLAN", SignalStrengthInfo.WIFI_MIN_SIGNAL_VALUE, SignalStrengthInfo.WIFI_MAX_SIGNAL_VALUE),
    TYPE_LAN("ETHERNET", Int.MIN_VALUE, Int.MIN_VALUE),
    TYPE_BROWSER("BROWSER", Int.MIN_VALUE, Int.MIN_VALUE),
    TYPE_UNKNOWN("UNKNOWN", Int.MIN_VALUE, Int.MIN_VALUE);

    companion object {

        fun fromResultIntType(value: Int): NetworkTypeCompat {

            if (value == Int.MAX_VALUE) {
                TYPE_2G
            }

            var cellTechnology: CellTechnology? = null
            val mobileNetworkType = MobileNetworkType.fromValue(value)
            val transportType = if (mobileNetworkType == MobileNetworkType.UNKNOWN) {
                when (value) {
                    TYPE_ETHERNET_VALUE -> TransportType.ETHERNET
                    TYPE_WIFI_VALUE -> TransportType.WIFI
                    TYPE_BROWSER_VALUE -> TransportType.BROWSER
                    else -> {
                        Timber.e("Unsupported type $value")
                        null
                    }
                }
            } else {
                cellTechnology = CellTechnology.fromMobileNetworkType(mobileNetworkType)
                TransportType.CELLULAR
            }

            return fromType(transportType, cellTechnology)
        }

        fun fromString(value: String): NetworkTypeCompat {
            values().forEach {
                if (it.stringValue.equals(value, ignoreCase = true)) return it
            }

            ServerNetworkType.values().forEach {
                if (it.stringValue.equals(value, ignoreCase = true) && it.compatType != null) return it.compatType
            }
            return TYPE_UNKNOWN
//            throw IllegalArgumentException("Failed to find NetworkTypeCompat for value $value")
        }

        fun fromType(transportType: TransportType?, cellTechnology: CellTechnology? = null): NetworkTypeCompat {
            return if (transportType == null) {
                TYPE_UNKNOWN
            } else when (transportType) {
                TransportType.BROWSER -> TYPE_BROWSER
                TransportType.ETHERNET -> TYPE_LAN
                TransportType.WIFI -> TYPE_WLAN
                TransportType.CELLULAR -> {
                    when (cellTechnology) {
                        CellTechnology.CONNECTION_2G -> TYPE_2G
                        CellTechnology.CONNECTION_3G -> TYPE_3G
                        CellTechnology.CONNECTION_4G -> TYPE_4G
                        CellTechnology.CONNECTION_4G_5G -> TYPE_5G_AVAILABLE
                        CellTechnology.CONNECTION_5G -> TYPE_5G
                        else -> {
                            Timber.e("Incorrect cell technology value or null ${cellTechnology?.name}")
                            TYPE_UNKNOWN
                        }
                    }
                }
                else -> {
                    Timber.e("Unsupported transport type ${transportType.name}")
                    TYPE_UNKNOWN
                }
            }
        }

        const val TYPE_BLUETOOTH_VALUE = 107
        const val TYPE_ETHERNET_VALUE = 106
        const val TYPE_WIFI_VALUE = 99
        const val TYPE_BROWSER_VALUE = 98
    }
}

enum class ServerNetworkType(
    val intValue: Int,
    val stringValue: String,
    val compatType: NetworkTypeCompat?,
    val transportType: TransportType?,
    val mobileNetworkType: MobileNetworkType?
) {
    TYPE_2G_GSM(1, "2G (GSM)", NetworkTypeCompat.TYPE_2G, TransportType.CELLULAR, MobileNetworkType.GSM),
    TYPE_2G_EDGE(2, "2G (EDGE)", NetworkTypeCompat.TYPE_2G, TransportType.CELLULAR, MobileNetworkType.EDGE),
    TYPE_3G_UMTS(3, "3G (UMTS)", NetworkTypeCompat.TYPE_3G, TransportType.CELLULAR, MobileNetworkType.UMTS),
    TYPE_2G_CDMA(4, "2G (CDMA)", NetworkTypeCompat.TYPE_2G, TransportType.CELLULAR, MobileNetworkType.CDMA),
    TYPE_2G_EVDO_0(5, "2G (EVDO_0)", NetworkTypeCompat.TYPE_2G, TransportType.CELLULAR, MobileNetworkType.EVDO_0),
    TYPE_2G_EVDO_A(6, "2G (EVDO_A)", NetworkTypeCompat.TYPE_2G, TransportType.CELLULAR, MobileNetworkType.EVDO_A),
    TYPE_2G_1xRTT(7, "2G (1xRTT)", NetworkTypeCompat.TYPE_2G, TransportType.CELLULAR, MobileNetworkType._1xRTT),
    TYPE_3G_HSDPA(8, "3G (HSDPA)", NetworkTypeCompat.TYPE_3G, TransportType.CELLULAR, MobileNetworkType.HSDPA),
    TYPE_3G_HSUPA(9, "3G (HSUPA)", NetworkTypeCompat.TYPE_3G, TransportType.CELLULAR, MobileNetworkType.HSUPA),
    TYPE_3G_HSPA(10, "3G (HSPA)", NetworkTypeCompat.TYPE_3G, TransportType.CELLULAR, MobileNetworkType.HSPA),
    TYPE_2G_IDEN(11, "2G (IDEN)", NetworkTypeCompat.TYPE_2G, TransportType.CELLULAR, MobileNetworkType.IDEN),
    TYPE_2G_EVDO_B(12, "2G (EVDO_B)", NetworkTypeCompat.TYPE_2G, TransportType.CELLULAR, MobileNetworkType.EVDO_B),
    TYPE_4G_LTE(13, "4G (LTE)", NetworkTypeCompat.TYPE_4G, TransportType.CELLULAR, MobileNetworkType.LTE),
    TYPE_2G_EHRPD(14, "2G (EHRPD)", NetworkTypeCompat.TYPE_2G, TransportType.CELLULAR, MobileNetworkType.EHRPD),
    TYPE_3G_HSPA_P(15, "3G (HSPA+)", NetworkTypeCompat.TYPE_3G, TransportType.CELLULAR, MobileNetworkType.HSPAP),
    TYPE_4G_LTE_CA(19, "4G (LTE CA)", NetworkTypeCompat.TYPE_4G, TransportType.CELLULAR, MobileNetworkType.LTE_CA),
    TYPE_5G_NR(20, "5G (NR)", NetworkTypeCompat.TYPE_5G, TransportType.CELLULAR, MobileNetworkType.NR),
    TYPE_5G_NR_NSA(41, "5G (NSA)", NetworkTypeCompat.TYPE_5G_NSA, TransportType.CELLULAR, MobileNetworkType.NR_NSA),
    TYPE_5G_NR_AVAILABLE(40, "4G+(5G)", NetworkTypeCompat.TYPE_5G_NSA, TransportType.CELLULAR, MobileNetworkType.NR_AVAILABLE),
    TYPE_CLI(97, "CLI", null, null, null),
    TYPE_BROWSER(98, "BROWSER", null, null, null),
    TYPE_WLAN(99, "WLAN", NetworkTypeCompat.TYPE_WLAN, TransportType.WIFI, null),
    TYPE_2G_3G(101, "2G/3G", NetworkTypeCompat.TYPE_3G, TransportType.CELLULAR, MobileNetworkType.HSUPA),
    TYPE_3G_4G(102, "3G/4G", NetworkTypeCompat.TYPE_3G, TransportType.CELLULAR, MobileNetworkType.HSUPA),
    TYPE_2G_4G(103, "2G/4G", NetworkTypeCompat.TYPE_4G, TransportType.CELLULAR, MobileNetworkType.EVDO_A),
    TYPE_2G_3G_4G(104, "2G/3G/4G", NetworkTypeCompat.TYPE_4G, TransportType.CELLULAR, MobileNetworkType.LTE),
    TYPE_MOBILE(105, "MOBILE", NetworkTypeCompat.TYPE_3G, TransportType.CELLULAR, MobileNetworkType.HSUPA),
    TYPE_ETHERNET(106, "Ethernet", NetworkTypeCompat.TYPE_LAN, TransportType.ETHERNET, null),
    TYPE_BLUETOOTH(107, "Bluetooth", null, TransportType.BLUETOOTH, null),
    TYPE_UNKNOWN(-1, "UNKNOWN", null, null, null),
    TYPE_UNKNOWN2(Int.MAX_VALUE, "UNKNOWN", null, null, null)
}