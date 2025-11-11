package at.specure.data

import android.telephony.TelephonyManager
import at.rmbt.client.control.CapabilitiesBody
import at.rmbt.client.control.CellInfoBody
import at.rmbt.client.control.CellLocationBody
import at.rmbt.client.control.ClassificationBody
import at.rmbt.client.control.CoverageRequestBody
import at.rmbt.client.control.CoverageResultRequestBody
import at.rmbt.client.control.FenceBody
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
import at.rmbt.client.control.SimpleLocationBody
import at.rmbt.client.control.SpeedBody
import at.rmbt.client.control.TestLocationBody
import at.rmbt.client.control.TestResultBody
import at.rmbt.client.control.data.SignalMeasurementType
import at.specure.config.Config
import at.specure.data.RequestFilters.Companion.createRadioInfoBody
import at.specure.data.RequestFilters.Companion.removeOldRedundantSignalValuesWithNegativeTimestamp
import at.specure.data.entity.CapabilitiesRecord
import at.specure.data.entity.CellInfoRecord
import at.specure.data.entity.CellLocationRecord
import at.specure.data.entity.ConnectivityStateRecord
import at.specure.data.entity.GeoLocationRecord
import at.specure.data.entity.PermissionStatusRecord
import at.specure.data.entity.PingRecord
import at.specure.data.entity.QoSResultRecord
import at.specure.data.entity.SignalMeasurementChunk
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.SignalMeasurementRecord
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.data.entity.SignalRecord
import at.specure.data.entity.SpeedRecord
import at.specure.data.entity.TestRecord
import at.specure.data.entity.TestTelephonyRecord
import at.specure.data.entity.TestWlanRecord
import at.specure.data.entity.VoipTestResultRecord
import at.specure.data.entity.getJitter
import at.specure.data.entity.getPacketLoss
import at.specure.info.TransportType
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NRConnectionState
import at.specure.info.strength.SignalStrengthInfo
import at.specure.info.strength.SignalStrengthInfoGsm
import at.specure.info.strength.SignalStrengthInfoLte
import at.specure.info.strength.SignalStrengthInfoWiFi
import at.specure.location.LocationInfo
import at.specure.test.DeviceInfo
import at.specure.util.exception.DataMissingException
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import java.util.UUID

const val UNKNOWN = "UNKNOWN"

fun DeviceInfo.toSettingsRequest(clientUUID: ClientUUID, clientUUIDLegacy: ClientUUIDLegacy, config: Config, tac: TermsAndConditions) =
    SettingsRequestBody(
        type = clientType,
        name = clientName,
        language = language ?: UNKNOWN,
        platform = platform,
        osVersion = osVersion,
        apiLevel = apiLevel,
        device = device ?: UNKNOWN,
        model = model ?: UNKNOWN,
        product = product ?: UNKNOWN,
        timezone = timezone ?: UNKNOWN,
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
        device = device ?: UNKNOWN,
        model = model ?: UNKNOWN,
        product = product ?: UNKNOWN,
        language = language ?: UNKNOWN,
        timezone = timezone ?: UNKNOWN,
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
    permissions: List<PermissionStatusRecord>,
    voipTestResultRecord: VoipTestResultRecord?
): TestResultBody {

    if (this == null) throw DataMissingException("TestRecord is null for request")
    if (clientUUID == null) throw DataMissingException("ClientUUID is null for request")
    if (deviceInfo == null) throw DataMissingException("DeviceInfo is null for request")
    val geoLocations: List<TestLocationBody>? = mapLocationsToRequest(locations)
    val pings: List<PingBody>? = mapPingsToRequest(pingList)
    val pair = mapRadioInfoToRequest(cellInfoList, signalList)
    val radioInfo: RadioInfoBody? = pair.first
    val dualSimDetectionMethod = pair.second
    val speedDetail: List<SpeedBody>? = mapSpeedsToRequest(speedInfoList)
    val cellLocations: List<CellLocationBody>? = mapCellLocationsToRequest(cellLocationList)
    val permissionStatuses: List<PermissionStatusBody>? = mapPermissionStatusesToRequest(permissions, this.networkCapabilitiesRaw)

    var telephonyNRConnectionState: String? = null
    val best5GTechnologyAchieved = extractBest5gTechnologyUsedDuringMeasurement(signalList)

    best5GTechnologyAchieved?.let {
        telephonyNRConnectionState = it.stringValue
    }

    return TestResultBody(
        platform = deviceInfo.platform,
        clientUUID = clientUUID,
        clientName = deviceInfo.clientName,
        clientVersion = clientVersion,
        clientLanguage = deviceInfo.language ?: UNKNOWN,
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
        product = deviceInfo.product ?: UNKNOWN,
        osVersion = deviceInfo.osVersion,
        apiLevel = deviceInfo.apiLevel,
        device = deviceInfo.device ?: UNKNOWN,
        model = deviceInfo.model ?: UNKNOWN,
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
        telephonyNRConnection = telephonyNRConnectionState,
        packetLoss = voipTestResultRecord?.getPacketLoss(),
        jitterMillis = voipTestResultRecord?.getJitter(),
        testUUID = uuid
    )
}

