package at.specure.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Tables

@Entity(
    tableName = Tables.CAPABILITIES
)
data class CapabilitiesRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val testUUID: String?,
    val signalChunkId: String?,
    val classificationCount: Int,
    val qosSupportInfo: Boolean,
    val rmbtHttpStatus: Boolean
)