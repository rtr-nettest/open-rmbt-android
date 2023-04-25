package at.specure.data.repository

interface MeasurementRepository {

    fun saveTelephonyInfo(uuid: String)

    fun saveWlanInfo(uuid: String)

    fun saveCapabilities(uuid: String?, signalChunkId: String?)

    fun savePermissionsStatus(uuid: String?, signalChunkId: String?)
}