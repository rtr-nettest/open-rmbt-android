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
            localMeasurementId = record.id,
            serverMeasurementId = newUuid,
        )
        .also {
            dao.saveDedicatedSignalMeasurementSession(it)
        }
    }

    override fun saveAndRegisterRecord(record: SignalMeasurementRecord) = io {
        dao.saveSignalMeasurementRecord(record)
        // TODO: Here is the logic when signalMeasurementRecord returns new uuid move this logic to session manager
        registerCoverageMeasurement(localMeasurementId = record.id)
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

    override fun saveCoverageMeasurementSession(session: CoverageMeasurementSession) {
        dao.saveDedicatedSignalMeasurementSession(session)
    }

    override fun getCoverageMeasurementSession(measurementId: String): CoverageMeasurementSession? {
        return dao.getCoverageMeasurementSessionForMeasurementId(measurementId)
    }

    override fun saveMeasurementPointRecord(point: CoverageMeasurementFenceRecord) = io {
        dao.saveSignalMeasurementPoint(point)
    }

    override fun loadSignalMeasurementPointRecordsForMeasurement(measurementId: String): LiveData<List<CoverageMeasurementFenceRecord>> {
        return dao.getCoverageMeasurementFences(measurementId)
    }

    override fun loadSignalMeasurementPointRecordsForMeasurementList(measurementId: String): List<CoverageMeasurementFenceRecord> {
        return dao.getCoverageMeasurementFencesList(measurementId)
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
    override fun registerCoverageMeasurement(localMeasurementId: String): Flow<Boolean> = flow {

        val clientUUID = clientUUID.value ?: throw DataMissingException("Missing client UUID")
        val coverageSession =
            retrieveCoverageMeasurementOrCreate(localMeasurementId) // if not created yet, we create one for registration
        val deviceInfo = deviceInfo ?: throw DataMissingException("Missing device info")
        val body = coverageSession.toCoverageRequest(clientUUID, deviceInfo, config)

//        val record = dao.getSignalMeasurementRecord(measurementId) ?: throw DataMissingException("Measurement record $measurementId is missing")

        val response = client.coverageRequest(body)
        response.onSuccess {
            Timber.d("$it")
            val responseReceivedTime = System.currentTimeMillis()
            dao.saveDedicatedSignalMeasurementSession(
                session = CoverageMeasurementSession(
                    localMeasurementId = coverageSession.localMeasurementId,
                    localLoopId = coverageSession.localLoopId,
                    serverMeasurementId = it.testUUID,
                    serverSessionLoopId = if (coverageSession.isFirstMeasurementInLoop()) it.testUUID else coverageSession.serverSessionLoopId, // TODO: it.loopUUID will be done"
                    pingServerHost = it.pingHost,
                    pingServerPort = it.pingPort.toIntOrNull() ?: -1,
                    pingServerToken = it.pingToken,
                    ipVersion = it.ipVersion,
                    remoteIpAddress = it.clientRemoteIp,
                    provider = it.provider,
                    startTimeMeasurementMillis = coverageSession.startTimeMeasurementMillis,
                    startMeasurementResponseReceivedMillis = responseReceivedTime,
                    startTimeLoopMillis = coverageSession.startTimeLoopMillis,
                    startLoopResponseReceivedMillis = if (coverageSession.isFirstMeasurementInLoop()) responseReceivedTime else coverageSession.startLoopResponseReceivedMillis,
                    maxCoverageMeasurementSeconds = it.maxCoverageMeasurementSeconds,
                    maxCoverageLoopSeconds = it.maxCoverageSessionSeconds,
                    sequenceNumber = coverageSession.sequenceNumber,
                )
            )
        }

        if (response.ok) {
            emit(true)
        } else {
            throw response.failure
        }

    }

    private fun retrieveCoverageMeasurementOrCreate(localMeasurementId: String?): CoverageMeasurementSession {
        val coverageSession =
            if (localMeasurementId == null) {
                null
            } else {
                val loadedMeasurement = dao.getCoverageMeasurementSessionForMeasurementId(localMeasurementId)
                // todo: check if measurement is still in max times defined or we are gonna create a new one
                loadedMeasurement
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
        val session = dao.getCoverageMeasurementSessionForMeasurementId(chunk.measurementId)
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
                    if (!it.isNullOrEmpty() && it != session.serverMeasurementId) {
                        callBack.newUUIDSent(it, session)
                    }
                }
            }
    }

    override suspend fun sendFences(localMeasurementId: String) {
        // todo: update times before sending the fences (regarding real time of coverageRequest response arrival time)
        val coverageSession = retrieveCoverageMeasurementOrCreate(localMeasurementId)
        if (coverageSession.isRegistered()) {
            val fencesForSession = dao.getCoverageMeasurementFencesList(coverageSession.localMeasurementId)
            clientUUID.value?.let {clientUuid ->
                fencesForSession.let { fences ->
                    if (fences.isNotEmpty()) {
                        val requestBody = coverageSession.toCoverageResultRequest(clientUuid, deviceInfo, config, fencesForSession)
                        val result = client.coverageResult(requestBody)
                        if (result.ok) {
                            dao.markSessionAsSynced(localMeasurementId)
                        } else {
                            dao.incrementRetryCountForSession(localMeasurementId)
                            // TODO: enqueue sending with worker in case of failed send
                        }
                    }
                }
            }
        }
    }

    override suspend fun retrySendFences() {
        val measurements = dao.getCoverageMeasurementsForRetrySend()
        measurements.forEach {coverageSessionMeasurement ->
            val fencesForSession = dao.getCoverageMeasurementFencesList(coverageSessionMeasurement.localMeasurementId)
            clientUUID.value?.let {clientUuid ->
                fencesForSession.let { fences ->
                    if (fences.isNotEmpty()) {
                        val requestBody = coverageSessionMeasurement.toCoverageResultRequest(clientUuid, deviceInfo, config, fencesForSession)
                        val result = client.coverageResult(requestBody)
                        if (result.ok) {
                            dao.markSessionAsSynced(coverageSessionMeasurement.localMeasurementId)
                        } else {
                            dao.incrementRetryCountForSession(coverageSessionMeasurement.localMeasurementId)
                        }
                    }
                    // TODO: enqueue sending with worker in case of failed send
                }
            }
        }
    }

    override suspend fun removeOldFencesAndSessions() {
        dao.deleteDeletableSessions()
    }

    /**
     *  TODO: add scenario when measurement chunk response with other uuid than it is already in the session
     */
    override fun sendMeasurementChunk(chunkId: String, callback: SignalMeasurementChunkResultCallback): Flow<String?> = flow {
        var chunk = dao.getSignalMeasurementChunk(chunkId) ?: throw DataMissingException("SignalMeasurementChunk not found with id: $chunkId")
        val record = dao.getSignalMeasurementRecord(chunk.measurementId)
            ?: throw DataMissingException("SignalMeasurementRecord not found with id: ${chunk.measurementId}")

        var session = dao.getCoverageMeasurementSessionForMeasurementId(record.id)

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
                measurementInfoUUID = session?.serverMeasurementId,
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
                Timber.d("SM Chunk OK responded with uuid: ${result.success.uuid}   before: ${session?.serverMeasurementId}")
                if (session == null) {
                    session = CoverageMeasurementSession(
                        localLoopId = record.id,
                        serverMeasurementId = result.success.uuid,
                        remoteIpAddress = "", // TODO need to fill that field
                        provider = "", // TODO need to fill that field
                        sequenceNumber = 0,
                    )
                    session?.let { signalMeasurementSession ->
                        dao.saveDedicatedSignalMeasurementSession(signalMeasurementSession)
                    }

                } else {
                    if (result.success.uuid.isNotEmpty() && result.success.uuid != session?.serverMeasurementId) {
                        Timber.d("SM Chunk creating new chunk with uuid: ${result.success.uuid}   before: ${session?.serverMeasurementId}")
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
                Timber.d("SM Chunk FAILED responded: ${session?.serverMeasurementId}")
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
    return this.serverMeasurementId != null
}