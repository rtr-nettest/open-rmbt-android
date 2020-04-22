package at.specure.data

import at.rmbt.client.control.CapabilitiesBody
import at.rmbt.client.control.CellInfoBody
import at.rmbt.client.control.CellLocationBody
import at.rmbt.client.control.ClassificationBody
import at.rmbt.client.control.IpRequestBody
import at.rmbt.client.control.NetworkEventBody
import at.rmbt.client.control.PermissionStatusBody
import at.rmbt.client.control.PingBody
import at.rmbt.client.control.QoSBody
import at.rmbt.client.control.QoSResultBody
import at.rmbt.client.control.RadioInfoBody
import at.rmbt.client.control.SettingsRequestBody
import at.rmbt.client.control.SignalBody
import at.rmbt.client.control.SignalItemBody
import at.rmbt.client.control.SignalMeasurementChunkBody
import at.rmbt.client.control.SignalMeasurementLocationBody
import at.rmbt.client.control.SignalMeasurementRequestBody
import at.rmbt.client.control.SpeedBody
import at.rmbt.client.control.TestLocationBody
import at.rmbt.client.control.TestResultBody
import at.rtr.rmbt.util.BandCalculationUtil
import at.specure.config.Config
import at.specure.data.entity.CapabilitiesRecord
import at.specure.data.entity.CellInfoRecord
import at.specure.data.entity.CellLocationRecord
import at.specure.data.entity.ConnectivityStateRecord
import at.specure.data.entity.GeoLocationRecord
import at.specure.data.entity.PermissionStatusRecord
import at.specure.data.entity.PingRecord
import at.specure.data.entity.QoSResultRecord
import at.specure.data.entity.SignalMeasurementChunk
import at.specure.data.entity.SignalMeasurementRecord
import at.specure.data.entity.SignalRecord
import at.specure.data.entity.SpeedRecord
import at.specure.data.entity.TestRecord
import at.specure.data.entity.TestTelephonyRecord
import at.specure.data.entity.TestWlanRecord
import at.specure.info.TransportType
import at.specure.info.network.MobileNetworkType
import at.specure.info.strength.SignalStrengthInfo
import at.specure.info.strength.SignalStrengthInfoGsm
import at.specure.info.strength.SignalStrengthInfoLte
import at.specure.info.strength.SignalStrengthInfoWiFi
import at.specure.location.LocationInfo
import at.specure.test.DeviceInfo
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import java.util.UUID

fun DeviceInfo.toSettingsRequest(clientUUID: ClientUUID, config: Config, tac: TermsAndConditions) = SettingsRequestBody(
    type = clientType,
    name = clientName,
    language = language,
    platform = platform,
    osVersion = osVersion,
    apiLevel = apiLevel,
    device = device,
    model = model,
    product = product,
    timezone = timezone,
    softwareVersionName = softwareVersionName,
    softwareVersionCode = softwareVersionCode.toString(),
    softwareRevision = softwareRevision,
    versionName = clientVersionName,
    versionCode = clientVersionCode.toString(),
    uuid = clientUUID.value ?: "",
    userServerSelectionEnabled = config.expertModeEnabled,
    tacVersion = tac.tacVersion ?: 0,
    tacAccepted = tac.tacAccepted,
    capabilities = config.toCapabilitiesBody()
)

fun DeviceInfo.toIpRequest(clientUUID: String?, location: LocationInfo?, signalStrengthInfo: SignalStrengthInfo?, capabilities: CapabilitiesBody) =
    IpRequestBody(
        platform = platform,
        osVersion = osVersion,
        apiLevel = apiLevel,
        device = device,
        model = model,
        product = product,
        language = language,
        timezone = timezone,
        softwareRevision = softwareRevision,
        softwareVersionCode = softwareVersionCode.toString(),
        softwareVersionName = softwareVersionName,
        type = clientType,
        location = location?.toRequest(),
        lastSignalItem = signalStrengthInfo?.toRequest(),
        capabilities = capabilities,
        uuid = clientUUID
    )

fun Config.toCapabilitiesBody() = CapabilitiesBody(
    classification = ClassificationBody(capabilitiesClassificationCount),
    qos = QoSBody(capabilitiesQosSupportsInfo),
    rmbtHttpStatus = capabilitiesRmbtHttp
)

