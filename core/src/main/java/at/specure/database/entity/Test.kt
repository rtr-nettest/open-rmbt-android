package at.specure.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.database.Tables.TEST

@Entity(tableName = TEST)
data class Test(
    @PrimaryKey
    val testUUID: String,
    val testToken: String?,
    val testPortRemote: String?,
    val testDownloadedBytes: Long?,
    val testUploadedBytes: Long?,
    val testTotalDownloadedBytes: Long?,
    val testTotalUploadedBytes: Long?,
    val testEncryption: String?,
    val testIPLocal: String?,
    val testIPServer: String?,
    val testDownloadPhaseDurationNs: Long?,
    val testUploadPhaseDurationNs: Long?,
    val testThreadNumber: Int?,
    val testDownloadSpeedKb: Int?,
    val testUploadSpeedKb: Int?,
    val testPingShortestNs: Long?,
    val testDownloadedBytesOnInterfaceKb: Long?,
    val testUploadedBytesOnInterfaceKb: Long?,
    val testDownloadedBytesOnDownloadInterfaceKb: Long?,
    val testUploadedBytesOnDownloadInterfaceKb: Long?,
    val testDownloadedBytesOnUploadInterfaceKb: Long?,
    val testUploadedBytesOnUploadInterfaceKb: Long?,
    val timeDownloadOffsetNs: Long?,
    val timeUploadOffsetNs: Long?,
    val testState: String?,
    val networkType: Int?,
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