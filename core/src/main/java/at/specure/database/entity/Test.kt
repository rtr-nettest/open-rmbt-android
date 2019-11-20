package at.specure.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.database.Tables.TEST
import at.specure.info.TransportType

@Entity(tableName = TEST)
data class Test(
    @PrimaryKey
    val UUID: String,
    val token: String?,
    val portRemote: String?,
    val downloadedBytes: Long?,
    val uploadedBytes: Long?,
    val totalDownloadedBytes: Long?,
    val totalUploadedBytes: Long?,
    val encryptionType: String?,
    val ipPublicClient: String?,
    val ipPublicServer: String?,
    val downloadPhaseDurationNs: Long?,
    val uploadPhaseDurationNs: Long?,
    val threadNumber: Int?,
    val downloadSpeedBps: Long?,
    val uploadSpeedBps: Long?,
    val pingShortestNs: Long?,
    val downloadedBytesOnInterface: Long?,
    val uploadedBytesOnInterface: Long?,
    val downloadedBytesOnDownloadInterface: Long?,
    val uploadedBytesOnDownloadInterface: Long?,
    val downloadedBytesOnUploadInterface: Long?,
    val uploadedBytesOnUploadInterfaceKb: Long?,
    val timeDownloadOffsetNs: Long?,
    val timeUploadOffsetNs: Long?,
    val state: String?,
    val transportType: TransportType,
    val time: Long?,
    // wifi
    val wifiSupplicantState: String?,
    val wifiSupplicantDetailedState: String?,
    val wifiSsid: String?,
    val wifiBssid: String?,
    val wifiNetworkId: String?,
    // mobile
    val telNetworkOperatorName: String?,
    val telNetworkOperator: String?,
    val telNetworkIsRoaming: Boolean?,
    val telNetworkCountry: String?,
    val telNetworkSimOperatorName: String?,
    val telNetworkSimOperator: String?,
    val telNetworkSimCountry: String?,
    val telPhoneType: String?,
    val telDataState: String?,
    val telApn: String?
)