fun SignalStrengthInfo.toRequest(): SignalItemBody? = when (this) {
    is SignalStrengthInfoWiFi -> SignalItemBody(
        time = timestampNanos,
        networkTypeId = transport.toRequestIntValue(null),
        wifiLinkSpeed = linkSpeed,
        wifiRssi = value
    )
    is SignalStrengthInfoLte -> SignalItemBody(
        time = timestampNanos,
        networkTypeId = transport.toRequestIntValue(MobileNetworkType.LTE),
        lteRsrp = rsrp,
        lteRsrq = rsrq,
        lteRssnr = rssnr,
        lteCqi = cqi
    )
    is SignalStrengthInfoGsm -> SignalItemBody(
        time = timestampNanos,
        networkTypeId = transport.toRequestIntValue(MobileNetworkType.GSM),
        gsmBitErrorRate = bitErrorRate
    )
    else -> null
}

fun LocationInfo.toRequest() = TestLocationBody(
    latitude = latitude,
    longitude = longitude,
    provider = provider,
    speed = speed,
    altitude = altitude,
    timeMillis = time,
    timeNanos = elapsedRealtimeNanos,
    age = ageNanos,
    accuracy = accuracy,
    mockLocation = locationIsMocked,
    satellites = satellites,
    bearing = bearing
)

fun TestRecord.toRequest(
    clientUUID: String,
    deviceInfo: DeviceInfo,
    telephonyInfo: TestTelephonyRecord?,
    wlanInfo: TestWlanRecord?,
    locations: List<GeoLocationRecord>,
    capabilities: CapabilitiesRecord,
    pingList: List<PingRecord>,
    cellInfoList: List<CellInfoRecord>,
    signalList: List<SignalRecord>,
    speedInfoList: List<SpeedRecord>,
    cellLocationList: List<CellLocationRecord>,
    permissions: List<PermissionStatusRecord>
): TestResultBody {

    val geoLocations: List<TestLocationBody>? = if (locations.isEmpty()) {
        null
    } else {
        locations.map { it.toRequest() }
    }

    val pings: List<PingBody>? = if (pingList.isNullOrEmpty()) {
        null
    } else {
        pingList.map { it.toRequest() }
    }

    var radioInfo: RadioInfoBody? = if (cellInfoList.isEmpty() && signalList.isEmpty()) {
        null
    } else {
        val cells: Map<String, CellInfoBody>? = if (cellInfoList.isEmpty()) {
            null
        } else {
            val map = mutableMapOf<String, CellInfoBody>()
            cellInfoList.forEach {
                map[it.uuid] = it.toRequest()
            }
            if (map.isEmpty()) null else map
        }

        val signals: List<SignalBody>? = if (signalList.isEmpty()) {
            null
        } else {
            val list = mutableListOf<SignalBody>()
            if (cells == null) {
                null
            } else {
                signalList.forEach {
                    val cell = cells[it.cellUuid]
                    if (cell != null) {
                        list.add(it.toRequest(cell.uuid, !cell.active))
                    }
                }
                if (list.isEmpty()) null else list
            }
        }

        RadioInfoBody(cells?.entries?.map { it.value }, signals)
    }

    if (radioInfo?.cells.isNullOrEmpty() && radioInfo?.signals.isNullOrEmpty()) {
        radioInfo = null
    }

    val speedDetail: List<SpeedBody>? = if (speedInfoList.isEmpty()) {
        null
    } else {
        speedInfoList.map { it.toRequest() }
    }

    val cellLocations: List<CellLocationBody>? = if (cellLocationList.isEmpty()) {
        null
    } else {
        cellLocationList.map { it.toRequest() }
    }

    val permissionStatuses: List<PermissionStatusBody>? = if (permissions.isEmpty()) {
        null
    } else {
        permissions.map { it.toRequest() }
    }

    return TestResultBody(
        platform = deviceInfo.platform,
        clientUUID = clientUUID,
        clientName = deviceInfo.clientName,
        clientVersion = deviceInfo.rmbtClientVersion,
        clientLanguage = deviceInfo.language,
        timeMillis = testTimeMillis,
        token = token,
        portRemote = portRemote,
        bytesDownloaded = bytesDownloaded,
        bytesUploaded = bytesUploaded,
        totalBytesDownloaded = totalBytesDownloaded,
        totalBytesUploaded = totalBytesUploaded,
        encryptionType = encryption,
        clientPublicIp = clientPublicIp,
        serverPublicIp = serverPublicIp,
        downloadDurationNanos = downloadDurationNanos,
        uploadDurationNanos = uploadDurationNanos,
        threadCount = threadCount,
        downloadSpeedKbs = downloadSpeedKps,
        uploadSpeedKbs = uploadSpeedKps,
        shortestPingNanos = shortestPingNanos,
        downloadedBytesOnInterface = downloadedBytesOnInterface,
        uploadedBytesOnInterface = uploadedBytesOnInterface,
        downloadedBytesOnDownloadInterface = downloadedBytesOnDownloadInterface,
        uploadedBytesOnDownloadInterface = uploadedBytesOnDownloadInterface,
        downloadedBytesOnUploadInterface = downloadedBytesOnUploadInterface,
        uploadedBytesOnUploadInterface = uploadedBytesOnUploadInterface,
        timeDownloadOffsetNanos = timeDownloadOffsetNanos,
        timeUploadOffsetNanos = timeUploadOffsetNanos,
        product = deviceInfo.product,
        osVersion = deviceInfo.osVersion,
        apiLevel = deviceInfo.apiLevel,
        device = deviceInfo.device,
        model = deviceInfo.model,
        clientSoftwareVersion = deviceInfo.clientVersionName,
        networkType = (transportType?.toRequestIntValue(mobileNetworkType) ?: Int.MAX_VALUE).toString(),
        geoLocations = geoLocations,
        capabilities = capabilities.toRequest(),
        pings = pings,
        radioInfo = radioInfo,
        speedDetail = speedDetail,
        cellLocations = cellLocations,
        permissionStatuses = permissionStatuses,
        telephonyNetworkOperator = telephonyInfo?.networkOperator,
        telephonyNetworkIsRoaming = telephonyInfo?.networkIsRoaming?.toString(),
        telephonyNetworkCountry = telephonyInfo?.networkCountry,
        telephonyNetworkOperatorName = telephonyInfo?.networkOperatorName,
        telephonyNetworkSimOperatorName = telephonyInfo?.networkSimOperatorName,
        telephonyNetworkSimOperator = telephonyInfo?.networkSimOperator,
        telephonyPhoneType = telephonyInfo?.phoneType,
        telephonyDataState = telephonyInfo?.dataState,
        telephonyApn = telephonyInfo?.apn,
        telephonyNetworkSimCountry = telephonyInfo?.networkSimCountry,
        telephonySimCount = telephonyInfo?.simCount.toString(),
        telephonyHasDualSim = telephonyInfo?.hasDualSim,
        wifiSupplicantState = wlanInfo?.supplicantState,
        wifiSupplicantStateDetail = wlanInfo?.supplicantDetailedState,
        wifiSsid = wlanInfo?.ssid,
        wifiNetworkId = wlanInfo?.networkId,
        wifiBssid = wlanInfo?.bssid,
        submissionRetryCount = submissionRetryCount,
        testStatus = testFinishReason?.ordinal,
        lastClientStatus = lastClientStatus?.name,
        testErrorCause = testErrorCause,
        lastQoSStatus = lastQoSStatus?.name,
        testTag = testTag,
        developerModeEnabled = developerModeEnabled,
        loopModeEnabled = loopModeEnabled,
        userServerSelectionEnabled = serverSelectionEnabled
    )
}

