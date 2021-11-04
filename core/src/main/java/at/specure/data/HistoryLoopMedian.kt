package at.specure.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = Tables.HISTORY_MEDIAN)
data class HistoryLoopMedian(
    @PrimaryKey
    val loopUuid: String,
    val pingMedianMillis: Float,
    val jitterMedianMillis: Float,
    val packetLossMedian: Float,
    val downloadMedianMbps: Float,
    val uploadMedianMbps: Float,
    val qosMedian: Float?
)
