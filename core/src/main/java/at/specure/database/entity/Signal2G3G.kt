package at.specure.database.entity

import androidx.room.Entity
import at.specure.database.Tables

@Entity(tableName = Tables.SIGNAL_2G_3G)
class Signal2G3G(
    val signalStrength: Int?,
    val timeNsLast: Long?,
    val bitErrorRate: Int?, id: Long, testUUID: String, cellUuid: String, networkTypeId: String, timeNs: Long
) : Signal(id, testUUID, cellUuid, networkTypeId, timeNs)