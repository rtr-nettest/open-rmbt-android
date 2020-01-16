package at.specure.test

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import at.rmbt.client.control.data.TestFinishReason
import at.rtr.rmbt.client.RMBTClientCallback
import at.rtr.rmbt.client.TotalTestResult
import at.rtr.rmbt.client.helper.TestStatus
import at.rtr.rmbt.client.v2.task.result.QoSResultCollector
import at.rtr.rmbt.client.v2.task.service.TestMeasurement.TrafficDirection
import at.specure.config.Config
import at.specure.data.entity.TestRecord
import at.specure.data.repository.TestDataRepository
import at.specure.info.TransportType
import at.specure.info.cell.CellInfoWatcher
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.cell.mccCompat
import at.specure.info.cell.mncCompat
import at.specure.info.network.ActiveNetworkLiveData
import at.specure.info.network.ActiveNetworkWatcher
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NetworkInfo
import at.specure.info.network.WifiNetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.info.strength.SignalStrengthLiveData
import at.specure.info.strength.SignalStrengthWatcher
import at.specure.info.wifi.WifiInfoWatcher
import at.specure.location.LocationInfo
import at.specure.location.LocationInfoLiveData
import at.specure.location.LocationWatcher
import at.specure.location.cell.CellLocationInfo
import at.specure.location.cell.CellLocationLiveData
import at.specure.location.cell.CellLocationWatcher
import at.specure.util.hasPermission
import at.specure.util.isReadPhoneStatePermitted
import at.specure.util.permission.PermissionsWatcher
import org.json.JSONArray
import timber.log.Timber
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.floor

