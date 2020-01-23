package at.specure.data.repository

import android.content.Context
import at.rmbt.client.control.ControlServerClient
import at.rmbt.client.control.GetSyncCodeBody
import at.specure.data.ClientUUID
import at.specure.data.CoreDatabase
import at.specure.test.DeviceInfo
import at.specure.util.exception.DataMissingException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class DeviceSyncRepositoryImpl(
    private val context: Context,
    db: CoreDatabase,
    private val client: ControlServerClient,
    private val clientUUID: ClientUUID
) : DeviceSyncRepository {

    override fun getDeviceSyncCode(): Flow<String> = flow {
        val uuid = clientUUID.value
        if (uuid == null) {
            Timber.w("Unable to update history client uuid is null")
            throw DataMissingException("ClientUUID is null")
        }
        val deviceInfo = DeviceInfo(context)
        val result = client.getDeviceSyncCode(GetSyncCodeBody(uuid, deviceInfo.language))
        result.onSuccess {

            emit(it.sync.first().syncCode) // TODO check it
        }

        result.onFailure { throw it }
    }

    override fun syncDevices(otherDeviceSyncCode: String): Flow<DeviceSyncRepository.SyncDeviceResult> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}