fun GeoLocationRecord.toRequest() = TestLocationBody(
    latitude = latitude,
    longitude = longitude,
    provider = provider,
    speed = speed,
    altitude = altitude,
    timeMillis = timestampMillis,
    timeNanos = timeRelativeNanos,
    age = ageNanos,
    accuracy = accuracy,
    bearing = bearing,
    mockLocation = isMocked,
    satellites = satellitesCount
)

fun PingRecord.toRequest() = PingBody(
    differenceClient = value,
    differenceServer = valueServer,
    timeNanos = testTimeNanos
)

fun CapabilitiesRecord.toRequest() = CapabilitiesBody(
    classification = ClassificationBody(classificationCount),
    qos = QoSBody(qosSupportInfo),
    rmbtHttpStatus = rmbtHttpStatus
)

fun CellInfoRecord.toRequest() = CellInfoBody(
    active = isActive,
    areaCode = areaCode,
    uuid = UUID.randomUUID().toString(),
    channelNumber = if (transportType == TransportType.WIFI) frequency?.toInt() else channelNumber,
    locationId = locationId,
    mnc = mnc,
    mcc = mcc,
    primaryScramblingCode = primaryScramblingCode,
    technology = NetworkTypeCompat.fromType(transportType, cellTechnology).stringValue,
    registered = registered
)

