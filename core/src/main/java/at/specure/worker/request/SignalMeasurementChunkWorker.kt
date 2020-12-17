package at.specure.worker.request

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import at.rmbt.util.exception.NoConnectionException
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.di.CoreInjector
import at.specure.measurement.signal.SignalMeasurementChunkResultCallback
import at.specure.util.exception.DataMissingException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import javax.inject.Inject

class SignalMeasurementChunkWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    @Inject
    lateinit var repository: SignalMeasurementRepository

    override suspend fun doWork(): Result {
        CoreInjector.inject(this)

        val chunkId = inputData.getString(KEY_CHUNK_ID) ?: throw DataMissingException("measurementId is missing in worker")

        var result = Result.failure()
        repository.sendMeasurementChunk(chunkId, object : SignalMeasurementChunkResultCallback {
            override fun chunkSentResult(respondedUuid: String?) {}
        })
            .catch { e ->
                if (e is NoConnectionException) {
                    emit(null)
                } else {
                    Timber.e(e, "Getting signal info record error")
                    emit("")
                }
            }
            .collect {
                result = if (it != null) Result.success() else Result.retry()
            }

        return result
    }

    companion object {

        private const val KEY_CHUNK_ID = "KEY_CHUNK_ID"

        fun getData(chunkId: String) = Data.Builder().apply {
            putString(KEY_CHUNK_ID, chunkId)
        }.build()
    }
}