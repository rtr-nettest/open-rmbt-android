package at.specure.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Tables
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = Tables.FENCES_RESULT_ITEM
)
data class FencesResultItemRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val testUUID: String,
    val fenceRemoteId: Long?,
    val networkTechnologyId: Int?, // 41
    @Deprecated("Client should use technologyId and convert to string")
    val networkTechnologyName: String?, // "NR NSA"
    val latitude: Double?,
    val longitude: Double?,
    val fenceRadiusMeters: Double?,
    val durationMillis: Long?, // duration of fence in millis
    val offsetMillis: Long?, // from the start of the test - official start is when coverage response arrives so it can be negative too
    val averagePingMillis: Double?,
)

fun FencesResultItemRecord.generateHash(): String {
    return "${this.id}-${this.fenceRadiusMeters}-${this.offsetMillis}"
}