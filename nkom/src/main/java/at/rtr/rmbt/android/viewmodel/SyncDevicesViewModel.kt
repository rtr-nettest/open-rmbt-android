package at.rtr.rmbt.android.viewmodel

import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.android.ui.viewstate.SyncDeviceViewState
import at.rtr.rmbt.android.util.safeOffer
import at.specure.data.repository.DeviceSyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

class SyncDevicesViewModel @Inject constructor(private val repository: DeviceSyncRepository) : BaseViewModel() {

    val state = SyncDeviceViewState().also {
        addStateSaveHandler(it)
    }

    val loadingChannel = Channel<Boolean>(Channel.CONFLATED)
    val pageChannel = Channel<SyncPage>(Channel.CONFLATED)
    val completedChannel = Channel<Unit>(Channel.CONFLATED)

    fun getSyncCode() = launch {
        loadingChannel.safeOffer(true)
        repository.getDeviceSyncCode()
            .flowOn(Dispatchers.IO)
            .catch {
                loadingChannel.safeOffer(false)
                state.currentDeviceSyncCode.set(null)
                if (it is HandledException) {
                    postError(it)
                } else {
                    throw it
                }
            }
            .collect {
                loadingChannel.safeOffer(false)
                state.currentDeviceSyncCode.set(it)
            }
    }

    fun syncDevices() = launch {
        loadingChannel.safeOffer(true)
        repository.syncDevices(state.otherDeviceSyncCode.get()!!)
            .flowOn(Dispatchers.IO)
            .catch {
                loadingChannel.safeOffer(false)
                state.currentDeviceSyncCode.set(null)
                if (it is HandledException) {
                    postError(it)
                } else {
                    throw it
                }
            }
            .collect {
                loadingChannel.safeOffer(false)
                completedChannel.safeOffer(Unit)
            }
    }

    fun showRequest() {
        pageChannel.safeOffer(SyncPage.REQUEST)
    }

    fun showEnter() {
        pageChannel.safeOffer(SyncPage.ENTER)
    }
}

enum class SyncPage {
    STARTER, REQUEST, ENTER
}