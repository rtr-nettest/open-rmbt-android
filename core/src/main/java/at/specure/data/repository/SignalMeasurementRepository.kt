package at.specure.data.repository

import at.specure.data.entity.SignalMeasurementChunk
import at.specure.data.entity.SignalMeasurementRecord
import at.specure.measurement.signal.SignalMeasurementChunkResultCallback
import kotlinx.coroutines.flow.Flow

interface SignalMeasurementRepository {

    /**
     * Saves [record] to the local database and sends request to the server to obtain signal measurement test uuid
     */
    fun saveAndRegisterRecord(record: SignalMeasurementRecord)

    fun updateSignalMeasurementRecord(record: SignalMeasurementRecord)

    fun registerMeasurement(measurementId: String): Flow<Boolean>

    fun saveMeasurementChunk(chunk: SignalMeasurementChunk)

    fun sendMeasurementChunk(chunk: SignalMeasurementChunk, callback: SignalMeasurementChunkResultCallback)

    fun getSignalMeasurementChunk(chunkId: String): Flow<SignalMeasurementChunk?>

    /**
     * if it returns:
     * null -> result was not send successfully
     * empty string -> result was sent successfully
     * string -> result was sent successfully and we have uuid to compare wih old one. If it is different we must use new uuid with signal chunks.
     */
    fun sendMeasurementChunk(chunkId: String, callback: SignalMeasurementChunkResultCallback): Flow<String?>
}