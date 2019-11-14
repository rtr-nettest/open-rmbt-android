package at.specure.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.database.Tables.TEST_TRAFFIC_ITEM

@Entity(tableName = TEST_TRAFFIC_ITEM)
data class TestTrafficItem(

    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ForeignKey(entity = Test::class, parentColumns = ["testUUID"], childColumns = ["testUUID"], onDelete = ForeignKey.CASCADE)
    val testUUID: String,
    val direction: String,
    val threadNumber: Int,
    val time: Long,
    val bytes: Long
)