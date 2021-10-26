package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.databinding.ObservableLong
import at.rtr.rmbt.android.config.AppConfig
import at.specure.data.HistoryLoopMedian
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
private const val KEY_LOOP_TOTAL = "KEY_LOOP_TOTAL"
private const val KEY_LOOP_NEXT_TEST_TIME_PROGRESS = "KEY_LOOP_NEXT_TEST_TIME_PROGRESS"
private const val KEY_LOOP_NEXT_TEST_TIME_PERCENT = "KEY_LOOP_NEXT_TEST_TIME_PERCENT"
private const val KEY_LOOP_STATE = "KEY_LOOP_STATE"
private const val KEY_LOOP_NEXT_TEST_DISTANCE_METERS = "KEY_LOOP_NEXT_TEST_DISTANCE_METERS"
private const val KEY_LOOP_NEXT_TEST_DISTANCE_PERCENT = "KEY_LOOP_NEXT_TEST_DISTANCE_PERCENT"
private const val KEY_LOOP_UUID = "KEY_LOOP_UUID"
private const val KEY_LOOP_MODE_ENABLED = "KEY_LOOP_MODE_ENABLED"
private const val KEY_LAST_MEASUREMENT_SIGNAL = "KEY_LAST_MEASUREMENT_SIGNAL"
private const val KEY_DOWNLOAD_MEDIAN = "KEY_DOWNLOAD_MEDIAN"
private const val KEY_UPLOAD_MEDIAN = "KEY_UPLOAD_MEDIAN"
private const val KEY_PING_MEDIAN = "KEY_PING_MEDIAN"
private const val KEY_QOS_MEDIAN = "KEY_QOS_MEDIAN"
private const val KEY_QOS_PROGRESS_PRECENTS = "KEY_QOS_PROGRESS_PRECENTS"
private const val KEY_JITTER_MEDIAN = "KEY_JITTER_MEDIAN"
private const val KEY_PACKET_LOSS_MEDIAN = "KEY_PACKET_LOSS_MEDIAN"

class MeasurementViewState(private val config: AppConfig) : ViewState {

    val isConnected = ObservableField<Boolean?>()
    val measurementState = ObservableField<MeasurementState>().apply { set(MeasurementState.IDLE) }
    val measurementProgress = ObservableInt()
    val measurementDownloadUploadProgress = ObservableInt()
    val downloadSpeedBps = ObservableLong()
    val uploadSpeedBps = ObservableLong()
    val pingNanos = ObservableLong()
    val jitterNanos = ObservableLong()
    val packetLossPercent = ObservableInt()
    val signalStrengthInfo = ObservableField<SignalStrengthInfo?>()
    val signalStrengthInfoResult = ObservableField<SignalStrengthInfo?>()
    val networkInfo = ObservableField<NetworkInfo?>()
    val qosEnabled = ObservableBoolean()
    val qosTaskProgress = ObservableField<String>()
    val qosProgressPercents = ObservableField<Int>()
    val loopCurrentProgress = ObservableInt()
    val loopTotalCount = ObservableInt()
    val loopLocalUUID = ObservableField<String>()
    val timeToNextTestElapsed = ObservableField<String>()
    val timeToNextTestPercentage = ObservableInt()
    val loopModeRecord = ObservableField<LoopModeRecord?>()
    val loopNextTestDistanceMeters = ObservableField<String>()
    val loopNextTestPercent = ObservableInt()
    val gpsEnabled = ObservableBoolean()
    val isLoopModeActive = ObservableBoolean(config.loopModeEnabled)
    val downloadSpeedBpsMedian = ObservableLong()
    val uploadSpeedBpsMedian = ObservableLong()
    val pingNanosMedian = ObservableLong()
    val jitterNanosMedian = ObservableLong()
    val packetLossPercentMedian = ObservableInt()
    val qosPercentsMedian = ObservableField<Int?>()

    val metersLeft = ObservableField<String>().apply { set(loopNextTestDistanceMeters.get()) }
    val locationAvailable = ObservableBoolean().apply { set(true) }

    fun setQoSTaskProgress(current: Int, total: Int) {
        qosTaskProgress.set("$current/$total")
        qosProgressPercents.set(100 * current / total)
    }

    fun setLoopProgress(current: Int, total: Int) {
        setLoopCurrentTestNumber(current)
        setLoopTotalTestNumber(total)
    }

    private fun setLoopCurrentTestNumber(current: Int) {
        loopCurrentProgress.set(current)
    }

