package at.specure.data

import at.rmbt.client.control.HistoryItemResponse
import at.rmbt.client.control.HistoryResponse
import at.rmbt.client.control.QoEClassification
import at.rmbt.client.control.ServerTestResultItem
import at.rmbt.client.control.ServerTestResultResponse
import at.rmbt.client.control.TestResultDetailItem
import at.rmbt.client.control.TestResultDetailResponse
import at.specure.data.entity.History
import at.specure.data.entity.QoeInfoRecord
import at.specure.data.entity.TestResultDetailsRecord
import at.specure.data.entity.TestResultRecord
import at.specure.result.QoECategory

fun HistoryResponse.toModelList(): List<History> = history.map { it.toModel() }

fun HistoryItemResponse.toModel() = History(
    testUUID = testUUID,
    model = model,
    networkType = NetworkTypeCompat.fromString(networkType),
    ping = ping,
    pingClassification = Classification.fromValue(pingClassification),
    pingShortest = pingShortest,
    pingShortestClassification = Classification.fromValue(pingShortestClassification),
    speedDownload = speedDownload,
    speedDownloadClassification = Classification.fromValue(speedDownloadClassification),
    speedUpload = speedUpload,
    speedUploadClassification = Classification.fromValue(speedUploadClassification),
    time = time,
    timeString = timeString,
    timezone = timezone
)

fun ServerTestResultResponse.toModel(testUUID: String): TestResultRecord = resultItem.first().toModel(testUUID)

fun ServerTestResultItem.toModel(testUUID: String): TestResultRecord {

    val signal: Int? = measurementItem.lte_rsrp ?: measurementItem.signalStrength

    return TestResultRecord(
        clientOpenUUID = clientOpenUUID,
        testOpenUUID = testOpenUUID,
        uuid = testUUID,
        downloadClass = Classification.fromValue(measurementItem.downloadClass),
        downloadSpeedKbs = measurementItem.downloadSpeedKbs,
        uploadClass = Classification.fromValue(measurementItem.uploadClass),
        uploadSpeedKbs = measurementItem.uploadSpeedKbs,
        pingClass = Classification.fromValue(measurementItem.pingClass),
        pingMillis = measurementItem.pingMillis,
        signalClass = Classification.fromValue(measurementItem.signalClass),
        signalStrength = signal,
        locationText = locationText,
        latitude = latitude,
        longitude = longitude,
        networkTypeRaw = networkType,
        shareText = shareText,
        shareTitle = shareSubject,
        timestamp = timestamp,
        timeText = timeText,
        timezone = timezone,
        networkName = networkItem.wifiNetworkSSID,
        networkProviderName = networkItem.providerName,
        networkTypeText = networkItem.networkTypeString,
        networkType = NetworkTypeCompat.fromResultIntType(networkType)
    )
}

fun ServerTestResultResponse.toQoeModel(testUUID: String): List<QoeInfoRecord> = resultItem.first().toQoeModel(testUUID)

fun ServerTestResultItem.toQoeModel(testUUID: String): List<QoeInfoRecord> {
    return qoeClassifications.map {
        it.toModel(testUUID)
    }
}

fun QoEClassification.toModel(testUUID: String): QoeInfoRecord {
    return QoeInfoRecord(
        testUUID = testUUID,
        category = QoECategory.fromString(category),
        classification = Classification.fromValue(classification),
        percentage = quality
    )
}

fun TestResultDetailResponse.toModelList(testUUID: String): List<TestResultDetailsRecord> = details.map { it.toModel(testUUID) }

fun TestResultDetailItem.toModel(testUUID: String): TestResultDetailsRecord =
    TestResultDetailsRecord(testUUID, openTestUUID, openUuid, time, timezone, title, value)