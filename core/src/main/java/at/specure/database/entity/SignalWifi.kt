package at.specure.database.entity

import androidx.room.Entity
import at.specure.database.Tables.SIGNAL_WIFI

@Entity(tableName = SIGNAL_WIFI)
class SignalWifi(
    val signal: Int?,
    val wifiLinkSpeed: Int?,
    id: Long,
    testUUID: String,
    cellUuid: String,
    networkTypeId: String,
    timeNs: Long
) : Signal(id, testUUID, cellUuid, networkTypeId, timeNs)