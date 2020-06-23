package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.databinding.ObservableLong
import at.rtr.rmbt.android.config.AppConfig
import at.specure.data.entity.LoopModeRecord
import at.specure.data.entity.LoopModeState
import at.specure.info.network.NetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.measurement.MeasurementState
import timber.log.Timber

private const val KEY_STATE = "KEY_STATE"
private const val KEY_PROGRESS = "KEY_PROGRESS"
private const val KEY_DOWNLOAD_UPLOAD_PROGRESS = "KEY_DOWNLOAD_UPLOAD_PROGRESS"
private const val KEY_DOWNLOAD = "KEY_DOWNLOAD"
private const val KEY_UPLOAD = "KEY_UPLOAD"
private const val KEY_PING = "KEY_PING"
private const val KEY_QOS_ENABLED = "KEY_QOS_ENABLED"
private const val KEY_QOS_TASK_PROGRESS = "QOS_PROGRESS"
private const val KEY_LOOP_PROGRESS = "KEY_LOOP_PROGRESS"
private const val KEY_LOOP_NEXT_TEST_TIME_PROGRESS = "KEY_LOOP_NEXT_TEST_TIME_PROGRESS"
private const val KEY_LOOP_NEXT_TEST_TIME_PERCENT = "KEY_LOOP_NEXT_TEST_TIME_PERCENT"
private const val KEY_LOOP_STATE = "KEY_LOOP_STATE"
private const val KEY_LOOP_NEXT_TEST_DISTANCE_METERS = "KEY_LOOP_NEXT_TEST_DISTANCE_METERS"
private const val KEY_LOOP_NEXT_TEST_DISTANCE_PERCENT = "KEY_LOOP_NEXT_TEST_DISTANCE_PERCENT"
private const val KEY_LOOP_UUID = "KEY_LOOP_UUID"
private const val KEY_LOOP_MODE_ENABLED = "KEY_LOOP_MODE_ENABLED"
private const val KEY_LAST_MEASUREMENT_SIGNAL = "KEY_LAST_MEASUREMENT_SIGNAL"

class MeasurementViewState(private val config: AppConfig) : ViewState {

    val measurementState = ObservableField<MeasurementState>().apply { set(MeasurementState.IDLE) }
    val measurementProgress = ObservableInt()
    val measurementDownloadUploadProgress = ObservableInt()
    val downloadSpeedBps = ObservableLong()
    val uploadSpeedBps = ObservableLong()
    val pingNanos = ObservableLong()
    val signalStrengthInfo = ObservableField<SignalStrengthInfo?>()
    val signalStrengthInfoResult = ObservableField<SignalStrengthInfo?>()
    val networkInfo = ObservableField<NetworkInfo?>()
    val qosEnabled = ObservableBoolean()
    val qosTaskProgress = ObservableField<String>()
    val loopProgress = ObservableField<String>()
    val loopLocalUUID = ObservableField<String>()
    val timeToNextTestElapsed = ObservableField<String>()
    val timeToNextTestPercentage = ObservableInt()
    val loopModeRecord = ObservableField<LoopModeRecord?>()
    val loopNextTestDistanceMeters = ObservableField<String>()
    val loopNextTestPercent = ObservableInt()
    val gpsEnabled = ObservableBoolean()
    val isLoopModeActive = ObservableBoolean(config.loopModeEnabled)

    val metersLeft = ObservableField<String>().apply { set("") }
    val locationAvailable = ObservableBoolean().apply { set(true) }

    fun setQoSTaskProgress(current: Int, total: Int) {
        qosTaskProgress.set("$current/$total")
    }

    fun setLoopProgress(current: Int, total: Int) {
        loopProgress.set("$current/$total")
    }

    private fun setLoopState(loopState: LoopModeState) {
        Timber.i("Measurement state from set loop state: loop: $loopState")
        if ((loopState != LoopModeState.RUNNING) && (loopState == LoopModeState.RUNNING)) {
            signalStrengthInfoResult.set(null)
        }
        if (loopState == LoopModeState.IDLE) {
            measurementProgress.set(0)
            measurementDownloadUploadProgress.set(0)
            measurementState.set(MeasurementState.FINISH)
            Timber.i("Measurement state from set loop state: ${measurementState.get()}")
        }
    }

