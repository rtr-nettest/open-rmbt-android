package at.rtr.rmbt.android.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rmbt.util.exception.HandledException
import at.rmbt.util.io
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.ui.viewstate.MeasurementViewState
import at.rtr.rmbt.android.util.plusAssign
import at.rtr.rmbt.android.util.timeString
import at.rtr.rmbt.client.v2.task.result.QoSTestResultEnum
import at.specure.data.TermsAndConditions
import at.specure.data.entity.GraphItemRecord
import at.specure.data.entity.LoopModeRecord
import at.specure.data.repository.HistoryRepository
import at.specure.data.repository.TestDataRepository
import at.specure.info.connectivity.ConnectivityInfoLiveData
import at.specure.info.network.ActiveNetworkLiveData
import at.specure.info.strength.SignalStrengthLiveData
import at.specure.location.LocationState
import at.specure.location.LocationWatcher
import at.specure.measurement.MeasurementClient
import at.specure.measurement.MeasurementProducer
import at.specure.measurement.MeasurementService
import at.specure.measurement.MeasurementState
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import javax.inject.Inject

class MeasurementViewModel @Inject constructor(
    private val testDataRepository: TestDataRepository,
    private val historyRepository: HistoryRepository,
    private val locationWatcher: LocationWatcher,
    val signalStrengthLiveData: SignalStrengthLiveData,
    val activeNetworkLiveData: ActiveNetworkLiveData,
    val connectivityInfoLiveData: ConnectivityInfoLiveData,
    val config: AppConfig,
    private val tac: TermsAndConditions
) : BaseViewModel(), MeasurementClient {

    private val _measurementFinishLiveData = MutableLiveData<Boolean>()
    private val _resultWaitingToBeSentLiveData = MutableLiveData<Boolean>()
    private val _measurementCancelledLiveData = MutableLiveData<Boolean>()
    private val _isTestsRunningLiveData = MutableLiveData<Boolean>()
    private val _measurementErrorLiveData = MutableLiveData<Boolean>()
    private val _downloadGraphLiveData = MutableLiveData<List<GraphItemRecord>>()
    private val _uploadGraphLiveData = MutableLiveData<List<GraphItemRecord>>()
    private val _qosProgressLiveData = MutableLiveData<Map<QoSTestResultEnum, Int>>()
    private val _loopUUIDLiveData = MutableLiveData<String>()
    private val _timeToNextTestElapsedLiveData = MutableLiveData<String>()
    private val _timeProgressPercentsLiveData = MutableLiveData<Int>()

    private var producer: MeasurementProducer? = null

    val state = MeasurementViewState(config)

    var testUUID: String? = null
        private set

    val locationStateLiveData: LiveData<LocationState?>
        get() = locationWatcher.stateLiveData

    val loopUuidLiveData: LiveData<String?>
        get() = _loopUUIDLiveData

    val timeToNextTestElapsedLiveData: LiveData<String>
        get() = _timeToNextTestElapsedLiveData

    val timeProgressPercentsLiveData: LiveData<Int>
        get() = _timeProgressPercentsLiveData

    val resultWaitingToBeSentLiveData: LiveData<Boolean>
        get() = _resultWaitingToBeSentLiveData

    val measurementFinishLiveData: LiveData<Boolean>
        get() = _measurementFinishLiveData

    val measurementCancelledLiveData: LiveData<Boolean>
        get() = _measurementCancelledLiveData

    val isTestsRunningLiveData: LiveData<Boolean>
        get() = _isTestsRunningLiveData

    val measurementErrorLiveData: LiveData<Boolean>
        get() = _measurementErrorLiveData

    val downloadGraphSource: LiveData<List<GraphItemRecord>>
        get() = _downloadGraphLiveData

    val uploadGraphSource: LiveData<List<GraphItemRecord>>
        get() = _uploadGraphLiveData

    val qosProgressLiveData: LiveData<Map<QoSTestResultEnum, Int>>
        get() = _qosProgressLiveData

    val isTacAccepted: Boolean
        get() = tac.tacAccepted

    lateinit var loopProgressLiveData: LiveData<LoopModeRecord?>

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceDisconnected(componentName: ComponentName?) {
            producer?.removeClient(this@MeasurementViewModel)
            producer = null
            Timber.i("On service disconnected")
        }

        override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
            producer = binder as MeasurementProducer?
            val loopLocalUuid = producer?.loopLocalUUID
            testUUID = producer?.testUUID

            initializeLoopData(loopLocalUuid)
            Timber.d("Passed local loop UUID: $loopLocalUuid")
            producer?.let {
                it.addClient(this@MeasurementViewModel)

                with(state) {
                    measurementState.set(it.measurementState)
                    _loopUUIDLiveData.postValue(it.loopLocalUUID)
                    measurementProgress.set(it.measurementProgress)
                    pingNanos.set(it.pingNanos)
                    downloadSpeedBps.set(it.downloadSpeedBps)
                    uploadSpeedBps.set(it.uploadSpeedBps)
                    signalStrengthInfoResult.set(it.lastMeasurementSignalInfo)
                }
                Timber.d("Ping value from: ${it.pingNanos}")
            }
            Timber.d("On service connected:\n test running:  ${producer?.isTestsRunning} \n measurement state:  ${producer?.measurementState} \n loop state: ${producer?.loopModeState} \nloop local uuid: ${producer?.loopLocalUUID} \n")

            val finished = producer?.isTestsRunning != true
            Timber.d("FINISHED?: $finished")

            _isTestsRunningLiveData.postValue(!finished) // to notify new opened home activity
            _measurementFinishLiveData.postValue(finished) // to notify recreated measurement activity to show results
        }

        override fun onNullBinding(name: ComponentName?) {
            super.onNullBinding(name)
            Timber.d("Measurement binding null")
            _measurementFinishLiveData.postValue(true)
        }

        override fun onBindingDied(name: ComponentName?) {
            super.onBindingDied(name)
            Timber.d("Measurement binding died")
            _measurementFinishLiveData.postValue(true)
        }
    }

    init {
        addStateSaveHandler(state)
    }

    fun attach(context: Context) {
        val bound = context.bindService(MeasurementService.intent(context), serviceConnection, Context.BIND_AUTO_CREATE)
        Timber.d("Measurement binding success: $bound")
        if (!bound) {
            _measurementFinishLiveData.postValue(true)
        }
    }

    fun detach(context: Context) {
        producer?.removeClient(this)
        context.unbindService(serviceConnection)
    }

    override fun onProgressChanged(state: MeasurementState, progress: Int) {
        this.state.measurementState.set(state)
        this.state.measurementProgress.set(progress)
        if (config.loopModeEnabled) {
            this.state.signalStrengthInfoResult.set(producer?.lastMeasurementSignalInfo)
            if (state == MeasurementState.INIT) {
                _resultWaitingToBeSentLiveData.postValue(true)
//                loadMedianValues(this.state.loopModeRecord.get()?.uuid)
            } else {
                if (this.loopUuidLiveData.value != null && this.loopProgressLiveData.value != null && this.loopProgressLiveData.value?.testsPerformed!! > 0 && this.state.pingNanos.get() == 0L || this.state.pingNanosMedian.get() == 0L) {
//                    loadMedianValues(this.state.loopModeRecord.get()?.uuid)
                }
            }
        }
    }

    private fun loadMedianValues(loopUUID: String?) {
        loopUUID?.let { loopUuid ->
            io {
                historyRepository.loadLoopMedianValues(loopUuid).collect { medians ->
                    this@MeasurementViewModel.state.setMedianValues(medians)
                }
            }
        }
    }

    override fun onMeasurementError() {
        _measurementErrorLiveData.postValue(true)
        _resultWaitingToBeSentLiveData.postValue(false)
    }

    override fun onDownloadSpeedChanged(progress: Int, speedBps: Long) {
        state.downloadSpeedBps.set(speedBps)
        if (progress > -1) {
            state.measurementDownloadUploadProgress.set(progress)

            if (state.measurementState.get() == MeasurementState.DOWNLOAD) {
                _downloadGraphLiveData += GraphItemRecord(
                    testUUID = "",
                    progress = progress,
                    value = speedBps,
                    type = GraphItemRecord.GRAPH_ITEM_TYPE_DOWNLOAD
                )
            }
        }
    }

    override fun onUploadSpeedChanged(progress: Int, speedBps: Long) {
        state.uploadSpeedBps.set(speedBps)
        if (progress > -1) {
            state.measurementDownloadUploadProgress.set(progress)

            if (state.measurementState.get() == MeasurementState.UPLOAD) {
                _uploadGraphLiveData += GraphItemRecord(
                    testUUID = "",
                    progress = progress,
                    value = speedBps,
                    type = GraphItemRecord.GRAPH_ITEM_TYPE_UPLOAD
                )
            }
        }
    }

