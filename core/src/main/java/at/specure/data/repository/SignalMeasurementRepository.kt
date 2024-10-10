package at.specure.data.repository

import androidx.lifecycle.LiveData
import at.specure.data.entity.SignalMeasurementChunk
import at.specure.data.entity.SignalMeasurementInfo
import at.specure.data.entity.SignalMeasurementPointRecord
import at.specure.data.entity.SignalMeasurementRecord
import at.specure.data.entity.SignalMeasurementSession
import at.specure.data.entity.SignalRecord
import at.specure.measurement.signal.SignalMeasurementChunkReadyCallback
import at.specure.measurement.signal.SignalMeasurementChunkResultCallback
import at.specure.measurement.signal.ValidChunkPostProcessing
import kotlinx.coroutines.flow.Flow

interface SignalMeasurementRepository {

    /**
     * Saves [record] to the local database and sends request to the server to obtain signal measurement test uuid
     */
    fun saveAndRegisterRecord(record: SignalMeasurementRecord)

    fun updateSignalMeasurementRecord(record: SignalMeasurementRecord)

    /**
     * Method to save new [record] and create new SignalMeasurementInfo record because of provided new [newUuid] from the backend and
     * create a new info from old info and updated uuid
     */
    fun saveAndUpdateRegisteredRecord(record: SignalMeasurementRecord, newUuid: String, oldInfo: SignalMeasurementInfo)

    fun registerMeasurement(measurementId: String): Flow<Boolean>

    fun saveMeasurementRecord(record: SignalMeasurementRecord)

    fun saveMeasurementChunk(chunk: SignalMeasurementChunk)

    fun sendMeasurementChunk(chunk: SignalMeasurementChunk, callBack: SignalMeasurementChunkResultCallback)

    fun shouldSendMeasurementChunk(
        chunk: SignalMeasurementChunk,
        postProcessing: ValidChunkPostProcessing,
        callback: SignalMeasurementChunkReadyCallback
    )

    fun getSignalMeasurementChunk(chunkId: String): Flow<SignalMeasurementChunk?>

    /**
     * if it returns:
     * null -> result was not send successfully
     * empty string -> result was sent successfully
     * string -> result was sent successfully and we have uuid to compare wih old one. If it is different we must use new uuid with signal chunks.
     */
    fun sendMeasurementChunk(chunkId: String, callback: SignalMeasurementChunkResultCallback): Flow<String?>

    fun saveDedicatedMeasurementSession(session: SignalMeasurementSession)

    fun getDedicatedMeasurementSession(sessionId: String): SignalMeasurementSession?

    fun saveMeasurementPointRecord(point: SignalMeasurementPointRecord)

    fun loadSignalMeasurementPointRecordsForMeasurement(measurementId: String): LiveData<List<SignalMeasurementPointRecord>>

    suspend fun getSignalMeasurementRecord(id: String?): SignalRecord?

    fun updateSignalMeasurementPoint(updatedPoint: SignalMeasurementPointRecord)
}