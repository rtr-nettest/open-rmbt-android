package at.rtr.rmbt.android.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.ui.viewstate.MeasurementViewState
import at.rtr.rmbt.android.util.plusAssign
import at.rtr.rmbt.client.v2.task.result.QoSTestResultEnum
import at.specure.data.entity.GraphItemRecord
import at.specure.data.entity.LoopModeRecord
import at.specure.data.repository.TestDataRepository
import at.specure.info.network.ActiveNetworkLiveData
import at.specure.info.strength.SignalStrengthLiveData
import at.specure.measurement.MeasurementClient
import at.specure.measurement.MeasurementProducer
import at.specure.measurement.MeasurementService
import at.specure.measurement.MeasurementState
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MeasurementViewModel @Inject constructor(
    private val testDataRepository: TestDataRepository,
    val signalStrengthLiveData: SignalStrengthLiveData,
    val activeNetworkLiveData: ActiveNetworkLiveData,
    val config: AppConfig
) : BaseViewModel(), MeasurementClient {

    private val _measurementFinishLiveData = MutableLiveData<Boolean>()
    private val _isTestsRunningLiveData = MutableLiveData<Boolean>()
    private val _measurementErrorLiveData = MutableLiveData<Boolean>()
    private val _downloadGraphLiveData = MutableLiveData<List<GraphItemRecord>>()
    private val _uploadGraphLiveData = MutableLiveData<List<GraphItemRecord>>()
    private val _qosProgressLiveData = MutableLiveData<Map<QoSTestResultEnum, Int>>()
    private val _loopUUID = MutableLiveData<String>()
    private val _loopProgressLiveData = MutableLiveData<LoopModeRecord?>()

    private var producer: MeasurementProducer? = null

    val state = MeasurementViewState(config)

    lateinit var testUUID: String
        private set

    val loopUuidLiveData: LiveData<String?>
        get() = _loopUUID

    val measurementFinishLiveData: LiveData<Boolean>
        get() = _measurementFinishLiveData

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

    val loopProgressLiveData: LiveData<LoopModeRecord?>
        get() = _loopUUID.value?.toString()?.let { testDataRepository.getLoopMode(it) }!!

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceDisconnected(componentName: ComponentName?) {
            producer?.removeClient(this@MeasurementViewModel)
            producer = null
            Timber.i("On service disconnected")
        }

        override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
            producer = binder as MeasurementProducer?
            Timber.i("On service connected")

            _isTestsRunningLiveData.postValue(producer?.isTestsRunning ?: false)

            producer?.let {
                it.addClient(this@MeasurementViewModel)

                with(state) {
                    measurementState.set(it.measurementState)
                    measurementProgress.set(it.measurementProgress)
                    pingMs.set(TimeUnit.NANOSECONDS.toMillis(it.pingNanos))
                    downloadSpeedBps.set(it.downloadSpeedBps)
                    uploadSpeedBps.set(it.uploadSpeedBps)
                }
            }
        }
    }

    init {
        addStateSaveHandler(state)
    }

    fun attach(context: Context) {
        context.bindService(MeasurementService.intent(context), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun detach(context: Context) {
        producer?.removeClient(this)
        context.unbindService(serviceConnection)
    }

    override fun onProgressChanged(state: MeasurementState, progress: Int) {
        this.state.measurementState.set(state)
        this.state.measurementProgress.set(progress)
    }

    override fun onMeasurementError() {
        _measurementErrorLiveData.postValue(true)
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

    override fun onPingChanged(pingNanos: Long) {
        state.pingMs.set(TimeUnit.NANOSECONDS.toMillis(pingNanos))
    }

    override fun isQoSEnabled(enabled: Boolean) {
        state.qosEnabled.set(enabled)
    }

    override fun onSubmitted() {
        Timber.d("Test Data sent")
        _measurementFinishLiveData.postValue(true)
    }

    override fun onSubmissionError(exception: HandledException) {
        Timber.d("Test Data submission failed")
        _measurementFinishLiveData.postValue(false)
    }

    override fun onLoopCountDownTimer(timePassedMillis: Long, timeTotalMillis: Long) {
        // TODO handle countdown timer
    }

    fun cancelMeasurement() {
        producer?.stopTests()
    }

    override fun onMeasurementCancelled() {
        _measurementFinishLiveData.postValue(false)
    }

    override fun onClientReady(testUUID: String, loopUUID: String?) {

        this.testUUID = testUUID
        _loopUUID.postValue(loopUUID)

        Timber.d("loopUUID: $loopUUID")

        testDataRepository.getDownloadGraphItemsLiveData(testUUID) {
            _downloadGraphLiveData.postValue(it)
        }

        testDataRepository.getUploadGraphItemsLiveData(testUUID) {
            _uploadGraphLiveData.postValue(it)
        }
    }

    override fun onQoSTestProgressUpdated(tasksPassed: Int, tasksTotal: Int, progressMap: Map<QoSTestResultEnum, Int>) {
        state.setQoSTaskProgress(tasksPassed, tasksTotal)
        _qosProgressLiveData.postValue(progressMap)
    }
}