package at.rtr.rmbt.android.viewmodel

import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.android.ui.viewstate.SyncDeviceViewState
import at.specure.data.repository.DeviceSyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

class SyncDevicesViewModel @Inject constructor(private val repository: DeviceSyncRepository) : BaseViewModel() {

    val state = SyncDeviceViewState().also {
        addStateSaveHandler(it)
    }

    fun getSyncCode() = launch {
        repository.getDeviceSyncCode()
            .flowOn(Dispatchers.IO)
            .catch {
                state.currentDeviceSyncCode.set(null)
                if (it is HandledException) {
                    // TODO add error handling for the sync dialog
                    postError(it)
                } else {
                    throw it
                }
            }
            .collect {
                state.currentDeviceSyncCode.set(it)
            }
    }
}