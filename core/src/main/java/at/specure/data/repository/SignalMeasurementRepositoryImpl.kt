package at.specure.data.repository

import android.content.Context
import at.rmbt.client.control.ControlServerClient
import at.rmbt.util.exception.NoConnectionException
import at.rmbt.util.io
import at.specure.data.ClientUUID
import at.specure.data.CoreDatabase
import at.specure.data.entity.SignalMeasurementChunk
import at.specure.data.entity.SignalMeasurementInfo
import at.specure.data.entity.SignalMeasurementRecord
import at.specure.data.entity.TestTelephonyRecord
import at.specure.data.entity.TestWlanRecord
import at.specure.data.toModel
import at.specure.data.toRequest
import at.specure.info.TransportType
import at.specure.measurement.signal.SignalMeasurementChunkResultCallback
import at.specure.test.DeviceInfo
import at.specure.util.exception.DataMissingException
import at.specure.worker.WorkLauncher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class SignalMeasurementRepositoryImpl(
    private val db: CoreDatabase,
    private val context: Context,
    private val clientUUID: ClientUUID,
    private val client: ControlServerClient
) : SignalMeasurementRepository {

    private val deviceInfo = DeviceInfo(context)
    private val dao = db.signalMeasurementDao()
    private val testDao = db.testDao()

    override fun saveAndUpdateRegisteredRecord(record: SignalMeasurementRecord, newUuid: String, oldInfo: SignalMeasurementInfo) = io {
        dao.saveSignalMeasurementRecord(record)
        SignalMeasurementInfo(
            measurementId = record.id,
            uuid = newUuid,
            clientRemoteIp = oldInfo.clientRemoteIp,
            resultUrl = oldInfo.resultUrl,
            provider = oldInfo.provider
        )
            .also {
                dao.saveSignalMeasurementInfo(it)
            }
    }

    override fun saveAndRegisterRecord(record: SignalMeasurementRecord) = io {
        dao.saveSignalMeasurementRecord(record)
        registerMeasurement(record.id)
            .catch { e ->
                if (e is NoConnectionException) {
                    emit(false)
                } else {
                    Timber.e(e, "Getting signal info record error")
                    emit(true)
                }
            }
            .collect {
                if (!it) {
                    WorkLauncher.enqueueSignalMeasurementInfoRequest(context, record.id)
                }
            }
    }

    override fun updateSignalMeasurementRecord(record: SignalMeasurementRecord) = io {
        dao.updateSignalMeasurementRecord(record)
    }

    override fun registerMeasurement(measurementId: String): Flow<Boolean> = flow {

        val uuid = clientUUID.value ?: throw DataMissingException("Missing client UUID")
        val record = dao.getSignalMeasurementRecord(measurementId) ?: throw DataMissingException("Measurement record $measurementId is missing")
        val body = record.toRequest(uuid, deviceInfo)

        val info = dao.getSignalMeasurementInfo(record.id)
        if (info == null) {

            val response = client.signalRequest(body)

            response.onSuccess {
                dao.saveSignalMeasurementInfo(it.toModel(measurementId))
            }

            if (response.ok) {
                emit(true)
            } else {
                throw response.failure
            }
        } else {
            emit(true)
        }
    }

    override fun getSignalMeasurementChunk(chunkId: String): Flow<SignalMeasurementChunk?> = flow {
        var chunk = dao.getSignalMeasurementChunk(chunkId)
        chunk?.let {
            val record = dao.getSignalMeasurementRecord(it.measurementId)
            /*if (record != null) {
                if (record.resetChunkNumber) {
                    chunk = it.copy(id = chunkId, sequenceNumber = 0)
                    Timber.i("update chunk sequence id GET chunkBefore = $chunkId, chunkID = ${it.id} sequence: ${it.sequenceNumber}")
                    record.resetChunkNumber = false
                    updateSignalMeasurementRecord(record)
                }
            }*/
        }
        emit(chunk)
    }

    override fun saveMeasurementChunk(chunk: SignalMeasurementChunk) = io {
        dao.saveSignalMeasurementChunk(chunk)
    }

    override fun saveMeasurementRecord(record: SignalMeasurementRecord) = io {
        dao.saveSignalMeasurementRecord(record)
    }

    override fun sendMeasurementChunk(chunk: SignalMeasurementChunk, callBack: SignalMeasurementChunkResultCallback) = io {
        dao.saveSignalMeasurementChunk(chunk)
        val info = dao.getSignalMeasurementInfo(chunk.measurementId)
        sendMeasurementChunk(chunk.id, callBack)
            .catch { e ->
                if (e is NoConnectionException) {
                    emit(null)
                } else {
                    Timber.e(e, "Getting signal info record error")
                    emit("")
                }
            }
            .collect {
                if (it == null) {
                    WorkLauncher.enqueueSignalMeasurementChunkRequest(context, chunk.id)
                }
                info?.let { info ->
                    if (!it.isNullOrEmpty() && it != info.uuid) {
                        callBack.newUUIDSent(it, info)
                    }
                }
            }
    }

    override fun sendMeasurementChunk(chunkId: String, callback: SignalMeasurementChunkResultCallback): Flow<String?> = flow {
        var chunk = dao.getSignalMeasurementChunk(chunkId) ?: throw DataMissingException("SignalMeasurementChunk not found with id: $chunkId")
        val record = dao.getSignalMeasurementRecord(chunk.measurementId)
            ?: throw DataMissingException("SignalMeasurementRecord not found with id: ${chunk.measurementId}")

        var info = dao.getSignalMeasurementInfo(record.id)

        val clientUUID = clientUUID.value ?: throw DataMissingException("ClientUUID is null")

        val telephonyInfo: TestTelephonyRecord? =
            if (record.transportType == TransportType.CELLULAR) {
                testDao.getTelephonyRecord(chunkId)
            } else {
                null
            }

        val wlanInfo: TestWlanRecord? = if (record.transportType == TransportType.WIFI) {
            testDao.getWlanRecord(chunkId)
        } else {
            null
        }

        /* if (record.resetChunkNumber) {
             chunk = chunk.copy(id = chunkId, sequenceNumber = 0)
             record.resetChunkNumber = false
             Timber.i("update chunk sequence id send chunk Before = $chunkId, chunkID = ${chunk.id} sequence: ${chunk.sequenceNumber}")
             dao.updateSignalMeasurementRecord(record)
         }*/

        val body = record.toRequest(
            measurementInfoUUID = info?.uuid,
            clientUUID = clientUUID,
            chunk = chunk,
            deviceInfo = deviceInfo,
            telephonyInfo = telephonyInfo,
            wlanInfo = wlanInfo,
            locations = db.geoLocationDao().get(chunkId),
            capabilities = db.capabilitiesDao().get(chunkId),
            cellInfoList = db.cellInfoDao().get(chunkId),
            signalList = db.signalDao().get(chunkId),
            permissions = db.permissionStatusDao().get(chunkId),
            networkEvents = db.connectivityStateDao().getStates(chunkId).toRequest(),
            cellLocationList = db.cellLocationDao().get(chunkId)
        )

        val result = client.signalResult(body)

        if (result.ok) {
            Timber.d("SM Chunk OK responded with uuid: ${result.success.uuid}   before: ${info?.uuid}")
            if (info == null) {
                info = SignalMeasurementInfo(
                    measurementId = record.id,
                    uuid = result.success.uuid,
                    clientRemoteIp = "", // TODO need to fill that field
                    resultUrl = "", // TODO need to fill that field
                    provider = "" // TODO need to fill that field
                )
                dao.saveSignalMeasurementInfo(info)
            } else {
                if (result.success.uuid.isNotEmpty() && result.success.uuid != info.uuid) {
                    Timber.d("SM Chunk creating new chunk with uuid: ${result.success.uuid}   before: ${info.uuid}")
                    callback.newUUIDSent(result.success.uuid, info)

                    /*
                    info = SignalMeasurementInfo(
                        measurementId = record.id,
                        uuid = result.success.uuid,
                        clientRemoteIp = info.clientRemoteIp,
                        resultUrl = info.resultUrl,
                        provider = info.provider
                    )
                    dao.saveSignalMeasurementInfo(info)
                    SignalMeasurementRecord(
                        id = record.id,
                        networkUUID = record.networkUUID,
                        location = record.location,
                        transportType = record.transportType,
                        mobileNetworkType = record.mobileNetworkType,
                        resetChunkNumber = true
                    ).also {
                        dao.updateSignalMeasurementRecord(it)
                        Timber.d("SM Chunk updating record to reset chunk sequence number")
                    }*/
                }
            }

            testDao.removeTelephonyInfo(chunkId)
            testDao.removeWlanRecord(chunkId)
            db.geoLocationDao().remove(chunkId)
            db.capabilitiesDao().remove(chunkId)
            db.cellInfoDao().removeCellInfo(chunkId)
            db.signalDao().remove(chunkId)
            db.permissionStatusDao().remove(chunkId)
            db.connectivityStateDao().remove(chunkId)
            db.cellLocationDao().remove(chunkId)
        } else {
            chunk.submissionRetryCount++
            Timber.d("SM Chunk FAILED responded: ${info?.uuid}")
            if (result.failure !is NoConnectionException) {
                chunk.testErrorCause = result.failure.message
            }

            dao.saveSignalMeasurementChunk(chunk)
            throw result.failure
        }
    }
}