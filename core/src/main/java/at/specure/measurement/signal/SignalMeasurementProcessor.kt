package at.specure.measurement.signal

import android.annotation.SuppressLint
import android.content.Context
import android.os.Binder
import android.os.Handler
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import at.specure.config.Config
import at.specure.data.entity.ConnectivityStateRecord
import at.specure.data.entity.SignalMeasurementChunk
import at.specure.data.entity.SignalMeasurementRecord
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.data.repository.TestDataRepository
import at.specure.info.TransportType
import at.specure.info.cell.CellInfoWatcher
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.cell.mccCompat
import at.specure.info.cell.mncCompat
import at.specure.info.connectivity.ConnectivityStateBundle
import at.specure.info.connectivity.ConnectivityWatcher
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
import at.specure.location.LocationProviderState
import at.specure.location.LocationProviderStateLiveData
import at.specure.location.LocationWatcher
import at.specure.location.cell.CellLocationInfo
import at.specure.location.cell.CellLocationLiveData
import at.specure.location.cell.CellLocationWatcher
import at.specure.test.toDeviceInfoLocation
import at.specure.util.isReadPhoneStatePermitted
import timber.log.Timber
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_SIGNAL_COUNT_PER_CHUNK = 25
private const val MAX_SIGNAL_UPTIME_PER_CHUNK_MIN = 10L

