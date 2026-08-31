package at.specure.measurement.coverage.data

import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NetworkInfo

fun NetworkInfo?.getMobileNetworkType(): MobileNetworkType {
    if (this is CellNetworkInfo) {
        return this.networkType
    }
    return MobileNetworkType.UNKNOWN
}

fun NetworkInfo?.getSignalStrengthValue(): Int? {
    if (this is CellNetworkInfo) {
        return this.signalStrength?.value
    }
    return null
}

/**
 * Signal strength (dBm) to use for the coverage measurement - for both the recorded fences and the
 * on-screen value.
 *
 * For 5G NSA the LTE anchor and the NR secondary carrier each report a signal. Combine them: when
 * both are available use the weaker one (minimum dBm, the more conservative value); when only one is
 * available (some devices do not report the NR signal) use that one. For every other technology this
 * is just the primary signal.
 */
fun NetworkInfo?.getCombinedSignalStrengthValue(secondaryNetworkInfo: NetworkInfo? = null): Int? {
    val primarySignal = this.getSignalStrengthValue()
    if ((this as? CellNetworkInfo)?.networkType != MobileNetworkType.NR_NSA) {
        return primarySignal
    }
    val secondarySignal = secondaryNetworkInfo.getSignalStrengthValue()
    return when {
        primarySignal != null && secondarySignal != null -> minOf(primarySignal, secondarySignal)
        else -> primarySignal ?: secondarySignal
    }
}

/**
 * Frequency band label for the cell, e.g. "1800".
 *
 * For 5G NSA the LTE anchor carries the primary band while the NR secondary cell carries its own
 * band. When [secondaryNetworkInfo] is a 5G NSA secondary cell that reports a band, both are shown
 * as "<LTE>/<NR>" (e.g. "800/3600"). If the device does not report the NR band, only the LTE band is
 * returned; if no band is known at all, null is returned.
 */
fun NetworkInfo?.getFrequencyBand(secondaryNetworkInfo: NetworkInfo? = null): String? {
    if (this !is CellNetworkInfo) return null
    val primaryBand = this.band?.name ?: return null
    val secondaryBand = (secondaryNetworkInfo as? CellNetworkInfo)?.band?.name
    return if (this.networkType == MobileNetworkType.NR_NSA && !secondaryBand.isNullOrEmpty()) {
        "$primaryBand/$secondaryBand"
    } else {
        primaryBand
    }
}