    private fun setLoopTotalTestNumber(total: Int) {
        loopTotalCount.set(total)
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

    fun setMedianValues(historyLoopMedian: HistoryLoopMedian?) {
        historyLoopMedian?.let {
            Timber.d("Loop Medians values = $it")
            this.downloadSpeedBpsMedian.set((historyLoopMedian.downloadMedianMbps * 1000000f).toLong())
            this.uploadSpeedBpsMedian.set((historyLoopMedian.uploadMedianMbps * 1000000f).toLong())
            this.pingNanosMedian.set((historyLoopMedian.pingMedianMillis * 1000000f).toLong())
            this.jitterNanosMedian.set((historyLoopMedian.jitterMedianMillis * 1000000f).toLong())
            this.packetLossPercentMedian.set((historyLoopMedian.packetLossMedian).toInt())
            this.qosPercentsMedian.set((historyLoopMedian.qosMedian)?.toInt())
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
            val distanceToNextTestMeters = config.loopModeDistanceMeters - loopModeRecord.movementDistanceMeters
            this.loopNextTestDistanceMeters.set(
                if (distanceToNextTestMeters < 0) {
                    "0"
                } else distanceToNextTestMeters.toString()
            )
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
            loopTotalCount.set(bundle.getInt(KEY_LOOP_TOTAL))
            loopCurrentProgress.set(bundle.getInt(KEY_LOOP_PROGRESS))
            timeToNextTestElapsed.set(bundle.getString(KEY_LOOP_NEXT_TEST_TIME_PROGRESS))
            timeToNextTestPercentage.set(bundle.getInt(KEY_LOOP_NEXT_TEST_TIME_PERCENT, 0))
            loopNextTestDistanceMeters.set(bundle.getString(KEY_LOOP_NEXT_TEST_DISTANCE_METERS))
            loopNextTestPercent.set(bundle.getInt(KEY_LOOP_NEXT_TEST_DISTANCE_PERCENT))
            loopLocalUUID.set(bundle.getString(KEY_LOOP_UUID))
            isLoopModeActive.set(bundle.getBoolean(KEY_LOOP_MODE_ENABLED))
            signalStrengthInfoResult.set(bundle.getParcelable(KEY_LAST_MEASUREMENT_SIGNAL))
            downloadSpeedBpsMedian.set(bundle.getLong(KEY_DOWNLOAD_MEDIAN, 0))
            uploadSpeedBpsMedian.set(bundle.getLong(KEY_UPLOAD_MEDIAN, 0))
            pingNanosMedian.set(bundle.getLong(KEY_PING_MEDIAN, 0))
            jitterNanosMedian.set(bundle.getLong(KEY_JITTER_MEDIAN, 0))
            packetLossPercentMedian.set(bundle.getInt(KEY_PACKET_LOSS_MEDIAN, 0))
            val qosMedian = bundle.getInt(KEY_QOS_MEDIAN, -1)
            if (qosMedian != -1) {
                qosPercentsMedian.set(qosMedian)
            }
            qosProgressPercents.set(bundle.getInt(KEY_QOS_PROGRESS_PRECENTS, 0))
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
            putInt(KEY_LOOP_PROGRESS, loopCurrentProgress.get())
            putInt(KEY_LOOP_TOTAL, loopTotalCount.get())
            putString(KEY_LOOP_NEXT_TEST_TIME_PROGRESS, timeToNextTestElapsed.get())
            putInt(KEY_LOOP_NEXT_TEST_TIME_PERCENT, timeToNextTestPercentage.get())
            putString(KEY_LOOP_NEXT_TEST_DISTANCE_METERS, loopNextTestDistanceMeters.get())
            putInt(KEY_LOOP_NEXT_TEST_DISTANCE_PERCENT, loopNextTestPercent.get())
            putString(KEY_LOOP_UUID, loopLocalUUID.get())
            putBoolean(KEY_LOOP_MODE_ENABLED, isLoopModeActive.get())
            putParcelable(KEY_LAST_MEASUREMENT_SIGNAL, signalStrengthInfoResult.get())
            putLong(KEY_DOWNLOAD_MEDIAN, downloadSpeedBpsMedian.get())
            putLong(KEY_UPLOAD_MEDIAN, uploadSpeedBpsMedian.get())
            putLong(KEY_PING_MEDIAN, pingNanosMedian.get())
            putInt(KEY_QOS_MEDIAN, qosPercentsMedian.get() ?: -1)
            putInt(KEY_QOS_PROGRESS_PRECENTS, qosProgressPercents.get() ?: 0)
        }
    }

    fun setSignalStrength(it: SignalStrengthInfo?) {
        signalStrengthInfo.set(it)
        if (!isLoopModeActive.get() || (measurementState.get() == MeasurementState.INIT || measurementState.get() == MeasurementState.DOWNLOAD || measurementState.get() == MeasurementState.PING || measurementState.get() == MeasurementState.UPLOAD || measurementState.get() == MeasurementState.QOS)) {
            signalStrengthInfoResult.set(it)
        }
    }
}