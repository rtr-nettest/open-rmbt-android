package at.specure.data

data class HistoryLoopMedian(
    val loopUuid: String,
    val pingMedianMillis: Float,
    val jitterMedianMillis: Float,
    val packetLossMedian: Float,
    val downloadMedianMbps: Float,
    val uploadMedianMbps: Float,
    val qosMedian: Float?
)
