package at.specure.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import at.rmbt.client.control.ControlServerClient
import at.rmbt.util.exception.NoConnectionException
import at.rmbt.util.io
import at.specure.config.Config
import at.specure.data.ClientUUID
import at.specure.data.CoreDatabase
import at.specure.data.RequestFilters.Companion.createRadioInfoBody
import at.specure.data.entity.CellInfoRecord
import at.specure.data.entity.SignalMeasurementChunk
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.SignalMeasurementRecord
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.data.entity.SignalRecord
import at.specure.data.entity.TestTelephonyRecord
import at.specure.data.entity.TestWlanRecord
import at.specure.data.toCoverageRequest
import at.specure.data.toCoverageResultRequest
import at.specure.data.toRequest
import at.specure.info.TransportType
import at.specure.measurement.signal.SignalMeasurementChunkReadyCallback
import at.specure.measurement.signal.SignalMeasurementChunkResultCallback
import at.specure.measurement.signal.ValidChunkPostProcessing
import at.specure.test.DeviceInfo
import at.specure.util.exception.DataMissingException
import at.specure.worker.WorkLauncher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class SignalMeasurementRepositoryImpl(
    private val db: CoreDatabase,
    private val context: Context,
    private val clientUUID: ClientUUID,
    private val client: ControlServerClient,
    private val config: Config
) : SignalMeasurementRepository {

    private val deviceInfo = DeviceInfo(context)
    private val dao = db.signalMeasurementDao()
    private val testDao = db.testDao()

    // TODO: we should perform a new request for session
    override fun saveAndUpdateRegisteredRecord(record: SignalMeasurementRecord, newUuid: String, oldSession: CoverageMeasurementSession) = io {
        dao.saveSignalMeasurementRecord(record)
        val newSession = oldSession.copy(
            sessionId = record.id,
            serverSessionId = newUuid,
        )
        .also {
            dao.saveDedicatedSignalMeasurementSession(it)
        }
    }

    override fun saveAndRegisterRecord(record: SignalMeasurementRecord) = io {
        dao.saveSignalMeasurementRecord(record)
        registerCoverageMeasurement(coverageSessionId = null, record.id)
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
                    WorkLauncher.enqueueCoverageMeasurementRequest(context, record.id)
                }
            }
    }

    override fun updateSignalMeasurementRecord(record: SignalMeasurementRecord) = io {
        val count = dao.updateSignalMeasurementRecord(record)
        if (count == 0) {
            Timber.e("DB: failed to update signal measurement record")
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

    override fun saveDedicatedMeasurementSession(session: CoverageMeasurementSession) {
        dao.saveDedicatedSignalMeasurementSession(session)
    }

    override fun getDedicatedMeasurementSession(sessionId: String): CoverageMeasurementSession? {
        return dao.getDedicatedSignalMeasurementSession(sessionId)
    }

    override fun saveMeasurementPointRecord(point: CoverageMeasurementFenceRecord) = io {
        dao.saveSignalMeasurementPoint(point)
    }

    override fun loadSignalMeasurementPointRecordsForMeasurement(measurementId: String): LiveData<List<CoverageMeasurementFenceRecord>> {
        return dao.getSignalMeasurementPoints(measurementId)
    }

    override suspend fun getSignalMeasurementRecord(id: String?): SignalRecord? {
        return dao.getSignalRecordNullable(id)
    }

    override fun updateSignalMeasurementFence(updatedPoint: CoverageMeasurementFenceRecord) {
        dao.updateSignalMeasurementPoint(updatedPoint)
    }

    /**
     * Do not use this method outside a worker as we need to perform it when connection is back
     */
    override fun registerCoverageMeasurement(coverageSessionId: String?, measurementRecordId: String?): Flow<Boolean> = flow {

        val clientUUID = clientUUID.value ?: throw DataMissingException("Missing client UUID")
        val coverageSession =
            retrieveCoverageSessionOrCreate(coverageSessionId, measurementRecordId) // if not created yet, we create one for registration
        val deviceInfo = deviceInfo ?: throw DataMissingException("Missing device info")
        val body = coverageSession.toCoverageRequest(clientUUID, deviceInfo, config)

//        val record = dao.getSignalMeasurementRecord(measurementId) ?: throw DataMissingException("Measurement record $measurementId is missing")

        val response = client.coverageRequest(body)
        response.onSuccess {
            Timber.d("$it")
            dao.saveDedicatedSignalMeasurementSession(
                session = CoverageMeasurementSession(
                    sessionId = coverageSession.sessionId,
                    measurementId = measurementRecordId,
                    serverSessionId = it.testUUID,
                    serverSessionLoopId = "it.loopUUID will be done", // TODO:
                    pingServerHost = it.pingHost,
                    pingServerPort = it.pingPort.toIntOrNull() ?: -1,
                    pingServerToken = it.pingToken,
                    ipVersion = it.ipVersion,
                    remoteIpAddress = it.clientRemoteIp,
                    provider = it.provider,
                    startTimeMillis = coverageSession.startTimeMillis,
                    startResponseReceivedMillis = System.currentTimeMillis(),
                    maxCoverageMeasurementSeconds = it.maxCoverageMeasurementSeconds,
                    maxCoverageSessionSeconds = it.maxCoverageMeasurementSeconds,
                )
            )
        }

        if (response.ok) {
            emit(true)
        } else {
            throw response.failure
        }

    }

    private fun retrieveCoverageSessionOrCreate(coverageSessionId: String?, measurementRecordId: String?): CoverageMeasurementSession {
        val coverageSession =
            if (coverageSessionId == null) {
                if (measurementRecordId == null) {
                    null
                } else {
                    dao.getDedicatedSignalMeasurementSessionForMeasurementId(measurementRecordId)
                }
            } else {
                dao.getDedicatedSignalMeasurementSession(coverageSessionId)
            } ?: CoverageMeasurementSession() // if not created yet, we create one for registration
        return coverageSession
    }

    override fun saveMeasurementChunk(chunk: SignalMeasurementChunk) = io {
        dao.saveSignalMeasurementChunk(chunk)
    }

    override fun saveMeasurementRecord(record: SignalMeasurementRecord) = io {
        dao.saveSignalMeasurementRecord(record)
    }

    override fun shouldSendMeasurementChunk(
        chunk: SignalMeasurementChunk,
        postProcessing: ValidChunkPostProcessing,
        callback: SignalMeasurementChunkReadyCallback
    ) = io {
        val valid = validateMeasurementChunk(db.cellInfoDao().get(null, chunk.id), db.signalDao().get(null, chunk.id), chunk)
        callback.onSignalMeasurementChunkReadyCheckResult(valid, chunk, postProcessing)
    }

    private fun validateMeasurementChunk(cellInfos: List<CellInfoRecord>, signals: List<SignalRecord>, chunk: SignalMeasurementChunk): Boolean {
        val radioInfo = createRadioInfoBody(cellInfos, signals, chunk)
        return (radioInfo != null) && radioInfo.signals?.isNotEmpty() ?: false
    }

    override fun sendMeasurementChunk(chunk: SignalMeasurementChunk, callBack: SignalMeasurementChunkResultCallback) = io {
        dao.saveSignalMeasurementChunk(chunk)
        val session = dao.getDedicatedSignalMeasurementSessionForMeasurementId(chunk.measurementId)
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
                session?.let { session ->
                    if (!it.isNullOrEmpty() && it != session.serverSessionId) {
                        callBack.newUUIDSent(it, session)
                    }
                }
            }
    }

    override fun sendFences(sessionId: String, fences: List<CoverageMeasurementFenceRecord>) {
        // todo: update times before sending the fences (regarding real time of coverageRequest response arrival time)
        val coverageSession = retrieveCoverageSessionOrCreate(sessionId, null)
        if (coverageSession.isRegistered()) {
            clientUUID.value?.let {clientUuid ->
                val requestBody = coverageSession.toCoverageResultRequest(clientUuid, deviceInfo, config, fences)
                client.coverageResult(requestBody)
                // TODO: enqueue sending with worker in case of failed send
            }
        }
    }

    /**
     *  TODO: add scenario when measurement chunk response with other uuid than it is already in the session
     */
    override fun sendMeasurementChunk(chunkId: String, callback: SignalMeasurementChunkResultCallback): Flow<String?> = flow {
        var chunk = dao.getSignalMeasurementChunk(chunkId) ?: throw DataMissingException("SignalMeasurementChunk not found with id: $chunkId")
        val record = dao.getSignalMeasurementRecord(chunk.measurementId)
            ?: throw DataMissingException("SignalMeasurementRecord not found with id: ${chunk.measurementId}")

        var session = dao.getDedicatedSignalMeasurementSessionForMeasurementId(record.id)

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
        val capabilities = db.capabilitiesDao().get(null, chunkId)

        capabilities?.let {

            val body = record.toRequest(
                measurementInfoUUID = session?.serverSessionId,
                clientUUID = clientUUID,
                chunk = chunk,
                deviceInfo = deviceInfo,
                telephonyInfo = telephonyInfo,
                wlanInfo = wlanInfo,
                locations = db.geoLocationDao().get(null, chunkId),
                capabilities = it,
                cellInfoList = db.cellInfoDao().get(null, chunkId),
                signalList = db.signalDao().get(null, chunkId),
                permissions = db.permissionStatusDao().get(null, chunkId),
                networkEvents = db.connectivityStateDao().getStates(chunkId).toRequest(),
                cellLocationList = db.cellLocationDao().get(null, chunkId)
            )

            val result = client.signalResult(body)

            if (result.ok) {
                Timber.d("SM Chunk OK responded with uuid: ${result.success.uuid}   before: ${session?.serverSessionId}")
                if (session == null) {
                    session = CoverageMeasurementSession(
                        measurementId = record.id,
                        serverSessionId = result.success.uuid,
                        remoteIpAddress = "", // TODO need to fill that field
                        provider = "" // TODO need to fill that field
                    )
                    session?.let { signalMeasurementSession ->
                        dao.saveDedicatedSignalMeasurementSession(signalMeasurementSession)
                    }

                } else {
                    if (result.success.uuid.isNotEmpty() && result.success.uuid != session?.serverSessionId) {
                        Timber.d("SM Chunk creating new chunk with uuid: ${result.success.uuid}   before: ${session?.serverSessionId}")
                        session?.let { signalMeasurementInfo ->
                            callback.newUUIDSent(result.success.uuid, signalMeasurementInfo)
                        }

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
                db.geoLocationDao().remove(null, chunkId)
                db.capabilitiesDao().remove(null, chunkId)
                db.cellInfoDao().removeAllCellInfo(null, chunkId)
                db.signalDao().remove(null, chunkId)
                db.permissionStatusDao().remove(null, chunkId)
                db.connectivityStateDao().remove(chunkId)
                db.cellLocationDao().remove(null, chunkId)

            } else {
                chunk.submissionRetryCount++
                Timber.d("SM Chunk FAILED responded: ${session?.serverSessionId}")
                if (result.failure !is NoConnectionException) {
                    chunk.testErrorCause = result.failure.message
                }
                dao.saveSignalMeasurementChunk(chunk)
                throw result.failure
            }
        }
    }
}

fun CoverageMeasurementSession.isRegistered(): Boolean {
    return this.serverSessionId != null
}