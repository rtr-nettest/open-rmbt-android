package at.specure.util.map

import at.specure.info.network.MobileNetworkType
import androidx.core.graphics.toColorInt

const val OFFLINE_GRAY = "#A0A0A0"
const val MIN_SIGNAL=-125
const val MAX_SIGNAL=-85

fun MobileNetworkType.color(): String {
    val colorHex = when (this) {
        MobileNetworkType.OFFLINE,
        MobileNetworkType.UNKNOWN -> OFFLINE_GRAY

        // Very slow (2G)
        MobileNetworkType.GPRS,
        MobileNetworkType.EDGE,
        MobileNetworkType.CDMA,
        MobileNetworkType._1xRTT,
        MobileNetworkType.IDEN,
        MobileNetworkType.GSM -> "#FFDE00"

        // 3G family
        MobileNetworkType.UMTS,
        MobileNetworkType.EVDO_0,
        MobileNetworkType.EVDO_A,
        MobileNetworkType.EVDO_B,
        MobileNetworkType.HSDPA,
        MobileNetworkType.HSUPA,
        MobileNetworkType.HSPA,
        MobileNetworkType.EHRPD,
        MobileNetworkType.TD_SCDMA,
        MobileNetworkType.HSPAP -> "#EFFF00"

        // 4G LTE
        MobileNetworkType.LTE,
        MobileNetworkType.LTE_CA,
        MobileNetworkType.IWLAN -> "#00DEFF"

        // 5G NSA and available (fastest)
        MobileNetworkType.NR_NSA,
        MobileNetworkType.NR_AVAILABLE -> "#0091FF"

        // 5G (fastest)
        MobileNetworkType.NR_SA -> "#5E00FF"
    }
    return colorHex
}

fun MobileNetworkType.colorInt(): Int {
    return this.color().toColorInt()
}

fun MobileNetworkType.blendedColorInt(signalDbm: Int?, ping: Double?): Int {
    val colorMax = this.colorInt()
    val colorMin = OFFLINE_GRAY.toColorInt()
    if (ping == null || (signalDbm != null && signalDbm < MIN_SIGNAL)) {
        return colorMin
    }
    if (signalDbm == null || signalDbm > MAX_SIGNAL) {
        return colorMax
    }
    val blendFactor = (signalDbm - MIN_SIGNAL).toFloat() / (MAX_SIGNAL - MIN_SIGNAL).toFloat()

    val r = ((colorMin shr 16 and 0xFF) + blendFactor * ((colorMax shr 16 and 0xFF) - (colorMin shr 16 and 0xFF))).toInt()
    val g = ((colorMin shr 8 and 0xFF) + blendFactor * ((colorMax shr 8 and 0xFF) - (colorMin shr 8 and 0xFF))).toInt()
    val b = ((colorMin and 0xFF) + blendFactor * ((colorMax and 0xFF) - (colorMin and 0xFF))).toInt()

    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}