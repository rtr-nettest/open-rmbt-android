package at.specure.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.database.Columns.TEST_UUID_PARENT_COLUMN
import at.specure.database.Tables.GEO_LOCATION

@Entity(tableName = GEO_LOCATION)
data class GeoLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ForeignKey(entity = Test::class, parentColumns = [TEST_UUID_PARENT_COLUMN], childColumns = ["testUUID"], onDelete = ForeignKey.CASCADE)
    val testUUID: String,
    val latitude: Double,
    val longitude: Double,
    val provider: String,
    val speed: Float,
    val altitude: Double,
    val time: Long,
    val timeCorrectionNs: Long,
    val age: Long,
    val accuracy: Float,
    val bearing: Float,
    val isMocked: Boolean,
    val satellitesCount: Int
)