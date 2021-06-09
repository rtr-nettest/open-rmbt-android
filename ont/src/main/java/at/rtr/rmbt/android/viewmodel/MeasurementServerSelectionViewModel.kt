package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.MutableLiveData
import at.rmbt.client.control.Server
import at.specure.data.MeasurementServers
import javax.inject.Inject
import kotlin.random.Random

/**
 * Used for Home Slider.
 */
class MeasurementServerSelectionViewModel @Inject constructor(private val servers: MeasurementServers) : BaseViewModel() {

    val measurementServers = servers.measurementServers!!.map { ServerWithDistance.from(it) }
//        if (servers.measurementServers.isNullOrEmpty()) provideMockedData() else servers.measurementServers!!.map { ServerWithDistance.from(it) }
    val selected =
        MutableLiveData<ServerWithDistance?>().also {
            it.value = servers.selectedMeasurementServer?.let { it1 -> ServerWithDistance.from(it1) } ?: measurementServers.first()
        }

    fun markAsSelected(server: ServerWithDistance) {
        // todo remove mocked server
//        servers.selectedMeasurementServer = server
        selected.postValue(server)
    }

    private fun provideMockedData() = listOf(
        ServerWithDistance("CZ.NIC, Praha, CZ", "0"),
        ServerWithDistance("Specure, Frankfurt, DE", "1"),
        ServerWithDistance("Specure, Nürnberg, DE", "2"),
        ServerWithDistance("AKOS, Ljubljana, SI", "3")
    )
        .sortedBy { it.distance }
}

// todo remove mocked distance
class ServerWithDistance(val name: String?, val uuid: String?, val distance: Int = Random.nextInt(0, 500)) {
    companion object {
        fun from(server: Server) = ServerWithDistance(server.name, server.uuid)
    }
}