fun SignalRecord.toRequest(cellUUID: String, ignoreNetworkId: Boolean) = SignalBody(
    cellUuid = cellUUID,
    networkTypeId = if (ignoreNetworkId) null else transportType.toRequestIntValue(mobileNetworkType),
    signal = signal.checkSignalValue(),
    bitErrorRate = bitErrorRate.checkSignalValue(),
    wifiLinkSpeed = wifiLinkSpeed.checkSignalValue(),
    lteRsrp = lteRsrp.checkSignalValue(),
    lteRsrq = lteRsrq.checkSignalValue(),
    lteRssnr = lteRssnr.checkSignalValue(),
    lteCqi = lteCqi.checkSignalValue(),
    timingAdvance = timingAdvance.checkSignalValue(),
    timeNanos = timeNanos,
    timeLastNanos = timeNanosLast,
    nrCsiRsrp = nrCsiRsrp,
    nrCsiRsrq = nrCsiRsrq,
    nrCsiSinr = nrSsSinr,
    nrSsRsrp = nrSsRsrp,
    nrSsRsrq = nrSsRsrq,
    nrSsSinr = nrSsSinr
)

private fun Int?.checkSignalValue(): Int? = if (this == null || this == Int.MAX_VALUE || this == Int.MAX_VALUE) {
    null
} else {
    this
}

fun SpeedRecord.toRequest() = SpeedBody(
    direction = if (isUpload) "upload" else "download",
    thread = threadId,
    timeNanos = timestampNanos,
    bytes = bytes
)

fun CellLocationRecord.toRequest() = CellLocationBody(
    timeMillis = timestampMillis,
    timeNanos = timestampNanos,
    locationId = locationId,
    areaCode = areaCode,
    primaryScramblingCode = scramblingCode
)

fun PermissionStatusRecord.toRequest() = PermissionStatusBody(
    permission = permissionName,
    status = status
)

fun TransportType.toRequestIntValue(mobileNetworkType: MobileNetworkType?): Int {
    return when (this) {
        TransportType.CELLULAR -> mobileNetworkType?.intValue ?: Int.MAX_VALUE
        TransportType.BLUETOOTH -> NetworkTypeCompat.TYPE_BLUETOOTH_VALUE
        TransportType.ETHERNET -> NetworkTypeCompat.TYPE_ETHERNET_VALUE
        TransportType.WIFI -> NetworkTypeCompat.TYPE_WIFI_VALUE
        else -> Int.MAX_VALUE
    }
}

fun QoSResultRecord.toRequest(clientUUID: String, deviceInfo: DeviceInfo): QoSResultBody {

    val parser = JsonParser()
    val qosResult = parser.parse(results.toString()) as JsonArray

    return QoSResultBody(
        clientUUID = clientUUID,
        clientName = deviceInfo.clientName,
        clientVersion = deviceInfo.rmbtClientVersion,
        clientLanguage = deviceInfo.language,
        qosResult = qosResult,
        testToken = testToken,
        timeMillis = timeMillis
    )
}

fun SignalMeasurementRecord.toRequest(clientUUID: String, deviceInfo: DeviceInfo) = SignalMeasurementRequestBody(
    platform = deviceInfo.platform,
    softwareVersionCode = deviceInfo.softwareVersionCode,
    softwareRevision = deviceInfo.softwareRevision,
    softwareVersion = deviceInfo.softwareRevision,
    time = startTimeMillis,
    timezone = deviceInfo.timezone,
    clientUUID = clientUUID,
    location = location?.toRequest()
)

fun DeviceInfo.Location.toRequest() = SignalMeasurementLocationBody(
    lat = lat,
    long = long,
    provider = provider,
    speed = speed,
    bearing = bearing,
    time = time,
    age = age,
    accuracy = accuracy,
    mock_location = mock_location,
    altitude = altitude,
    satellites = satellites
)

