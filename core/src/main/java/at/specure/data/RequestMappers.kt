package at.specure.data

import at.rmbt.client.control.CapabilitiesBody
import at.rmbt.client.control.CellInfoBody
import at.rmbt.client.control.CellLocationBody
import at.rmbt.client.control.ClassificationBody
import at.rmbt.client.control.IpRequestBody
import at.rmbt.client.control.PermissionStatusBody
import at.rmbt.client.control.PingBody
import at.rmbt.client.control.QoSBody
import at.rmbt.client.control.RadioInfoBody
import at.rmbt.client.control.SettingsRequestBody
import at.rmbt.client.control.SignalBody
import at.rmbt.client.control.SignalItemBody
import at.rmbt.client.control.SpeedBody
import at.rmbt.client.control.TestLocationBody
import at.rmbt.client.control.TestResultBody
import at.specure.config.Config
import at.specure.data.entity.CapabilitiesRecord
import at.specure.data.entity.CellInfoRecord
import at.specure.data.entity.CellLocationRecord
import at.specure.data.entity.GeoLocationRecord
import at.specure.data.entity.PermissionStatusRecord
import at.specure.data.entity.PingRecord
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
    userServerSelectionEnabled = config.userServerSelectionEnabled,
    tacVersion = 6, // TODO replace with tac.tacVersion ?: 0,
    tacAccepted = true, // TODO should be replaced with tac.tacAccepted,
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

    val radioInfo: RadioInfoBody? = if (cellInfoList.isEmpty() && signalList.isEmpty()) {
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
                        list.add(it.toRequest(cell.uuid))
                    }
                }
                if (list.isEmpty()) null else list
            }
        }

        RadioInfoBody(cells?.entries?.map { it.value }, signals)
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
        telephonySimCount = telephonyInfo?.simCount,
        telephonyHasDualSim = telephonyInfo?.hasDualSim,
        wifiSupplicantState = wlanInfo?.supplicantState,
        wifiSupplicantStateDetail = wlanInfo?.supplicantDetailedState,
        wifiSsid = wlanInfo?.ssid,
        wifiNetworkId = wlanInfo?.networkId,
        wifiBssid = wlanInfo?.bssid
    )
}

fun GeoLocationRecord.toRequest() = TestLocationBody(
    latitude = latitude,
    longitude = longitude,
    provider = provider,
    speed = speed,
    altitude = altitude,
    timeMillis = time,
    timeNanos = timeCorrectionNanos,
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
    channelNumber = channelNumber,
    locationId = locationId,
    mnc = mnc,
    mcc = mcc,
    primaryScramblingCode = primaryScramblingCode,
    technology = NetworkTypeCompat.fromType(transportType, cellTechnology).stringValue,
    registered = registered
)

fun SignalRecord.toRequest(cellUUID: String) = SignalBody(
    cellUuid = cellUUID,
    networkTypeId = transportType.toRequestIntValue(mobileNetworkType),
    signal = signal,
    bitErrorRate = bitErrorRate,
    wifiLinkSpeed = wifiLinkSpeed,
    lteRsrp = lteRsrp,
    lteRsrq = lteRsrq,
    lteRssnr = lteRssnr,
    lteCqi = lteCqi,
    timingAdvance = timingAdvance,
    timeNanos = timeNanos,
    timeLastNanos = timeNanosLast
)

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
        TransportType.BLUETOOTH -> 107
        TransportType.ETHERNET -> 106
        TransportType.WIFI -> 99
        else -> Int.MAX_VALUE
    }
}

fun TransportType.toRequestValue(mobileNetworkType: MobileNetworkType?): String {
    return when (toRequestIntValue(mobileNetworkType)) {
        1 -> "2G (GSM)"
        2 -> "2G (EDGE)"
        3 -> "3G (UMTS)"
        4 -> "2G (CDMA)"
        5 -> "2G (EVDO_0)"
        6 -> "2G (EVDO_A)"
        7 -> "2G (1xRTT)"
        8 -> "3G (HSDPA)"
        9 -> "3G (HSUPA)"
        10 -> "3G (HSPA)"
        11 -> "2G (IDEN)"
        12 -> "2G (EVDO_B)"
        13 -> "4G (LTE)"
        14 -> "2G (EHRPD)"
        15 -> "3G (HSPA+)"
        19 -> "4G (LTE CA)"
        20 -> "5G (NR)"
        97 -> "CLI"
        98 -> "BROWSER"
        99 -> "WLAN"
        101 -> "2G/3G"
        102 -> "3G/4G"
        103 -> "2G/4G"
        104 -> "2G/3G/4G"
        105 -> "MOBILE"
        106 -> "Ethernet"
        107 -> "Bluetooth"
        else -> "UNKNOWN"
    }
}