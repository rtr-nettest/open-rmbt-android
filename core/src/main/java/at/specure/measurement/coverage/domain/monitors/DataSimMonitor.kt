package at.specure.measurement.coverage.domain.monitors

import kotlinx.coroutines.flow.StateFlow

interface DataSimMonitor {
    val activeDataSim: StateFlow<Int?>

    fun start()

    fun stop()

    fun getCurrentDefaultDataSimId(): Int?

}