    fun setLoopRecord(loopModeRecord: LoopModeRecord?) {
        if (loopModeRecord != null) {
            this.loopModeRecord.set(loopModeRecord)
            var distancePercent = 100
            if (config.loopModeDistanceMeters != 0) {
                distancePercent = loopModeRecord.movementDistanceMeters * 100 / config.loopModeDistanceMeters
            }
            this.loopNextTestPercent.set(distancePercent)
            this.loopNextTestDistanceMeters.set(loopModeRecord.movementDistanceMeters.toString())
            this.loopModeRecord.get()?.status?.let { setLoopState(it) }
        } else {
            this.loopNextTestPercent.set(0)
            this.loopNextTestDistanceMeters.set("")
        }
    }

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.let {
            measurementState.set(bundle.getSerializable(KEY_STATE) as MeasurementState)
            measurementProgress.set(bundle.getInt(KEY_PROGRESS, 0))
            measurementDownloadUploadProgress.set(bundle.getInt(KEY_DOWNLOAD_UPLOAD_PROGRESS, 0))
            downloadSpeedBps.set(bundle.getLong(KEY_DOWNLOAD, 0))
            uploadSpeedBps.set(bundle.getLong(KEY_UPLOAD, 0))
            pingNanos.set(bundle.getLong(KEY_PING, 0))
            qosEnabled.set(bundle.getBoolean(KEY_QOS_ENABLED, false))
            qosTaskProgress.set(bundle.getString(KEY_QOS_TASK_PROGRESS))
            loopProgress.set(bundle.getString(KEY_LOOP_PROGRESS))
            timeToNextTestElapsed.set(bundle.getString(KEY_LOOP_NEXT_TEST_TIME_PROGRESS))
            timeToNextTestPercentage.set(bundle.getInt(KEY_LOOP_NEXT_TEST_TIME_PERCENT, 0))
            loopNextTestDistanceMeters.set(bundle.getString(KEY_LOOP_NEXT_TEST_DISTANCE_METERS))
            loopNextTestPercent.set(bundle.getInt(KEY_LOOP_NEXT_TEST_DISTANCE_PERCENT))
            loopLocalUUID.set(bundle.getString(KEY_LOOP_UUID))
            isLoopModeActive.set(bundle.getBoolean(KEY_LOOP_MODE_ENABLED))
            signalStrengthInfoResult.set(bundle.getParcelable(KEY_LAST_MEASUREMENT_SIGNAL))
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.apply {
            putSerializable(KEY_STATE, measurementState.get())
            putInt(KEY_PROGRESS, measurementProgress.get())
            putInt(KEY_DOWNLOAD_UPLOAD_PROGRESS, measurementDownloadUploadProgress.get())
            putLong(KEY_DOWNLOAD, downloadSpeedBps.get())
            putLong(KEY_UPLOAD, uploadSpeedBps.get())
            putLong(KEY_PING, pingNanos.get())
            putBoolean(KEY_QOS_ENABLED, qosEnabled.get())
            putString(KEY_QOS_TASK_PROGRESS, qosTaskProgress.get())
            putString(KEY_LOOP_PROGRESS, loopProgress.get())
            putString(KEY_LOOP_NEXT_TEST_TIME_PROGRESS, timeToNextTestElapsed.get())
            putInt(KEY_LOOP_NEXT_TEST_TIME_PERCENT, timeToNextTestPercentage.get())
            putString(KEY_LOOP_NEXT_TEST_DISTANCE_METERS, loopNextTestDistanceMeters.get())
            putInt(KEY_LOOP_NEXT_TEST_DISTANCE_PERCENT, loopNextTestPercent.get())
            putString(KEY_LOOP_UUID, loopLocalUUID.get())
            putBoolean(KEY_LOOP_MODE_ENABLED, isLoopModeActive.get())
            putParcelable(KEY_LAST_MEASUREMENT_SIGNAL, signalStrengthInfoResult.get())
        }
    }

    fun setSignalStrength(it: SignalStrengthInfo?) {
        signalStrengthInfo.set(it)
        if (!isLoopModeActive.get() || (measurementState.get() == MeasurementState.INIT || measurementState.get() == MeasurementState.DOWNLOAD || measurementState.get() == MeasurementState.PING || measurementState.get() == MeasurementState.UPLOAD || measurementState.get() == MeasurementState.QOS)) {
            signalStrengthInfoResult.set(it)
        }
    }
}