//    fun setMeasurementResultsShown() {
//        _measurementResultShownLiveData.value = true
//    }

    override fun onPingChanged(pingNanos: Long) {
//        _measurementResultShownLiveData.value = false
        Timber.i("Ping value from: $pingNanos")
        state.pingNanos.set(pingNanos)
    }

    override fun onJitterChanged(jitterNanos: Long) {
        Timber.i("JPL jitter value from: $jitterNanos")
        state.jitterNanos.set(jitterNanos)
    }

    override fun onPacketLossPercentChanged(packetLossPercent: Int) {
        Timber.i("JPL packetLoss value from: $packetLossPercent")
        state.packetLossPercent.set(packetLossPercent)
    }

    override fun isQoSEnabled(enabled: Boolean) {
        state.qosEnabled.set(enabled)
    }

    override fun onResultSubmitted() {
        Timber.d("Test Data sent")
        _resultWaitingToBeSentLiveData.postValue(false)
    }

    override fun onSubmitted() {
        Timber.d("Test Data sent")
        _measurementFinishLiveData.postValue(true)
        _resultWaitingToBeSentLiveData.postValue(false)
    }

    override fun onSubmissionError(exception: HandledException) {
        Timber.d("Test Data submission failed")
        _measurementFinishLiveData.postValue(false)
        _resultWaitingToBeSentLiveData.postValue(false)
    }

    override fun onLoopCountDownTimer(timePassedMillis: Long, timeTotalMillis: Long) {
        _timeToNextTestElapsedLiveData.postValue((timeTotalMillis - timePassedMillis).timeString())
        _timeProgressPercentsLiveData.postValue(((timePassedMillis * 100) / timeTotalMillis).toInt())
    }

    fun cancelMeasurement() {
        producer?.stopTests()
        _resultWaitingToBeSentLiveData.postValue(false)
    }

    override fun onMeasurementCancelled() {
        _measurementCancelledLiveData.postValue(false)
        _resultWaitingToBeSentLiveData.postValue(false)
    }

    override fun onClientReady(testUUID: String, loopLocalUUID: String?) {

        this.testUUID = testUUID
        _loopUUIDLiveData.postValue(loopLocalUUID)
        initializeLoopData(loopLocalUUID)

        Timber.d("loopUUID: $loopLocalUUID")

        testDataRepository.getDownloadGraphItemsLiveData(testUUID) {
            _downloadGraphLiveData.postValue(it)
        }

        testDataRepository.getUploadGraphItemsLiveData(testUUID) {
            _uploadGraphLiveData.postValue(it)
        }
    }

    private fun initializeLoopData(loopLocalUUID: String?) {
        _resultWaitingToBeSentLiveData.postValue(false)
        if (loopLocalUUID != null) {
            Timber.d("Loop UUID not null")
            loopProgressLiveData = testDataRepository.getLoopModeByLocal(loopLocalUUID)
            val loopUUID = this.state.loopModeRecord.get()?.uuid
            Timber.d("Loop UUID to load median values (Loaded from DB): ${loopProgressLiveData.value?.uuid}")
            Timber.d("Loop UUID to load median values (already loaded): $loopUUID")
            loadMedianValues(loopUUID)
            _loopUUIDLiveData.postValue(loopLocalUUID)
            this.state.loopLocalUUID.set(loopLocalUUID)
        }
    }

    override fun onQoSTestProgressUpdated(tasksPassed: Int, tasksTotal: Int, progressMap: Map<QoSTestResultEnum, Int>) {
        state.setQoSTaskProgress(tasksPassed, tasksTotal)
        _qosProgressLiveData.postValue(progressMap)
    }

    override fun onLoopDistanceChanged(distancePassed: Int, distanceTotal: Int, locationAvailable: Boolean) {
        state.metersLeft.set((distanceTotal - distancePassed).toString())
        state.locationAvailable.set(locationAvailable)
    }
}