private fun extractBest5gTechnologyUsedDuringMeasurement(
    signalList: List<SignalRecord>,
): NRConnectionState? {
    var best5GTechnologyAchieved: NRConnectionState? = null
    val signals5G = signalList.filter {
        it.mobileNetworkType == MobileNetworkType.NR_AVAILABLE || it.mobileNetworkType == MobileNetworkType.NR_NSA || it.mobileNetworkType == MobileNetworkType.NR_SA
    }

    signals5G.forEach {
        val current5GTechnology =
            when {
                (it.mobileNetworkType == MobileNetworkType.NR_SA && it.nrConnectionState != NRConnectionState.NSA) -> NRConnectionState.SA
                (it.mobileNetworkType == MobileNetworkType.NR_SA && it.nrConnectionState == NRConnectionState.NSA) -> NRConnectionState.NSA
                (it.mobileNetworkType == MobileNetworkType.NR_NSA) -> NRConnectionState.NSA
                (it.mobileNetworkType == MobileNetworkType.NR_AVAILABLE) -> NRConnectionState.AVAILABLE
                else -> null
            }
        when (current5GTechnology) {
            NRConnectionState.SA -> if (best5GTechnologyAchieved == null || best5GTechnologyAchieved == NRConnectionState.NSA || best5GTechnologyAchieved == NRConnectionState.AVAILABLE) {
                best5GTechnologyAchieved = NRConnectionState.SA
            }

            NRConnectionState.NSA -> if (best5GTechnologyAchieved == null || best5GTechnologyAchieved == NRConnectionState.AVAILABLE) {
                best5GTechnologyAchieved = NRConnectionState.NSA
            }

            NRConnectionState.AVAILABLE -> if (best5GTechnologyAchieved == null) {
                best5GTechnologyAchieved = NRConnectionState.AVAILABLE
            }

            else -> {
                best5GTechnologyAchieved = null
            }
        }
    }

    return best5GTechnologyAchieved
}

private fun mapSpeedsToRequest(speedInfoList: List<SpeedRecord>) =
    speedInfoList.takeIf { it.isNotEmpty() }?.map { it.toRequest() }

private fun mapRadioInfoToRequest(
    cellInfoList: List<CellInfoRecord>,
    signalList: List<SignalRecord>,
): Pair<RadioInfoBody?, String?> {
    var dualSimDetectionMethod: String? = null
    val radioInfoBody = if (cellInfoList.isEmpty() && signalList.isEmpty()) {
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
                        list.add(it.toRequest(cell.uuid, null))
                    }
                }
                if (list.isEmpty()) null else list
            }
        }

        signals = removeOldRedundantSignalValuesWithNegativeTimestamp(signals)?.distinctBy {
            listOf(
                it.timeNanos,
                it.networkTypeId
            )
        }

        val radioInfoBody = RadioInfoBody(cells?.entries?.map { it.value }, signals)
        getRadioInfoIntegrityCheckedOrNull(radioInfoBody)
    }

    return Pair(radioInfoBody, dualSimDetectionMethod)
}

private fun mapPingsToRequest(pingList: List<PingRecord>) =
    if (pingList.isEmpty()) {
        null
    } else {
        pingList.map { it.toRequest() }
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
    registered = registered,
    isPrimaryDataSubscription = isPrimaryDataSubscription,
    cellState = cellState
)

