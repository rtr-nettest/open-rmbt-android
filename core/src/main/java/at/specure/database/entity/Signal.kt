package at.specure.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.database.Tables.SIGNAL

@Entity(tableName = SIGNAL)
data class Signal(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ForeignKey(entity = Test::class, parentColumns = ["testUUID"], childColumns = ["testUUID"], onDelete = ForeignKey.CASCADE)
    val testUUID: String?,
    val signal: Int?,
    val cellUuid: String?,
    val wifiLinkSpeed: Int?,
    val networkTypeId: String?,
    val timeNs: Long?,
    // another fields for mobile2g/3g
    val timeNsLast: Long?,
    val bitErrorRate: Int?,
    // another fields for 4G
    val lteRsrp: Int?,
    val lteRsrq: Int?,
    val lteRssnr: Int?,
    val lteCqi: Int?,
    val timingAdvance: Int?
)