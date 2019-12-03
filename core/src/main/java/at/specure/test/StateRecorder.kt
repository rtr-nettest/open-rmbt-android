package at.specure.test

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import at.specure.config.Config
import at.specure.info.cell.CellInfoWatcher
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.ActiveNetworkLiveData
import at.specure.info.network.ActiveNetworkWatcher
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NetworkInfo
import at.specure.info.network.WifiNetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.info.strength.SignalStrengthLiveData
import at.specure.info.strength.SignalStrengthWatcher
import at.specure.location.LocationInfo
import at.specure.location.LocationInfoLiveData
import at.specure.location.LocationWatcher
import at.specure.repository.TestDataRepository
import at.specure.util.hasPermission
import at.specure.util.permission.PermissionsWatcher
import javax.inject.Inject

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
    private val config: Config
) {

    private var testUUID: String? = null
    private var testStartTimeNanos: Long = 0L

    private var _locationInfo: LocationInfo? = null
    private var signalStrengthInfo: SignalStrengthInfo? = null
    private var networkInfo: NetworkInfo? = null

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
        })
    }

    fun start(testUUID: String, testStartTimeNanos: Long) {
        this.testUUID = testUUID
        this.testStartTimeNanos = testStartTimeNanos
        saveLocationInfo()
        saveSignalStrengthInfo()
        saveCellInfo()
        saveCapabilities()
        savePermissionsStatus()
    }

    fun finish() {
        // TODO finish
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

            repository.saveCellInfo(uuid, infoList)
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
}