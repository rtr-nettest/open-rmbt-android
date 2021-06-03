package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.MutableLiveData
import at.rmbt.client.control.Server
import at.rtr.rmbt.android.util.liveDataOf
import at.specure.data.MeasurementServers
import javax.inject.Inject

class MeasurementServerSelectionViewModel @Inject constructor(private val servers: MeasurementServers) : BaseViewModel() {

    val measurementServers =  servers.measurementServers
    val selected = MutableLiveData<Server?>().also { it.value = servers.selectedMeasurementServer }

    fun markAsSelected(server: Server) {
        servers.selectedMeasurementServer = server
        selected.postValue(server)
    }

}