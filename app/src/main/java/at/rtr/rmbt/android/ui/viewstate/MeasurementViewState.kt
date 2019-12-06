package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.databinding.ObservableLong
import at.specure.data.entity.GraphItemRecord
import at.specure.info.network.NetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.measurement.MeasurementState

private const val KEY_STATE = "KEY_STATE"
private const val KEY_PROGRESS = "KEY_PROGRESS"
private const val KEY_DOWNLOAD_UPLOAD_PROGRESS = "KEY_DOWNLOAD_UPLOAD_PROGRESS"
private const val KEY_DOWNLOAD = "KEY_DOWNLOAD"
private const val KEY_UPLOAD = "KEY_UPLOAD"
private const val KEY_PING = "KEY_PING"
private const val KEY_QOS_ENABLED = "KEY_QOS_ENABLED"

class MeasurementViewState : ViewState {

    val measurementState = ObservableField<MeasurementState>().apply { set(MeasurementState.IDLE) }
    val measurementProgress = ObservableInt()
    val measurementDownloadUploadProgress = ObservableInt()
    val downloadSpeedBps = ObservableLong()
    val uploadSpeedBps = ObservableLong()
    val pingMs = ObservableLong()
    val signalStrengthInfo = ObservableField<SignalStrengthInfo?>()
    val networkInfo = ObservableField<NetworkInfo?>()
    val downloadGraphItems = ObservableField<List<GraphItemRecord>>()
    val uploadGraphItems = ObservableField<List<GraphItemRecord>>()
    val qosEnabled = ObservableBoolean()

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.let {
            measurementState.set(bundle.getSerializable(KEY_STATE) as MeasurementState)
            measurementProgress.set(bundle.getInt(KEY_PROGRESS, 0))
            measurementDownloadUploadProgress.set(bundle.getInt(KEY_DOWNLOAD_UPLOAD_PROGRESS, 0))
            downloadSpeedBps.set(bundle.getLong(KEY_DOWNLOAD, 0))
            uploadSpeedBps.set(bundle.getLong(KEY_UPLOAD, 0))
            pingMs.set(bundle.getLong(KEY_PING, 0))
            qosEnabled.set(bundle.getBoolean(KEY_QOS_ENABLED, false))
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.apply {
            putSerializable(KEY_STATE, measurementState.get())
            putInt(KEY_PROGRESS, measurementProgress.get())
            putInt(KEY_DOWNLOAD_UPLOAD_PROGRESS, measurementDownloadUploadProgress.get())
            putLong(KEY_DOWNLOAD, downloadSpeedBps.get())
            putLong(KEY_UPLOAD, uploadSpeedBps.get())
            putLong(KEY_PING, pingMs.get())
            putBoolean(KEY_QOS_ENABLED, qosEnabled.get())
        }
    }
}