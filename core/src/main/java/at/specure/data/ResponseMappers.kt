package at.specure.data

import at.rmbt.client.control.HistoryItemONTResponse
import at.rmbt.client.control.HistoryItemResponse
import at.rmbt.client.control.HistoryONTResponse
import at.rmbt.client.control.HistoryResponse
import at.rmbt.client.control.MapFilterObjectResponse
import at.rmbt.client.control.MapFiltersResponse
import at.rmbt.client.control.MapTypeOptionsResponse
import at.rmbt.client.control.MarkerMeasurementItem
import at.rmbt.client.control.MarkerMeasurementsResponse
import at.rmbt.client.control.MarkerNetworkItem
import at.rmbt.client.control.MarkersResponse
import at.rmbt.client.control.PingGraphItemResponse
import at.rmbt.client.control.QoEClassification
import at.rmbt.client.control.QosTestCategoryDescription
import at.rmbt.client.control.QosTestResult
import at.rmbt.client.control.QosTestResultDetailResponse
import at.rmbt.client.control.ServerTestResultItem
import at.rmbt.client.control.ServerTestResultResponse
import at.rmbt.client.control.SignalGraphItemResponse
import at.rmbt.client.control.SignalMeasurementRequestResponse
import at.rmbt.client.control.SpeedGraphItemResponse
import at.rmbt.client.control.TestResultDetailItem
import at.rmbt.client.control.TestResultDetailResponse
import at.rmbt.client.control.data.MapFilterType
import at.specure.data.entity.History
import at.specure.data.entity.MarkerMeasurementRecord
import at.specure.data.entity.QoeInfoRecord
import at.specure.data.entity.QosCategoryRecord
import at.specure.data.entity.QosTestGoalRecord
import at.specure.data.entity.QosTestItemRecord
import at.specure.data.entity.SignalMeasurementInfo
import at.specure.data.entity.TestResultDetailsRecord
import at.specure.data.entity.TestResultGraphItemRecord
import at.specure.data.entity.TestResultRecord
import at.specure.result.QoECategory
import at.specure.result.QoSCategory
import org.joda.time.DateTime
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

fun HistoryResponse.toModelList(): List<History> = history?.map { it.toModel() } ?: emptyList()

fun HistoryItemResponse.toModel() = History(
    testUUID = testUUID,
    loopUUID = loopUUID,
    referenceUUID = if (loopUUID == null) testUUID else loopUUID!!,
    model = model,
    networkType = NetworkTypeCompat.fromString(
        networkType ?: ServerNetworkType.TYPE_UNKNOWN2.stringValue
    ),
    ping = ping,
    pingClassification = Classification.fromValue(pingClassification),
    pingShortest = pingShortest,
    pingShortestClassification = Classification.fromValue(pingShortestClassification),
    speedDownload = speedDownload,
    speedDownloadClassification = Classification.fromValue(speedDownloadClassification),
    speedUpload = speedUpload,
    speedUploadClassification = Classification.fromValue(speedUploadClassification),
    signalClassification = signalClassification?.let { Classification.fromValue(it) }
        ?: Classification.NONE,
    time = time,
    timeString = timeString,
    timezone = timezone,
    qos = qosResultPercents,
    jitterMillis = jitterMillisResult,
    packetLossPercents = packetLossPercents,
    packetLossClassification = classificationPacketLoss?.let { Classification.fromValue(it) },
    jitterClassification = classificationJitter?.let { Classification.fromValue(it) }
)

fun HistoryONTResponse.toModelList(): List<History> =
    history?.historyList?.map { it.toModel() } ?: emptyList()

fun HistoryItemONTResponse.toModel(): History {
    val dateTime = DateTime(measurementDate)
    return History(
        testUUID = testUUID,
        loopUUID = null,
        referenceUUID = testUUID, // todo: change when loop uuid will be available
        model = "",
        networkType = NetworkTypeCompat.fromString(
            networkType ?: ServerNetworkType.TYPE_UNKNOWN2.stringValue
        ),
        ping = ping.toFormattedPing(),
        pingClassification = Classification.NONE,
        pingShortest = ping.toFormattedPing(),
        pingShortestClassification = Classification.NONE,
        speedDownload = speedDownload.toSpeedValue(),
        speedDownloadClassification = Classification.NONE,
        speedUpload = speedUpload.toSpeedValue(),
        speedUploadClassification = Classification.NONE,
        signalClassification = Classification.NONE,
        time = dateTime.millis,
        timeString = SimpleDateFormat(
            "dd.MM.yy, HH:mm:ss",
            Locale.US
        ).format(Date(dateTime.millis)),
        timezone = "",
        qos = qosResultPercents?.roundToInt().toString(),
        jitterMillis = jitterMillisResult?.roundToInt().toString(),
        packetLossPercents = packetLossPercents?.roundToInt().toString(),
        packetLossClassification = Classification.NONE,
        jitterClassification = Classification.NONE
    )
}

