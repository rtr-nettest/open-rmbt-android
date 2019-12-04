package at.specure.database.entity

import androidx.room.Entity
import at.specure.database.Tables.UPLOAD_TRAFFIC

@Entity(tableName = UPLOAD_TRAFFIC)
class UploadTrafficRecord(
    id: Long = 0,
    testUUID: String,
    threadNumber: Int,
    timeNanos: Long,
    bytes: Long
) : DownloadTrafficRecord(id, testUUID, threadNumber, timeNanos, bytes)