package at.specure.data

import at.rmbt.client.control.HistoryItemResponse
import at.rmbt.client.control.HistoryResponse
import at.specure.data.entity.History

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