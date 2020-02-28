package at.specure.worker.request

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import at.rmbt.util.exception.NoConnectionException
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.di.CoreInjector
import at.specure.util.exception.DataMissingException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import javax.inject.Inject

class SignalMeasurementInfoWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    @Inject
    lateinit var repository: SignalMeasurementRepository

    override suspend fun doWork(): Result {
        CoreInjector.inject(this)

        val measurementId = inputData.getString(KEY_MEASUREMENT_ID) ?: throw DataMissingException("measurementId is missing in worker")

        var result = Result.failure()
        repository.registerMeasurement(measurementId)
            .catch { e ->
                if (e is NoConnectionException) {
                    emit(false)
                } else {
                    Timber.e(e, "Getting signal info record error")
                    emit(true)
                }
            }
            .collect {
                result = if (it) Result.success() else Result.retry()
            }

        return result
    }

    companion object {

        private const val KEY_MEASUREMENT_ID = "KEY_MEASUREMENT_ID"

        fun getData(measurementId: String) = Data.Builder().apply {
            putString(KEY_MEASUREMENT_ID, measurementId)
        }.build()
    }
}