fun SignalRecord.toRequest(cellUUID: String, signalMeasurementStartTimeNs: Long?) = SignalBody(
    cellUuid = cellUUID,
    networkTypeId = transportType.toRequestIntValue(mobileNetworkType),
    signal = if (lteRsrp == null && nrCsiRsrp == null && nrSsRsrp == null) signal.checkSignalValue() else null,
    bitErrorRate = bitErrorRate.checkSignalValue(),
    wifiLinkSpeed = wifiLinkSpeed.checkSignalValue(),
    lteRsrp = lteRsrp.checkSignalValue(),
    lteRsrq = lteRsrq.checkSignalValue(),
    lteRssnr = lteRssnr.checkSignalValue(),
    lteCqi = lteCqi.checkSignalValue(),
    timingAdvance = timingAdvance.checkSignalValue(),
    timeNanos = if (signalMeasurementStartTimeNs != null) timeNanos.minus(signalMeasurementStartTimeNs) else timeNanos,
    timeLastNanos = timeNanosLast,
    nrCsiRsrp = nrCsiRsrp,
    nrCsiRsrq = nrCsiRsrq,
    nrCsiSinr = nrCsiSinr,
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
        TransportType.CELLULAR -> mobileNetworkType?.intValue ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
        TransportType.BLUETOOTH -> NetworkTypeCompat.TYPE_BLUETOOTH_VALUE
        TransportType.ETHERNET -> NetworkTypeCompat.TYPE_ETHERNET_VALUE
        TransportType.WIFI -> NetworkTypeCompat.TYPE_WIFI_VALUE
        TransportType.VPN -> NetworkTypeCompat.TYPE_VPN_VALUE
        else -> Int.MAX_VALUE
    }
}

