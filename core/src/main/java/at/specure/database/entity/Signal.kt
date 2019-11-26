package at.specure.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.database.Columns.TEST_UUID_PARENT_COLUMN
import at.specure.database.Tables.SIGNAL

@Entity(tableName = SIGNAL)
data class Signal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ForeignKey(entity = Test::class, parentColumns = [TEST_UUID_PARENT_COLUMN], childColumns = ["testUUID"], onDelete = ForeignKey.CASCADE)
    val testUUID: String,
    val cellUuid: String,
    /**
     * Value according to [at.specure.info.network.MobileNetworkType] or 99 if connected via Wi-Fi :/
     */
    val networkTypeId: Int,
    // wifi
    val signal: Int?,
    val wifiLinkSpeed: Int?,
    // 2G/3G
    val timeNanos: Long?,
    val timeNanosLast: Long?,
    val bitErrorRate: Int?,
    // 4G
    val lteRsrp: Int?,
    val lteRsrq: Int?,
    val lteRssnr: Int?,
    val lteCqi: Int?,
    val timingAdvance: Int?
)