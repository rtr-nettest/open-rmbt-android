package at.rtr.rmbt.android.util

import at.specure.info.network.MobileNetworkType
import com.google.android.gms.maps.model.BitmapDescriptorFactory

fun MobileNetworkType.getMarkerColor(): Float {
    return when (this) {
        // Very slow (2G)
        MobileNetworkType.UNKNOWN,
        MobileNetworkType.GPRS,
        MobileNetworkType.EDGE,
        MobileNetworkType.CDMA,
        MobileNetworkType._1xRTT,
        MobileNetworkType.IDEN,
        MobileNetworkType.GSM -> BitmapDescriptorFactory.HUE_RED

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
        MobileNetworkType.HSPAP -> BitmapDescriptorFactory.HUE_ORANGE

        // 4G LTE
        MobileNetworkType.LTE,
        MobileNetworkType.LTE_CA,
        MobileNetworkType.IWLAN -> BitmapDescriptorFactory.HUE_YELLOW

        // 5G (fastest)
        MobileNetworkType.NR_SA,
        MobileNetworkType.NR_NSA,
        MobileNetworkType.NR_AVAILABLE -> BitmapDescriptorFactory.HUE_GREEN
    }
}