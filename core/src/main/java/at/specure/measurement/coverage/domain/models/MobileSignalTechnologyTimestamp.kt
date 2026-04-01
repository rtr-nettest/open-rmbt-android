package at.specure.measurement.coverage.domain.models

import at.specure.info.network.MobileNetworkType

data class MobileSignalTechnologyTimestamp(
    val type: MobileNetworkType,
    val signalValueDbm: Int?,
    val frequencyBand: String?,
    val timestamp: Long
)