private fun Float.toSpeedValue(): String {
    return when {
        this >= 10 -> this.roundToInt().toString()
        this >= 1 -> DecimalFormat("0.#").format(this)
        this >= 0.01 -> DecimalFormat("0.##").format(this)
        else -> "0"
    }
}

private fun Float.toFormattedPing(): String {
    return this.roundToInt().toString()
}

fun ServerTestResultResponse.toModel(testUUID: String): TestResultRecord =
    resultItem.first().toModel(testUUID)

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
        pingMillis = measurementItem.pingMillis.toDouble(),
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
        networkType = NetworkTypeCompat.fromResultIntType(networkType),
        jitterMillis = measurementItem.jitterMillis?.toDouble(),
        packetLossPercents = measurementItem.packetLossPercents,
        packetLossClass = measurementItem.packetLossClass?.let { Classification.fromValue(it) },
        jitterClass = measurementItem.jitterClass?.let { Classification.fromValue(it) }
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
        percentage = quality,
        info = null,
        priority = 0
    )
}

fun SignalGraphItemResponse.toModel(testUUID: String): TestResultGraphItemRecord {
    return TestResultGraphItemRecord(
        testUUID = testUUID,
        time = timeMillis,
        value = signalStrength?.toLong() ?: lteRsrp?.toLong() ?: 0,
        type = TestResultGraphItemRecord.Type.SIGNAL
    )
}

fun PingGraphItemResponse.toModel(testUUID: String): TestResultGraphItemRecord {
    return TestResultGraphItemRecord(
        testUUID = testUUID,
        time = timeMillis,
        value = durationMillis.toLong(),
        type = TestResultGraphItemRecord.Type.PING
    )
}

fun SpeedGraphItemResponse.toModel(testUUID: String, type: TestResultGraphItemRecord.Type): TestResultGraphItemRecord {
    return TestResultGraphItemRecord(
        testUUID = testUUID,
        time = timeMillis,
        value = bytes,
        type = type
    )
}

fun TestResultDetailResponse.toModelList(testUUID: String): List<TestResultDetailsRecord> = details.map { it.toModel(testUUID) }

fun TestResultDetailItem.toModel(testUUID: String): TestResultDetailsRecord =
    TestResultDetailsRecord(testUUID, openTestUUID, openUuid, time, timezone, title, value)

fun MarkersResponse.toModelList(): List<MarkerMeasurementRecord> = measurements?.map { it.toModel() } ?: emptyList()

fun MarkerMeasurementsResponse.toModel(): MarkerMeasurementRecord {
    val upload = measurement.extractFromList(1) as? MarkerMeasurementItem
    val download = measurement.extractFromList(0) as? MarkerMeasurementItem
    val signal = measurement.extractFromList(3) as? MarkerMeasurementItem
    val ping = measurement.extractFromList(2) as? MarkerMeasurementItem

    val networkType = networkItems.extractFromList(0) as? MarkerNetworkItem
    val provider = networkItems.extractFromList(1) as? MarkerNetworkItem
    val wifi = networkItems.extractFromList(2) as? MarkerNetworkItem

    return MarkerMeasurementRecord(
        longitude,
        latitude,
        Classification.values()[upload?.classification ?: Classification.NONE.intValue],
        upload?.value ?: "-",
        Classification.values()[download?.classification ?: Classification.NONE.intValue],
        download?.value ?: "-",
        Classification.values()[signal?.classification ?: Classification.NONE.intValue],
        signal?.value ?: "-",
        Classification.values()[ping?.classification ?: Classification.NONE.intValue],
        ping?.value ?: "-",
        networkType?.value,
        provider?.value,
        wifi?.value,
        openTestUUID,
        time,
        timeString
    )
}

