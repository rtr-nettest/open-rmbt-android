package at.specure.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.data.Columns
import at.specure.data.Tables

@Entity(tableName = Tables.GEO_LOCATION)
data class GeoLocationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ForeignKey(entity = TestRecord::class, parentColumns = [Columns.TEST_UUID_PARENT_COLUMN], childColumns = ["testUUID"], onDelete = ForeignKey.CASCADE)
    val testUUID: String,
    val latitude: Double,
    val longitude: Double,
    val provider: String,
    val speed: Float,
    val altitude: Double,
    val time: Long,
    val timeCorrectionNanos: Long,
    val ageNanos: Long,
    val accuracy: Float,
    val bearing: Float,
    val isMocked: Boolean,
    val satellitesCount: Int
)