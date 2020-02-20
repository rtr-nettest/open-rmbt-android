package at.specure.data.repository

import at.specure.data.entity.SignalMeasurementChunk
import at.specure.data.entity.SignalMeasurementRecord
import kotlinx.coroutines.flow.Flow

interface SignalMeasurementRepository {

    /**
     * Saves [record] to the local database and sends request to the server to obtain signal measurement test uuid
     */
    fun saveAndRegisterRecord(record: SignalMeasurementRecord)

    fun updateSignalMeasurementRecord(record: SignalMeasurementRecord)

    fun registerMeasurement(measurementId: String): Flow<Boolean>

    fun saveMeasurementChunk(chunk: SignalMeasurementChunk)

    fun sendMeasurementChunk(chunk: SignalMeasurementChunk)

    fun sendMeasurementChunk(chunkId: String): Flow<Boolean>
}