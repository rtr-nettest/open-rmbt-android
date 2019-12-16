package at.specure.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Classification
import at.specure.data.NetworkTypeCompat
import at.specure.data.Tables

@Entity(tableName = Tables.HISTORY)
data class History(
    @PrimaryKey
    val testUUID: String,
    val model: String,
    val networkType: NetworkTypeCompat,
    val ping: Int,
    val pingClassification: Classification,
    val pingShortest: Int,
    val pingShortestClassification: Classification,
    val speedDownload: Double,
    val speedDownloadClassification: Classification,
    val speedUpload: Double,
    val speedUploadClassification: Classification,
    val time: Long,
    val timeString: String,
    val timezone: String
)