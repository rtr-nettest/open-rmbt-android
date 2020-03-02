package at.specure.data.repository

interface MeasurementRepository {

    fun saveTelephonyInfo(uuid: String)

    fun saveWlanInfo(uuid: String)

    fun saveCapabilities(uuid: String)

    fun savePermissionsStatus(uuid: String)
}