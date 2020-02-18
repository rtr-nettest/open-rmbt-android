package at.specure.data.repository

import android.content.Context
import at.rmbt.client.control.ControlServerClient
import at.rmbt.client.control.GetSyncCodeBody
import at.rmbt.client.control.SyncDevicesBody
import at.specure.data.ClientUUID
import at.specure.test.DeviceInfo
import at.specure.util.exception.DataMissingException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class DeviceSyncRepositoryImpl(
    private val context: Context,
    private val client: ControlServerClient,
    private val clientUUID: ClientUUID,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository
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
            if (it.sync.isNotEmpty()) {
                emit(it.sync.first().syncCode)
            } else {
                throw DataMissingException("Sync Code is missing")
            }
        }

        result.onFailure { throw it }
    }

    override fun syncDevices(otherDeviceSyncCode: String): Flow<DeviceSyncRepository.SyncDeviceResult> = flow {
        val uuid = clientUUID.value
        if (uuid == null) {
            Timber.w("Unable to update history client uuid is null")
            throw DataMissingException("ClientUUID is null")
        }

        val deviceInfo = DeviceInfo(context)
        val result = client.syncDevices(SyncDevicesBody(uuid, deviceInfo.language, otherDeviceSyncCode))

        result.onSuccess {
            if (it.sync.isNotEmpty()) {
                settingsRepository.refreshSettings()
                historyRepository.getHistorySource()
                val entry = it.sync.first()
                emit(DeviceSyncRepository.SyncDeviceResult(entry.messageTitle, entry.messageText, entry.success))
            } else {
                throw DataMissingException("Sync Result is missing")
            }
        }

        result.onFailure { throw it }
    }
}