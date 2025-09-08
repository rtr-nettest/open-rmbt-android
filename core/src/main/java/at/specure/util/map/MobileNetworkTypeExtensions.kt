package at.specure.util.map

import at.specure.info.network.MobileNetworkType
import androidx.core.graphics.toColorInt

fun MobileNetworkType.getMarkerColorInt(): Int {
    val colorHex = when (this) {
        // Very slow (2G)
        MobileNetworkType.UNKNOWN -> "#d9d9d9"

        MobileNetworkType.GPRS,
        MobileNetworkType.EDGE,
        MobileNetworkType.CDMA,
        MobileNetworkType._1xRTT,
        MobileNetworkType.IDEN,
        MobileNetworkType.GSM -> "#fca636"

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
        MobileNetworkType.HSPAP -> "#e16462"

        // 4G LTE
        MobileNetworkType.LTE,
        MobileNetworkType.LTE_CA,
        MobileNetworkType.IWLAN -> "#b12a90"

        // 5G NSA and available (fastest)
        MobileNetworkType.NR_NSA,
        MobileNetworkType.NR_AVAILABLE -> "#6a00a8"

        // 5G (fastest)
        MobileNetworkType.NR_SA -> "#0d0887"
    }
    return colorHex.toColorInt()
}