@Singleton
class SignalMeasurementProcessor @Inject constructor(
    private val context: Context,
    private val repository: TestDataRepository,
    private val locationInfoLiveData: LocationInfoLiveData,
    private val locationWatcher: LocationWatcher,
    private val signalStrengthLiveData: SignalStrengthLiveData,
    private val signalStrengthWatcher: SignalStrengthWatcher,
    private val activeNetworkLiveData: ActiveNetworkLiveData,
    private val activeNetworkWatcher: ActiveNetworkWatcher,
    private val cellInfoWatcher: CellInfoWatcher,
    private val config: Config,
    private val cellLocationLiveData: CellLocationLiveData,
    private val cellLocationWatcher: CellLocationWatcher,
    private val telephonyManager: TelephonyManager,
    private val subscriptionManager: SubscriptionManager,
    private val wifiInfoWatcher: WifiInfoWatcher,
    private val locationStateLiveData: LocationProviderStateLiveData,
    private val signalRepository: SignalMeasurementRepository,
    private val connectivityWatcher: ConnectivityWatcher
) : Binder(), SignalMeasurementProducer {

    private var _isActive = false
    private var _isPaused = false
    private val _activeStateLiveData = MutableLiveData<Boolean>()
    private val _pausedStateLiveData = MutableLiveData<Boolean>()

    private var networkInfo: NetworkInfo? = null
    private var record: SignalMeasurementRecord? = null
    private var chunk: SignalMeasurementChunk? = null

    private var chunkDataSize = 0
    private var chunkCountDownRunner = Runnable {
        Timber.i("Chunk countdown timer reached")
        commitChunkData()
        createNewChunk()
    }
    private var chunkCountDownHandler = Handler()

    private var locationInfo: LocationInfo? = null
    private var signalStrengthInfo: SignalStrengthInfo? = null
    private var cellLocation: CellLocationInfo? = null

    override val isActive: Boolean
        get() = _isActive

    override val isPaused: Boolean
        get() = _isPaused

    override val activeStateLiveData: LiveData<Boolean>
        get() = _activeStateLiveData

    override val pausedStateLiveData: LiveData<Boolean>
        get() = _pausedStateLiveData

    override fun startMeasurement() {
        Timber.w("startMeasurement")
        _isActive = true
        _activeStateLiveData.postValue(_isActive)
        _pausedStateLiveData.postValue(_isPaused)

        if (!isPaused) {
            handleNewNetwork(activeNetworkWatcher.currentNetworkInfo)
        }
    }

    override fun stopMeasurement() {
        Timber.w("stopMeasurement")

        chunk?.state = SignalMeasurementState.SUCCESS
        commitChunkData()

        _isActive = false
        _isPaused = false
        networkInfo = null
        record = null
        chunk = null
        _activeStateLiveData.postValue(_isActive)
        _pausedStateLiveData.postValue(_isPaused)
    }

    override fun pauseMeasurement() {
        Timber.w("pauseMeasurement")
        _isPaused = true
        _pausedStateLiveData.postValue(_isPaused)
    }

    override fun resumeMeasurement() {
        Timber.w("resumeMeasurement")

        _isPaused = false
        _pausedStateLiveData.postValue(_isPaused)
        if (isActive) {
            handleNewNetwork(activeNetworkWatcher.currentNetworkInfo)
        }
    }

    fun bind(owner: LifecycleOwner) {
        activeNetworkLiveData.observe(owner, Observer {
            if (isActive && !isPaused) {
                handleNewNetwork(it)
            }
        })

        if (locationStateLiveData.value == LocationProviderState.ENABLED) {
            locationInfo = locationWatcher.getLatestLocationInfo()
        }
        locationInfoLiveData.observe(owner, Observer { info ->
            if (locationStateLiveData.value == LocationProviderState.ENABLED) {
                locationInfo = info
                if (isActive && !isPaused) {
                    saveLocationInfo()
                }
            }
        })

        signalStrengthInfo = signalStrengthWatcher.lastSignalStrength
        signalStrengthLiveData.observe(owner, Observer { info ->
            signalStrengthInfo = info
            if (isActive && !isPaused) {
                saveSignalStrengthInfo()
            }
        })

        cellLocation = cellLocationWatcher.latestLocation
        cellLocationLiveData.observe(owner, Observer {
            cellLocation = it
            if (isActive && !isPaused) {
                saveCellLocation()
            }
        })

        connectivityWatcher.connectivityStateLiveData.observe(owner, Observer { state ->
            state?.let {
                if (isActive) {
                    saveConnectivityState(state)
                }
            }
        })
    }

    private fun handleNewNetwork(newInfo: NetworkInfo?) {
        val currentInfo = networkInfo
        when {
            newInfo == null && currentInfo != null -> {
                Timber.i("Network become unavailable")
                commitChunkData()
                networkInfo = null
                record = null
            }
            newInfo != null && currentInfo == null -> {
                Timber.i("Network appeared")
                networkInfo = newInfo
                createNewRecord(newInfo)
            }
            newInfo != null && currentInfo != null && currentInfo.cellUUID != newInfo.cellUUID -> {
                Timber.i("Network changed")
                networkInfo = newInfo
                commitChunkData()
                createNewRecord(newInfo)
            }
            else -> {
                Timber.i("New network other case -> new: ${newInfo?.cellUUID} old ${currentInfo?.cellUUID}")
            }
        }
    }

    private fun createNewRecord(networkInfo: NetworkInfo) {
        record = SignalMeasurementRecord(
            networkUUID = networkInfo.cellUUID,
            transportType = networkInfo.type,
            location = locationInfo.toDeviceInfoLocation()
        ).also {
            signalRepository.saveAndRegisterRecord(it)
        }
        chunk = null
        createNewChunk()
    }

    private fun commitChunkData() {
        chunk?.let {
            Timber.i("Commit chunk data")
            signalRepository.sendMeasurementChunk(it)
        }
    }

    private fun createNewChunk() {
        record?.let {
            chunk = SignalMeasurementChunk(
                measurementId = it.id,
                sequenceNumber = chunk?.sequenceNumber?.inc() ?: 0,
                state = SignalMeasurementState.RUNNING,
                startTimeNanos = System.nanoTime(),
                submissionRetryCount = 0
            ).also { chunk ->
                signalRepository.saveMeasurementChunk(chunk)
                chunkDataSize = 0
                scheduleCountDownTimer()
                Timber.i("New chunk created")
            }

            saveWlanInfo()
            saveCellInfo()
            saveTelephonyInfo()
            saveSignalStrengthInfo()
            saveCellLocation()
            saveLocationInfo()
            saveCapabilities()
        }
    }

    private fun scheduleCountDownTimer() {
        chunkCountDownHandler.removeCallbacks(chunkCountDownRunner)
        chunkCountDownHandler.postDelayed(chunkCountDownRunner, TimeUnit.MINUTES.toMillis(MAX_SIGNAL_UPTIME_PER_CHUNK_MIN))
    }

    private fun saveConnectivityState(bundle: ConnectivityStateBundle) {
        chunk?.let {
            repository.saveConnectivityState(
                ConnectivityStateRecord(
                    uuid = it.id,
                    state = bundle.state,
                    message = bundle.message,
                    timeNanos = bundle.timeNanos
                )
            )
        }
    }

    private fun saveCellInfo() {
        val uuid = chunk?.id
        val info = networkInfo
        if (uuid != null && info != null) {
            val infoList: List<NetworkInfo> = when (info) {
                is WifiNetworkInfo -> listOf(info)
                is CellNetworkInfo -> cellInfoWatcher.allCellInfo
                else -> throw IllegalArgumentException("Unknown cell info ${info.javaClass.simpleName}")
            }

            repository.saveCellInfo(uuid, infoList, record?.startTimeMillis ?: 0)
        }
    }

    private fun saveCapabilities() {
        chunk?.id?.let {
            repository.saveCapabilities(
                it,
                config.capabilitiesRmbtHttp,
                config.capabilitiesQosSupportsInfo,
                config.capabilitiesClassificationCount
            )
        }
    }

    private fun saveLocationInfo() {
        val uuid = chunk?.id
        val location = locationInfo
        if (uuid != null && location != null && locationStateLiveData.value == LocationProviderState.ENABLED) {
            repository.saveGeoLocation(uuid, location)
        }
    }

    private fun saveCellLocation() {
        val uuid = chunk?.id
        val location = cellLocation
        if (uuid != null && location != null) {
            repository.saveCellLocation(uuid, location)
        }
    }

    private fun saveSignalStrengthInfo() {
        val uuid = chunk?.id
        val info = signalStrengthInfo
        if (uuid != null && info != null) {
            val cellUUID = networkInfo?.cellUUID ?: ""
            var mobileNetworkType: MobileNetworkType? = null
            if (networkInfo != null && networkInfo is CellNetworkInfo) {
                mobileNetworkType = (networkInfo as CellNetworkInfo).networkType
            }
            repository.saveSignalStrength(uuid, cellUUID, mobileNetworkType, info, record?.startTimeMillis ?: 0)

            chunkDataSize++
            if (chunkDataSize >= MAX_SIGNAL_COUNT_PER_CHUNK) {
                Timber.v("Chunk max size reached: $chunkDataSize")
                commitChunkData()
                createNewChunk()
            }
        }
    }

    private fun saveWlanInfo() {
        val wifiInfo = wifiInfoWatcher.activeWifiInfo
        if (wifiInfo?.ssid != null && wifiInfo.bssid != null) {
            record?.let {
                repository.saveWlanInfo(it.id, wifiInfo)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun saveTelephonyInfo() {
        val info = networkInfo
        if (info != null && info is CellNetworkInfo) {
            record?.let {
                it.mobileNetworkType = info.networkType
                signalRepository.updateSignalMeasurementRecord(it)
            }
        }

        val type = activeNetworkWatcher.currentNetworkInfo?.type
        val isDualSim = telephonyManager.phoneCount > 1
        val isDualByMobile = type == TransportType.CELLULAR && isDualSim

        chunk?.id?.let {
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