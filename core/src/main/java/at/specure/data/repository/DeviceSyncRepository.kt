package at.specure.data.repository

import kotlinx.coroutines.flow.Flow

interface DeviceSyncRepository {

    /**
     * Get device sync code of current device
     */
    fun getDeviceSyncCode(): Flow<String>

    /**
     * Sync two devices
     * returns pair<Title, Text>
     */
    fun syncDevices(otherDeviceSyncCode: String): Flow<SyncDeviceResult>

    data class SyncDeviceResult(
        val dialogTitle: String,
        val dialogText: String,
        val success: Boolean
    )
}