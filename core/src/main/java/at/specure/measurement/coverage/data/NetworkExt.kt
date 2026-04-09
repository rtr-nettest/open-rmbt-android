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

fun NetworkInfo?.getFrequencyBand(): String? {
    if (this is CellNetworkInfo) {
        return this.band?.name
    }
    return null
}