fun QoSResultRecord.toRequest(clientUUID: String, deviceInfo: DeviceInfo, clientVersion: String): QoSResultBody {

    val qosResult = JsonParser.parseString(results.toString()) as JsonArray

    return QoSResultBody(
        clientUUID = clientUUID,
        clientName = deviceInfo.clientName,
        clientVersion = clientVersion,
        clientLanguage = deviceInfo.language ?: UNKNOWN,
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
    timezone = deviceInfo.timezone ?: UNKNOWN,
    clientUUID = clientUUID,
    measurementTypeFlag = signalMeasurementType.signalTypeName,
    location = location?.toRequest()
)

fun CoverageMeasurementSession.toCoverageRequest(clientUUID: String, deviceInfo: DeviceInfo, config: Config) = CoverageRequestBody(
    clientUUID = clientUUID,
    platform = deviceInfo.platform,
    softwareVersionCode = deviceInfo.softwareVersionCode,
    softwareRevision = deviceInfo.softwareRevision,
    softwareVersion = deviceInfo.softwareRevision,
    timezone = deviceInfo.timezone ?: UNKNOWN,
    time = startTimeMillis,
    measurementTypeFlag = SignalMeasurementType.DEDICATED.signalTypeName,
    languageCode = deviceInfo.language,
    model = deviceInfo.model,
    osVersion = deviceInfo.osVersion,
    clientName = deviceInfo.clientName,
    device = deviceInfo.device,
    signal = true,
    capabilities = CapabilitiesBody(
        classification = ClassificationBody(config.capabilitiesClassificationCount),
        qos = QoSBody(config.capabilitiesQosSupportsInfo),
        rmbtHttpStatus = config.capabilitiesRmbtHttp
    ),
    version = deviceInfo.clientVersionName,
)

fun CoverageMeasurementSession.toCoverageResultRequest(
    clientUUID: String,
    deviceInfo: DeviceInfo,
    config: Config,
    fences: List<CoverageMeasurementFenceRecord>,
) = CoverageResultRequestBody(
    clientUUID = clientUUID,
    testUUID = this.serverSessionId ?: throw DataMissingException("Missing signal measurement server session ID"),
    platform = deviceInfo.platform,
    softwareVersion = deviceInfo.softwareVersionName,
    timezone = deviceInfo.timezone ?: UNKNOWN,
    model = deviceInfo.model ?: UNKNOWN,
    osVersion = deviceInfo.osVersion,
    device = deviceInfo.device ?: UNKNOWN,
    capabilities = CapabilitiesBody(
        classification = ClassificationBody(config.capabilitiesClassificationCount),
        qos = QoSBody(config.capabilitiesQosSupportsInfo),
        rmbtHttpStatus = config.capabilitiesRmbtHttp
    ),
    clientVersion = deviceInfo.clientVersionName,
    clientLanguage = deviceInfo.language ?: UNKNOWN,
    product = deviceInfo.product ?: UNKNOWN,
    apiLevel = deviceInfo.apiLevel,
    softwareVersionCode = deviceInfo.softwareVersionCode.toString(),
    fences = fences.toRequest(startTimeMillis),
)

fun List<CoverageMeasurementFenceRecord>.toRequest(measurementStartMillis: Long): List<FenceBody> {
    return this.map { it.toRequest(measurementStartMillis) }
}

fun CoverageMeasurementFenceRecord.toRequest(measurementStartMillis: Long): FenceBody = FenceBody(
    centerLocation = this.location?.toSimpleLocation(),
    networkTechnologyId = this.technologyId ?: MobileNetworkType.UNKNOWN.intValue,
    networkTechnologyName = MobileNetworkType.fromValue(this.technologyId ?: MobileNetworkType.UNKNOWN.intValue).displayName,
    offsetMillis = this.entryTimestampMillis - measurementStartMillis,
    durationMillis = this.leaveTimestampMillis - this.entryTimestampMillis,
    fenceRadiusMeters = this.radiusMeters,
    averagePingMillis = this.avgPingMillis?.toInt(),
    timestampMicroseconds = this.entryTimestampMillis
)

fun DeviceInfo.Location.toSimpleLocation(): SimpleLocationBody = SimpleLocationBody(
    latitude = lat,
    longitude = long,
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

    val geoLocations: List<TestLocationBody>? = mapLocationsToRequest(locations)

    var radioInfo: RadioInfoBody? = createRadioInfoBody(cellInfoList, signalList, chunk)

    radioInfo = getRadioInfoIntegrityCheckedOrNull(radioInfo)

    val permissionStatuses: List<PermissionStatusBody>? = mapPermissionStatusesToRequest(permissions, this.rawCapabilitiesRecord)

    val cellLocations: List<CellLocationBody>? = mapCellLocationsToRequest(cellLocationList)

    return SignalMeasurementChunkBody(
        uuid = measurementInfoUUID,
        platform = deviceInfo.platform,
        clientUUID = clientUUID,
        clientLanguage = deviceInfo.language ?: UNKNOWN,
        product = deviceInfo.product ?: UNKNOWN,
        osVersion = deviceInfo.osVersion,
        apiLevel = deviceInfo.apiLevel,
        device = deviceInfo.device ?: UNKNOWN,
        model = deviceInfo.model ?: UNKNOWN,
        timezone = deviceInfo.timezone ?: UNKNOWN,
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

private fun mapPermissionStatusesToRequest(permissions: List<PermissionStatusRecord>, rawCapabilitiesRecord: String?): List<PermissionStatusBody> {
    val permissionStatuses = permissions.takeIf { it.isNotEmpty() }?.map { it.toRequest() }
    return addDebugCapabilities(permissionStatuses, rawCapabilitiesRecord)
}

private fun mapCellLocationsToRequest(cellLocationList: List<CellLocationRecord>) =
    cellLocationList.takeIf { it.isNotEmpty() }?.map { it.toRequest() }

private fun getRadioInfoIntegrityCheckedOrNull(radioInfo: RadioInfoBody?): RadioInfoBody? {
    var radioInfoLocal = radioInfo
    if (radioInfoLocal?.cells.isNullOrEmpty() && radioInfoLocal?.signals.isNullOrEmpty()) {
        radioInfoLocal = null
    }
    return radioInfoLocal
}

private fun mapLocationsToRequest(locations: List<GeoLocationRecord>) =
    locations.takeIf { it.isNotEmpty() }?.map { it.toRequest() }

private fun addDebugCapabilities(permissionStatuses: List<PermissionStatusBody>?, capabilitiesString: String?): List<PermissionStatusBody> {
    val permissionStatusesWithDebugInfo = permissionStatuses?.toMutableList() ?: mutableListOf()
    permissionStatusesWithDebugInfo.add(PermissionStatusBody("current network capabilities $capabilitiesString", false))
    return permissionStatusesWithDebugInfo
}

fun convertLocalNetworkTypeToServerType(transportType: TransportType?, mobileNetworkType: MobileNetworkType?): String {
    val primaryNetworkType = (transportType?.toRequestIntValue(mobileNetworkType) ?: Int.MAX_VALUE)
    val secondaryNetworkType = mobileNetworkType?.intValue
    if (primaryNetworkType == Int.MAX_VALUE) {
        if (secondaryNetworkType != null) {
            return secondaryNetworkType.toString()
        }
    } else {
        return primaryNetworkType.toString()
    }
    return primaryNetworkType.toString()
}

fun List<ConnectivityStateRecord>.toRequest(): List<NetworkEventBody>? {
    return this.takeIf { isEmpty() }
        ?.map {
            NetworkEventBody(
                eventType = it.state.name,
                eventMessage = it.message,
                timeNanos = it.timeNanos
            )
        }
}