class StateRecorder @Inject constructor(
    private val context: Context,
    private val repository: TestDataRepository,
    private val locationInfoLiveData: LocationInfoLiveData,
    private val locationWatcher: LocationWatcher,
    private val signalStrengthLiveData: SignalStrengthLiveData,
    private val signalStrengthWatcher: SignalStrengthWatcher,
    private val activeNetworkLiveData: ActiveNetworkLiveData,
    private val activeNetworkWatcher: ActiveNetworkWatcher,
    private val cellInfoWatcher: CellInfoWatcher,
    private val permissionsWatcher: PermissionsWatcher,
    private val config: Config,
    private val cellLocationLiveData: CellLocationLiveData,
    private val cellLocationWatcher: CellLocationWatcher,
    private val telephonyManager: TelephonyManager,
    private val subscriptionManager: SubscriptionManager,
    private val wifiInfoWatcher: WifiInfoWatcher
) : RMBTClientCallback {
    private var testUUID: String? = null
    private var testToken: String? = null
    private var testStartTimeNanos: Long = 0L
    private var testRecord: TestRecord? = null

    private var _locationInfo: LocationInfo? = null
    private var signalStrengthInfo: SignalStrengthInfo? = null
    private var networkInfo: NetworkInfo? = null
    private var cellLocation: CellLocationInfo? = null

    var onReadyToSubmit: ((Boolean) -> Unit)? = null

    val locationInfo: LocationInfo?
        get() = _locationInfo

    /**
     * Associate current state collector object with service lifecycle
     */
    fun bind(lifecycle: LifecycleOwner) {

        _locationInfo = locationWatcher.getLatestLocationInfo()
        locationInfoLiveData.observe(lifecycle, Observer { info ->
            _locationInfo = info
            saveLocationInfo()
        })

        signalStrengthInfo = signalStrengthWatcher.lastSignalStrength
        signalStrengthLiveData.observe(lifecycle, Observer { info ->
            signalStrengthInfo = info
            saveSignalStrengthInfo()
        })

        networkInfo = activeNetworkWatcher.currentNetworkInfo
        activeNetworkLiveData.observe(lifecycle, Observer {
            networkInfo = it
            saveCellInfo()
            saveTelephonyInfo()
            saveWlanInfo()
        })

        cellLocation = cellLocationWatcher.latestLocation
        cellLocationLiveData.observe(lifecycle, Observer {
            cellLocation = it
            saveCellLocation()
        })
    }

    override fun onClientReady(testUUID: String, loopUUID: String?, testToken: String, testStartTimeNanos: Long, threadNumber: Int) {
        this.testUUID = testUUID
        this.testToken = testToken
        this.testStartTimeNanos = testStartTimeNanos
        saveTestInitialTestData(testUUID, loopUUID, testToken, testStartTimeNanos, threadNumber)
        saveLocationInfo()
        saveSignalStrengthInfo()
        saveCellInfo()
        saveCapabilities()
        savePermissionsStatus()
        saveCellLocation()
        saveTelephonyInfo()
        saveWlanInfo()
    }

    fun finish() {
        // TODO finish
        testUUID = null
        testToken = null
    }

    private fun saveTestInitialTestData(testUUID: String, loopUUID: String?, testToken: String, testStartTimeNanos: Long, threadNumber: Int) {
        Timber.d("testUUID $testUUID, loopUUId $loopUUID, testToken: $testToken, start: $testStartTimeNanos, threadNumber $threadNumber")
        testRecord = TestRecord(
            uuid = testUUID,
            loopUUID = loopUUID,
            token = testToken,
            testStartTimeMillis = TimeUnit.NANOSECONDS.toMillis(testStartTimeNanos),
            threadCount = threadNumber
        )
        repository.saveTest(testRecord!!)
    }

    private fun saveLocationInfo() {
        val uuid = testUUID
        val location = locationInfo
        if (uuid != null && location != null) {
            repository.saveGeoLocation(uuid, location)
        }
    }

    private fun saveSignalStrengthInfo() {
        val uuid = testUUID
        val info = signalStrengthInfo
        if (uuid != null && info != null) {
            val cellUUID = networkInfo?.cellUUID ?: ""
            var mobileNetworkType: MobileNetworkType? = null
            if (networkInfo != null && networkInfo is CellNetworkInfo) {
                mobileNetworkType = (networkInfo as CellNetworkInfo).networkType
            }
            repository.saveSignalStrength(uuid, cellUUID, mobileNetworkType, info, testStartTimeNanos)
        }
    }

    private fun saveCellInfo() {
        val uuid = testUUID
        val info = networkInfo
        if (uuid != null && info != null) {
            val infoList: List<NetworkInfo> = when (info) {
                is WifiNetworkInfo -> listOf(info)
                is CellNetworkInfo -> cellInfoWatcher.allCellInfo
                else -> throw IllegalArgumentException("Unknown cell info ${info.javaClass.simpleName}")
            }

            repository.saveCellInfo(uuid, infoList, testStartTimeNanos)
        }
    }

    private fun saveCapabilities() {
        val uuid = testUUID
        uuid?.let {
            repository.saveCapabilities(
                it,
                config.capabilitiesRmbtHttp,
                config.capabilitiesQosSupportsInfo,
                config.capabilitiesClassificationCount
            )
        }
    }

    private fun savePermissionsStatus() {
        val permissions = permissionsWatcher.allPermissions
        val uuid = testUUID
        uuid?.let {
            permissions.forEach { permission ->
                val permissionGranted = context.hasPermission(permission)
                repository.savePermissionStatus(it, permission, permissionGranted)
            }
        }
    }

    private fun saveCellLocation() {
        val uuid = testUUID
        val location = cellLocation
        if (uuid != null && location != null) {
            repository.saveCellLocation(uuid, location)
        }
    }

    @SuppressLint("MissingPermission")
    private fun saveTelephonyInfo() {
        val info = networkInfo
        if (info != null && info is CellNetworkInfo) {
            testRecord?.mobileNetworkType = info.networkType
        }

        val type = activeNetworkWatcher.currentNetworkInfo?.type
        val isDualSim = telephonyManager.phoneCount > 1
        val isDualByMobile = type == TransportType.CELLULAR && isDualSim

        testUUID?.let {
            var operatorName: String? = null
            var networkOperator: String? = null
            var networkCountry: String? = null
            val simCount: Int

            if (context.isReadPhoneStatePermitted() && isDualByMobile) {
                val subscription = subscriptionManager.activeSubscriptionInfoList.firstOrNull()
                simCount = if (subscription != null) subscriptionManager.activeSubscriptionInfoCount else 2
                subscription?.let {
                    operatorName = subscription.carrierName.toString()
                    val networkSimOperator = when {
                        subscription.mccCompat() == null -> null
                        subscription.mncCompat() == null -> null
                        else -> "${subscription.mccCompat()}-${DecimalFormat("00").format(subscription.mncCompat())}"
                    }
                    networkOperator = networkSimOperator
                    networkCountry = subscription.countryIso
                }
            } else {
                simCount = 1
                operatorName = telephonyManager.networkOperatorName
                networkOperator = telephonyManager.networkOperator.fixOperatorName()
                networkCountry = telephonyManager.networkCountryIso
            }

            val networkInfo = cellInfoWatcher.activeNetwork
            val simCountry = telephonyManager.simCountryIso.fixOperatorName()
            val simOperatorName = try { // hack for Motorola Defy (#594)
                telephonyManager.simOperatorName
            } catch (ex: SecurityException) {
                ex.printStackTrace()
                "s.exception"
            }
            val phoneType = telephonyManager.phoneType.toString()
            val dataState = try {
                telephonyManager.dataState.toString()
            } catch (ex: SecurityException) {
                ex.printStackTrace()
                "s.exception"
            }

            repository.saveTelephonyInfo(
                it,
                networkInfo,
                operatorName,
                networkOperator,
                networkCountry,
                simCountry,
                simOperatorName,
                phoneType,
                dataState,
                simCount
            )
        }
    }

    private fun saveWlanInfo() {
        val wifiInfo = wifiInfoWatcher.activeWifiInfo
        if (wifiInfo?.ssid != null && wifiInfo.bssid != null) {
            testUUID?.let {
                repository.saveWlanInfo(it, wifiInfo)
            }
        }
    }

    fun onDownloadSpeedChanged(progress: Int, speedBps: Long) {
        testUUID?.let {
            if (progress > -1) {
                repository.saveDownloadGraphItem(it, progress, speedBps)
            }
        }
    }

    fun onUploadSpeedChanged(progress: Int, speedBps: Long) {
        testUUID?.let {
            if (progress > -1) {
                repository.saveUploadGraphItem(it, progress, speedBps)
            }
        }
    }

    override fun onSpeedDataChanged(threadId: Int, bytes: Long, timestampNanos: Long, isUpload: Boolean) {
        testUUID?.let {
            repository.saveSpeedData(it, threadId, bytes, timestampNanos, isUpload)
        }
    }

    override fun onPingDataChanged(clientPing: Long, serverPing: Long, timeNs: Long) {
        testUUID?.let {
            repository.saveAllPingValues(it, clientPing, serverPing, timeNs)
        }
    }

    override fun onTestCompleted(result: TotalTestResult, waitQosResults: Boolean) {
        testRecord?.apply {
            portRemote = result.port_remote
            bytesDownloaded = result.bytes_download
            bytesUploaded = result.bytes_upload
            totalBytesDownloaded = result.totalDownBytes
            totalBytesUploaded = result.totalUpBytes
            encryption = result.encryption
            clientPublicIp = result.ip_local?.hostAddress
            serverPublicIp = result.ip_server?.hostAddress
            downloadDurationNanos = result.nsec_download
            uploadDurationNanos = result.nsec_upload
            downloadSpeedKps = floor(result.speed_download + 0.5).toLong()
            uploadSpeedKps = floor(result.speed_upload + 0.5).toLong()
            shortestPingNanos = result.ping_shortest
            downloadedBytesOnInterface = result.getTotalTrafficMeasurement(TrafficDirection.RX)
            uploadedBytesOnInterface = result.getTotalTrafficMeasurement(TrafficDirection.TX)
            downloadedBytesOnDownloadInterface = result.getTrafficByTestPart(TestStatus.DOWN, TrafficDirection.RX)
            uploadedBytesOnDownloadInterface = result.getTrafficByTestPart(TestStatus.DOWN, TrafficDirection.TX)
            downloadedBytesOnUploadInterface = result.getTrafficByTestPart(TestStatus.UP, TrafficDirection.RX)
            uploadedBytesOnUploadInterface = result.getTrafficByTestPart(TestStatus.UP, TrafficDirection.TX)

            val dlMeasurement = result.getTestMeasurementByTestPart(TestStatus.DOWN)
            dlMeasurement?.let {
                timeDownloadOffsetNanos = it.timeStampStart - testStartTimeNanos
            }

            val ulMeasurement = result.getTestMeasurementByTestPart(TestStatus.UP)
            ulMeasurement?.let {
                timeUploadOffsetNanos = it.timeStampStart - testStartTimeNanos
            }

            transportType = networkInfo?.type
            testTimeMillis = System.currentTimeMillis()

            testFinishReason = TestFinishReason.SUCCESS
        }

        testRecord?.let {
            repository.update(it) {
                if (!waitQosResults) {
                    onReadyToSubmit?.invoke(true)
                }
            }
        }

        if (!waitQosResults) {
            testUUID = null
            testToken = null
        }
    }

    override fun onQoSTestCompleted(qosResult: QoSResultCollector?) {
        val uuid = testUUID
        val token = testToken
        val data: JSONArray? = qosResult?.toJson()
        if (uuid != null && token != null && qosResult != null && data != null) {
            repository.saveQoSResults(uuid, token, data) {
                onReadyToSubmit?.invoke(true)
            }
        }
    }

    override fun onTestStatusUpdate(status: TestStatus?) {
        status?.let {
            testRecord?.also {
                it.status = status

                if (status != TestStatus.ERROR && status != TestStatus.ABORTED) {
                    it.lastClientStatus = status
                }
            }
        }
    }

    fun onUnsuccessTest(reason: TestFinishReason) {
        testRecord?.also {
            it.testFinishReason = reason

            repository.update(it) {
                onReadyToSubmit?.invoke(false)
            }
        }
    }

    fun setErrorCause(message: String) {
        testRecord?.testErrorCause = message
    }

    private fun String?.fixOperatorName(): String? {
        return if (this == null) {
            null
        } else if (length >= 5 && !contains("-")) {
            "${substring(0, 3)}-${substring(3)}"
        } else {
            this
        }
    }
}