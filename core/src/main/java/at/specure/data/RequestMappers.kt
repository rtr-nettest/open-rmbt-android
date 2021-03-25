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
import at.specure.info.network.NRConnectionState
import at.specure.info.strength.SignalStrengthInfo
import at.specure.info.strength.SignalStrengthInfoGsm
import at.specure.info.strength.SignalStrengthInfoLte
import at.specure.info.strength.SignalStrengthInfoWiFi
import at.specure.location.LocationInfo
import at.specure.test.DeviceInfo
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import timber.log.Timber
import java.util.UUID
import kotlin.math.abs

fun DeviceInfo.toSettingsRequest(clientUUID: ClientUUID, clientUUIDLegacy: ClientUUIDLegacy, config: Config, tac: TermsAndConditions) =
    SettingsRequestBody(
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
        capabilities = config.toCapabilitiesBody(),
        uuidLegacy = clientUUIDLegacy.value
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
    var dualSimDetectionMethod: String? = null
    var radioInfo: RadioInfoBody? = if (cellInfoList.isEmpty() && signalList.isEmpty()) {
        null
    } else {
        val cells: Map<String, CellInfoBody>? = if (cellInfoList.isEmpty()) {
            null
        } else {
            val map = mutableMapOf<String, CellInfoBody>()
            cellInfoList.forEach {
                map[it.uuid] = it.toRequest()
                if (it.isActive) {
                    dualSimDetectionMethod = it.dualSimDetectionMethod
                }
                Timber.d("valid cell to send: ${it.uuid} mapped to ${map[it.uuid]?.uuid}")
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
                        list.add(it.toRequest(cell.uuid, !cell.active, null))
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

    var telephonyNRConnectionState: String? = null

    val signals5G = signalList.filter {
        it.mobileNetworkType == MobileNetworkType.NR_AVAILABLE || it.mobileNetworkType == MobileNetworkType.NR_NSA || it.mobileNetworkType == MobileNetworkType.NR
    }

    if (signals5G.isNotEmpty()) {
        telephonyNRConnectionState = when (signals5G[0].mobileNetworkType) {
            MobileNetworkType.NR -> NRConnectionState.SA.toString()
            MobileNetworkType.NR_NSA -> NRConnectionState.NSA.toString()
            MobileNetworkType.NR_AVAILABLE -> NRConnectionState.AVAILABLE.toString()
            else -> null
        }
    }

    return TestResultBody(
        platform = deviceInfo.platform,
        clientUUID = clientUUID,
        clientName = deviceInfo.clientName,
        clientVersion = clientVersion,
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
        networkType = convertLocalNetworkTypeToServerType(transportType, mobileNetworkType),
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
        dualSimDetectionMethod = dualSimDetectionMethod,
        wifiSupplicantState = wlanInfo?.supplicantState,
        wifiSupplicantStateDetail = wlanInfo?.supplicantDetailedState,
        wifiSsid = wlanInfo?.ssid,
        wifiNetworkId = wlanInfo?.networkId,
        wifiBssid = wlanInfo?.bssid,
        submissionRetryCount = submissionRetryCount,
        testStatus = testFinishReason?.ordinal.toString(),
        lastClientStatus = lastClientStatus?.name,
        testErrorCause = testErrorCause,
        lastQoSStatus = lastQoSStatus?.name,
        testTag = testTag,
        developerModeEnabled = developerModeEnabled,
        loopModeEnabled = loopModeEnabled,
        userServerSelectionEnabled = serverSelectionEnabled,
        telephonyNRConnection = telephonyNRConnectionState
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
    technology = NetworkTypeCompat.fromType(transportType, cellTechnology)?.stringValue,
    registered = registered
)

fun SignalRecord.toRequest(cellUUID: String, ignoreNetworkId: Boolean, signalMeasurementStartTimeNs: Long?) = SignalBody(
    cellUuid = cellUUID,
    networkTypeId = if (ignoreNetworkId) null else if (transportType.toRequestIntValue(mobileNetworkType) == MobileNetworkType.NR.intValue) {
        if (nrConnectionState == NRConnectionState.NSA) {
            MobileNetworkType.NR_NSA.intValue
        } else {
            transportType.toRequestIntValue(mobileNetworkType)
        }
    } else {
        transportType.toRequestIntValue(mobileNetworkType)
    },
    signal = signal.checkSignalValue(),
    bitErrorRate = bitErrorRate.checkSignalValue(),
    wifiLinkSpeed = wifiLinkSpeed.checkSignalValue(),
    lteRsrp = lteRsrp.checkSignalValue(),
    lteRsrq = lteRsrq.checkSignalValue(),
    lteRssnr = lteRssnr.checkSignalValue(),
    lteCqi = lteCqi.checkSignalValue(),
    timingAdvance = timingAdvance.checkSignalValue(),
    timeNanos = if (signalMeasurementStartTimeNs != null) timeNanos else timeNanos.minus(signalMeasurementStartTimeNs ?: 0),
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
    measurementTypeFlag = signalMeasurementType.signalTypeName,
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

        var signals: List<SignalBody>? = if (signalList.isEmpty()) {
            null
        } else {
            val list = mutableListOf<SignalBody>()
            if (cells == null) {
                null
            } else {
                signalList.forEach {
                    val cell = cells[it.cellUuid]
                    if (cell != null) {
                        list.add(it.toRequest(cell.uuid, false, null))
                    } else {
                        list.add(it.toRequest("", false, null))
                    }
                }
                if (list.isEmpty()) null else list
            }
        }
        Timber.i("Old list size: ${signals?.size}")
        // remove last signal from previous chunk
        if (signals != null && (signals.size > 1) && (chunk.sequenceNumber > 0)) {
            signals = signals.filterIndexed { index, it ->
                if (index == signals?.lastIndex) {
                    true
                } else {
                    val newValueUnder60s = abs(signals!![index + 1].timeNanos) - abs(it.timeNanos) <= 60000000000
                    if (newValueUnder60s) {
                        true
                    } else {
                        Timber.i("Filtered out: $it")
                        false
                    }
                }
            }
            // remove previous last entry which is added by getLastSignal
            signals = signals.filterIndexed { index, _ ->
                Timber.i("index checking list size: $index")
                index != 0
            }
        }
        Timber.i("New list size: ${signals?.size} + last time: ")

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
        networkType = convertLocalNetworkTypeToServerType(transportType, mobileNetworkType),
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
        testStatus = chunk.state.ordinal.toString(),
        testErrorCause = chunk.testErrorCause,
        sequenceNumber = chunk.sequenceNumber,
        testStartTimeNanos = chunk.startTimeNanos - startTimeNanos,
        networkEvents = networkEvents,
        cellLocations = cellLocations
    )
}

fun convertLocalNetworkTypeToServerType(transportType: TransportType?, mobileNetworkType: MobileNetworkType?): String {
    return (transportType?.toRequestIntValue(mobileNetworkType) ?: Int.MAX_VALUE).toString()
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