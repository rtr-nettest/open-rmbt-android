package at.specure.database.entity

import androidx.room.Entity
import at.specure.database.Tables.SIGNAL_4G

@Entity(tableName = SIGNAL_4G)
class Signal4G(
    val lteRsrp: Int?,
    val lteRsrq: Int?,
    val lteRssnr: Int?,
    val lteCqi: Int?,
    val timingAdvance: Int?,
    id: Long, testUUID: String, cellUuid: String, networkTypeId: String, timeNs: Long
) : Signal(id, testUUID, cellUuid, networkTypeId, timeNs)