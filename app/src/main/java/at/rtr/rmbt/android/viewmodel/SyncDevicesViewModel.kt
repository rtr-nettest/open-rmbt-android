package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

    private val _loadingLiveData = MutableLiveData<Boolean>()
    private val _getSyncCodeLiveData = MutableLiveData<String>()
    private val _syncDevicesLiveData = MutableLiveData<DeviceSyncRepository.SyncDeviceResult>()

    val loadingLiveData: LiveData<Boolean>
        get() = _loadingLiveData

    val syncDevicesLiveData: LiveData<DeviceSyncRepository.SyncDeviceResult>
        get() = _syncDevicesLiveData

    val getSyncCodeLiveData: LiveData<String>
        get() = _getSyncCodeLiveData

    fun getSyncCode() = launch {
        _loadingLiveData.postValue(true)
        repository.getDeviceSyncCode()
            .flowOn(Dispatchers.IO)
            .catch {
                _loadingLiveData.postValue(false)
                state.currentDeviceSyncCode.set(null)
                if (it is HandledException) {
                    postError(it)
                } else {
                    throw it
                }
            }
            .collect {
                _loadingLiveData.postValue(false)
                _getSyncCodeLiveData.postValue(it)
            }
    }

    fun syncDevices() = launch {
        _loadingLiveData.postValue(true)
        repository.syncDevices(state.otherDeviceSyncCode.get()!!)
            .flowOn(Dispatchers.IO)
            .catch {
                _loadingLiveData.postValue(false)
                state.currentDeviceSyncCode.set(null)
                if (it is HandledException) {
                    postError(it)
                } else {
                    throw it
                }
            }
            .collect {
                _loadingLiveData.postValue(false)
                _syncDevicesLiveData.postValue(it)
            }
    }
}