fun SignalMeasurementRecord.toRequest(
    measurementInfoUUID: String?,
    clientUUID: String,
    chunk: SignalMeasurementChunk,
    deviceInfo: DeviceInfo,
    telephonyInfo: TestTelephonyRecord?,
    wlanInfo: TestWlanRecord?,
    locations: List<GeoLocationRecord>,
    capabilities: CapabilitiesRecord,
    cellInfoList: List<CellInfoRecord>,
    signalList: List<SignalRecord>,
    permissions: List<PermissionStatusRecord>,
    networkEvents: List<NetworkEventBody>?,
    cellLocationList: List<CellLocationRecord>
): SignalMeasurementChunkBody {

    val geoLocations: List<TestLocationBody>? = if (locations.isEmpty()) {
        null
    } else {
        locations.map { it.toRequest() }
    }

    var radioInfo: RadioInfoBody? = if (cellInfoList.isEmpty() && signalList.isEmpty()) {
        null
    } else {
        val cells: Map<String, CellInfoBody>? = if (cellInfoList.isEmpty()) {
            null
        } else {
            val map = mutableMapOf<String, CellInfoBody>()
            cellInfoList.forEach {
                map[it.uuid] = it.toRequest()
            }
            if (map.isEmpty()) null else map
        }

        val signals: List<SignalBody>? = if (signalList.isEmpty()) {
            null
        } else {
            val list = mutableListOf<SignalBody>()
            if (cells == null) {
                null
            } else {
                signalList.forEach {
                    val cell = cells[it.cellUuid]
                    if (cell != null) {
                        list.add(it.toRequest(cell.uuid, false))
                    }
                }
                if (list.isEmpty()) null else list
            }
        }

        RadioInfoBody(cells?.entries?.map { it.value }, signals)
    }

    if (radioInfo?.cells.isNullOrEmpty() && radioInfo?.signals.isNullOrEmpty()) {
        radioInfo = null
    }

    val permissionStatuses: List<PermissionStatusBody>? = if (permissions.isEmpty()) {
        null
    } else {
        permissions.map { it.toRequest() }
    }

    val cellLocations: List<CellLocationBody>? = if (cellLocationList.isEmpty()) {
        null
    } else {
        cellLocationList.map { it.toRequest() }
    }

    return SignalMeasurementChunkBody(
        uuid = measurementInfoUUID,
        platform = deviceInfo.platform,
        clientUUID = clientUUID,
        clientVersion = deviceInfo.rmbtClientVersion,
        clientLanguage = deviceInfo.language,
        product = deviceInfo.product,
        osVersion = deviceInfo.osVersion,
        apiLevel = deviceInfo.apiLevel,
        device = deviceInfo.device,
        model = deviceInfo.model,
        timezone = deviceInfo.timezone,
        clientSoftwareVersion = deviceInfo.clientVersionName,
        networkType = (transportType?.toRequestIntValue(mobileNetworkType) ?: Int.MAX_VALUE).toString(),
        geoLocations = geoLocations,
        capabilities = capabilities.toRequest(),
        radioInfo = radioInfo,
        permissionStatuses = permissionStatuses,
        telephonyNetworkOperator = telephonyInfo?.networkOperator,
        telephonyNetworkIsRoaming = telephonyInfo?.networkIsRoaming?.toString(),
        telephonyNetworkCountry = telephonyInfo?.networkCountry,
        telephonyNetworkOperatorName = telephonyInfo?.networkOperatorName,
        telephonyNetworkSimOperatorName = telephonyInfo?.networkSimOperatorName,
        telephonyNetworkSimOperator = telephonyInfo?.networkSimOperator,
        telephonyPhoneType = telephonyInfo?.phoneType,
        telephonyDataState = telephonyInfo?.dataState,
        telephonyAPN = telephonyInfo?.apn,
        telephonyNetworkSimCountry = telephonyInfo?.networkSimCountry,
        wifiSupplicantState = wlanInfo?.supplicantState,
        wifiSupplicantStateDetail = wlanInfo?.supplicantDetailedState,
        wifiSSID = wlanInfo?.ssid,
        wifiNetworkId = wlanInfo?.networkId,
        wifiBSSID = wlanInfo?.bssid,
        submissionRetryCount = chunk.submissionRetryCount,
        testStatus = chunk.state.ordinal,
        testErrorCause = chunk.testErrorCause,
        sequenceNumber = chunk.sequenceNumber,
        testStartTimeNanos = chunk.startTimeNanos,
        networkEvents = networkEvents,
        cellLocations = cellLocations
    )
}

fun List<ConnectivityStateRecord>.toRequest(): List<NetworkEventBody>? {
    return if (isEmpty()) {
        null
    } else {
        map {
            NetworkEventBody(
                eventType = it.state.name,
                eventMessage = it.message,
                timeNanos = it.timeNanos
            )
        }
    }
}