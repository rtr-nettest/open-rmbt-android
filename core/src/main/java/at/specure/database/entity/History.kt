package at.specure.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.database.Tables.HISTORY

@Entity(tableName = HISTORY)
data class History(
    @PrimaryKey
    val testUUID: String,
    val networkType: String?,
    val networkName: String?,
    val networkProviderName: String?,
    val timeMillis: Long,
    val timeString: String?,
    val timezone: String,
    val deviceName: String,
    /**
     * result in percent
     */
    val qosResult: String,
    val pingShortestMs: String,
    val pingMs: String,
    val downloadSpeedMbps: String,
    val uploadSpeedMbps: String,
    val downloadSpeedClass: Int,
    val uploadSpeedClass: Int,
    val pingShortestClass: Int,
    val pingClass: Int
)