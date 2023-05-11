package at.specure.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Tables

@Entity(
    tableName = Tables.PING
)
data class PingRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val testUUID: String,
    val value: Long,
    val valueServer: Long,
    val testTimeNanos: Long
)