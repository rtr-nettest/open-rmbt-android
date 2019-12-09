package at.specure.data

import at.rmbt.client.control.CapabilitiesBody
import at.rmbt.client.control.CellInfoBody
import at.rmbt.client.control.CellLocationBody
import at.rmbt.client.control.ClassificationBody
import at.rmbt.client.control.PermissionStatusBody
import at.rmbt.client.control.PingBody
import at.rmbt.client.control.QoSBody
import at.rmbt.client.control.RadioInfoBody
import at.rmbt.client.control.SignalBody
import at.rmbt.client.control.SpeedBody
import at.rmbt.client.control.TestLocationBody
import at.rmbt.client.control.TestResultBody
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
import at.specure.test.DeviceInfo

fun TestRecord.toRequest(
    clientUUID: String,
    uuid: String,
    deviceInfo: DeviceInfo,
    telephonyInfo: TestTelephonyRecord?,
    wlanInfo: TestWlanRecord?,
    locations: List<GeoLocationRecord>,
    capabilities: CapabilitiesRecord,
    pings: List<PingRecord>,
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

    val _pings: List<PingBody>? = if (pings.isNullOrEmpty()) {
        null
    } else {
        pings.map { it.toRequest() }
    }

    val radioInfo: RadioInfoBody? = if (cellInfoList.isEmpty() && signalList.isEmpty()) {
        null
    } else {
        val cells: List<CellInfoBody>? = if (cellInfoList.isEmpty()) {
            null
        } else {
            cellInfoList.map { it.toRequest() }
        }

        val signals: List<SignalBody>? = if (signalList.isEmpty()) {
            null
        } else {
            signalList.map { it.toRequest() }
        }

        RadioInfoBody(cells, signals)
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
        uuid = uuid,
        clientName = deviceInfo.clientName,
        clientVersion = deviceInfo.clientVersion,
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
        clientSoftwareVersion = deviceInfo.clientVersion,
        networkType = transportType?.toRequestIntValue(mobileNetworkType) ?: Int.MAX_VALUE,
        geoLocations = geoLocations,
        capabilities = capabilities.toRequest(),
        pings = _pings,
        radioInfo = radioInfo,
        speedDetail = speedDetail,
        cellLocations = cellLocations,
        permissionStatuses = permissionStatuses,
        telephonyNetworkOperator = telephonyInfo?.networkOperator,
        telephonyNetworkIsRoaming = telephonyInfo?.networkIsRoaming,
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
    uuid = uuid,
    channelNumber = channelNumber,
    locationId = locationId,
    mnc = mnc,
    mcc = mcc,
    primaryScramblingCode = primaryScramblingCode,
    technology = cellTechnology?.displayName,
    registered = registered
)

fun SignalRecord.toRequest() = SignalBody(
    cellUuid = cellUuid,
    networkTypeId = transportType.toRequestIntValue(mobileNetworkType),
    signal = signal,
    bitErrorRate = bitErrorRate,
    wifiLinkSpeed = wifiLinkSpeed,
    lteRsrp = lteRsrp,
    lteRsrq = lteRsrq,
    lteRssnr = lteRssnr,
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