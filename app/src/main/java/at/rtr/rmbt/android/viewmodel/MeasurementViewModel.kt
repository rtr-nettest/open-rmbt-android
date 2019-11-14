package at.rtr.rmbt.android.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.android.ui.viewstate.MeasurementViewState
import at.specure.info.network.NetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.measurement.MeasurementClient
import at.specure.measurement.MeasurementProducer
import at.specure.measurement.MeasurementService
import at.specure.measurement.MeasurementState
import timber.log.Timber
import javax.inject.Inject

class MeasurementViewModel @Inject constructor() : BaseViewModel(), MeasurementClient {

    val state = MeasurementViewState()
    var producer: MeasurementProducer? = null

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceDisconnected(componentName: ComponentName?) {
            producer?.removeClient(this@MeasurementViewModel)
            producer = null
            Timber.i("On service disconnected")
        }

        override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
            producer = binder as MeasurementProducer?
            Timber.i("On service connected")

            producer?.let {
                it.addClient(this@MeasurementViewModel)

                with(state) {
                    measurementState.set(it.measurementState)
                    measurementProgress.set(it.measurementProgress)
                    pingMs.set(it.pingMs)
                    downloadSpeedBps.set(it.downloadSpeedBps)
                    uploadSpeedBps.set(it.uploadSpeedBps)
                    signalStrengthInfo.set(it.signalStrengthInfo)
                    networkInfo.set(it.networkInfo)
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

    override fun onMeasurementFinish() {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun onMeasurementError(error: HandledException) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun onSignalChanged(signalStrengthInfo: SignalStrengthInfo?) {
        state.signalStrengthInfo.set(signalStrengthInfo)
    }

    override fun onDownloadSpeedChanged(speedBps: Long) {
        state.downloadSpeedBps.set(speedBps)
    }

    override fun onUploadSpeedChanged(speedBps: Long) {
        state.uploadSpeedBps.set(speedBps)
    }

    override fun onPingChanged(pingMs: Long) {
        state.pingMs.set(pingMs)
    }

    override fun onActiveNetworkChanged(networkInfo: NetworkInfo?) {
        state.networkInfo.set(networkInfo)
    }
}