fun QosTestResultDetailResponse.toModels(
    testUUID: String,
    language: String
): Triple<List<QosCategoryRecord>, List<QosTestItemRecord>, List<QosTestGoalRecord>> {

    val categories = mutableListOf<QosCategoryRecord>()
    val results = mutableListOf<QosTestItemRecord>()
    val goals = mutableListOf<QosTestGoalRecord>()

    this.qosResultDetailsDesc.forEach { qosGoal ->
        val qoSCategory = QoSCategory.fromString(qosGoal.testCategory)
        val success = qosGoal.resultStatus.equals("ok", true)
        qosGoal.qosTestUids.forEach { qosTestId ->
            goals.add(
                QosTestGoalRecord(
                    testUUID = testUUID,
                    qosTestId = qosTestId,
                    language = language,
                    category = qoSCategory,
                    success = success,
                    description = qosGoal.resultDescription
                )
            )
        }
    }

    var qosResultDescMap: Map<QoSCategory, List<QosTestResult>> = EnumMap(QoSCategory::class.java)
    this.qosResultDetails.forEach { result ->
        val qoSCategory = QoSCategory.fromString(result.testType)
        var alreadySavedQosTest: MutableList<QosTestResult>? = qosResultDescMap[qoSCategory]?.toMutableList()
        if (alreadySavedQosTest == null) {
            alreadySavedQosTest = mutableListOf(result)
        } else {
            alreadySavedQosTest.add(result)
        }
        qosResultDescMap = qosResultDescMap.plus(Pair(qoSCategory, alreadySavedQosTest))
    }

    var categoryDescMap: Map<QoSCategory, QosTestCategoryDescription> = EnumMap(QoSCategory::class.java)
    this.qosResultDetailsTestDesc.forEach { category ->
        val qoSCategory = QoSCategory.fromString(category.testType)
        categoryDescMap = categoryDescMap.plus(Pair(qoSCategory, category))
    }

    val successfulTests: MutableList<QosTestResult> = ArrayList<QosTestResult>()
    categoryDescMap.keys.forEach { key ->

        val qosTestCategoryDescription = categoryDescMap[key]
        val resultList = qosResultDescMap[key]

        var successCount = 0
        var failureCount = 0
        var qosTestOrderNumber = 1

        resultList?.forEach { result ->
            if (result.failureCount > 0) {
                failureCount++
                results.add(
                    QosTestItemRecord(
                        testUUID = testUUID,
                        qosTestId = result.qosTestUid,
                        language = language,
                        category = key,
                        success = false,
                        testSummary = result.testSummary,
                        testDescription = result.testDescription,
                        testNumber = qosTestOrderNumber,
                        durationNanos = if (result.result.has("duration_ns") && result.result.get("duration_ns") != null) {
                            result.result.get("duration_ns").asLong
                        } else null,
                        startTimeNanos = if (result.result.has("start_time_ns") && result.result.get("start_time_ns") != null) {
                            result.result.get("start_time_ns").asLong
                        } else null
                    )
                )
                qosTestOrderNumber++
            } else {
                successfulTests.add(result)
                successCount++
            }
        }

        successfulTests.forEach { result ->
            results.add(
                QosTestItemRecord(
                    testUUID = testUUID,
                    qosTestId = result.qosTestUid,
                    language = language,
                    category = key,
                    success = true,
                    testSummary = result.testSummary,
                    testDescription = result.testDescription,
                    testNumber = qosTestOrderNumber,
                    durationNanos = if (result.result.has("duration_ns") && result.result.get("duration_ns") != null) {
                        result.result.get("duration_ns").asLong
                    } else null,
                    startTimeNanos = if (result.result.has("start_time_ns") && result.result.get("start_time_ns") != null) {
                        result.result.get("start_time_ns").asLong
                    } else null
                )
            )
            qosTestOrderNumber++
        }
        successfulTests.clear()

        if (failureCount != successCount && qosTestCategoryDescription != null) {
            categories.add(
                QosCategoryRecord(
                    testUUID = testUUID,
                    category = key,
                    language = language,
                    categoryDescription = qosTestCategoryDescription.descLocalized,
                    categoryName = qosTestCategoryDescription.nameLocalized,
                    failedCount = failureCount,
                    successCount = successCount
                )
            )
        }
    }
    return Triple(categories, results, goals)
}

inline fun <reified T> MapFiltersResponse.toMap(): Map<MapFilterType, List<T>> {
    val result = hashMapOf<MapFilterType, List<T>>()
    all.find { it.options.all { it is T } }?.let { result.put(MapFilterType.ALL, it.options as List<T>) }
    mobile.find { it.options.all { it is T } }?.let { result.put(MapFilterType.MOBILE, it.options as List<T>) }
    wifi.find { it.options.all { it is T } }?.let { result.put(MapFilterType.WLAN, it.options as List<T>) }
    browser.find { it.options.all { it is T } }?.let { result.put(MapFilterType.BROWSER, it.options as List<T>) }
    return result
}

inline fun <reified T> MapFiltersResponse.getTypeTitle(): String {
    all.find { it.options.all { it is T } }?.let { return it.title }
    mobile.find { it.options.all { it is T } }?.let { return it.title }
    wifi.find { it.options.all { it is T } }?.let { return it.title }
    browser.find { it.options.all { it is T } }?.let { return it.title }
    return ""
}

fun MapFilterObjectResponse.toSubtypesMap(types: MutableMap<String, MapFilterType>): Map<MapFilterType, List<MapTypeOptionsResponse>> {
    val result = hashMapOf<MapFilterType, List<MapTypeOptionsResponse>>()

    mapTypes.forEach {
        result[types.getValue(it.title)] = it.options
    }

    return result
}

fun SignalMeasurementRequestResponse.toModel(measurementId: String) = SignalMeasurementInfo(
    measurementId = measurementId,
    uuid = testUUID,
    clientRemoteIp = clientRemoteIp,
    resultUrl = resultUrl,
    provider = provider
)

private fun List<Any?>.extractFromList(position: Int): Any? = try {
    this[position]
} catch (ex: java.lang.IndexOutOfBoundsException) {
    null
}