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