package at.specure.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.database.Tables.PING

@Entity(tableName = PING)
data class Ping(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ForeignKey(entity = Test::class, parentColumns = ["testUUID"], childColumns = ["testUUID"], onDelete = ForeignKey.CASCADE)
    val testUUID: String,
    val value: Long,
    val valueServer: Long,
    val testTimeNs: Long
)