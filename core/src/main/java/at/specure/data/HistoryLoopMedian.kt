package at.specure.data

data class HistoryLoopMedian(
    val loopUuid: String,
    val pingMedian: Float,
    val jitterMedian: Float,
    val packetLossMedian: Float,
    val downloadMedian: Float,
    val uploadMedian: Float,
    val qosMedian: Float?
)
