package at.specure.database.entity

import androidx.room.Entity
import at.specure.database.Tables.TEST_TRAFFIC_UPLOAD_ITEM

@Entity(tableName = TEST_TRAFFIC_UPLOAD_ITEM)
class TestTrafficUpload(
    id: Long,
    testUUID: String,
    threadNumber: Int,
    timeNanos: Long,
    bytes: Long
) : TestTrafficDownload(id, testUUID, threadNumber, timeNanos, bytes)