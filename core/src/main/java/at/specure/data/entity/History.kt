package at.specure.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Classification
import at.specure.data.NetworkTypeCompat
import at.specure.data.Tables

@Entity(
    tableName = Tables.HISTORY
)
data class History(
    @PrimaryKey
    val testUUID: String,
    val openTestUUID: String?,
    val loopUUID: String?,
    val referenceUUID: String,
    val model: String,
    val networkType: NetworkTypeCompat,
    val ping: String,
    val pingClassification: Classification,
    val pingShortest: String,
    val pingShortestClassification: Classification,
    val speedDownload: String,
    val speedDownloadClassification: Classification,
    val speedUpload: String,
    val speedUploadClassification: Classification,
    val signalClassification: Classification,
    val time: Long,
    val timeString: String,
    val timezone: String,
    val qos: String?,
    val jitterMillis: String?,
    val packetLossPercents: String?,
    val packetLossClassification: Classification?,
    val jitterClassification: Classification?,
    val isCoverageResult: Boolean?,
    val fencesCount: Int?
)