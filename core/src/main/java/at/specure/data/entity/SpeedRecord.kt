package at.specure.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Tables

@Entity(
    tableName = Tables.SPEED
)
data class SpeedRecord(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val testUUID: String,

    val isUpload: Boolean,
    val threadId: Int,
    val timestampNanos: Long,
    val bytes: Long
)