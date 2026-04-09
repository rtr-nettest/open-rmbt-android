package at.specure.measurement.coverage.presentation.monitors

import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import at.specure.measurement.coverage.domain.monitors.DataSimMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Singleton
class CoverageDataSimMonitor(
    private val scope: CoroutineScope,
    private val pollIntervalMs: Long = 2_000L // adjust if needed
): DataSimMonitor {

    private val _activeDataSim = MutableStateFlow<Int?>(null)
    override val activeDataSim: StateFlow<Int?> = _activeDataSim.asStateFlow()

    private var pollingJob: Job? = null

    override fun start() {
        if (pollingJob != null) return

        pollingJob = scope.launch(Dispatchers.Default) {
            var lastValue: Int? = null

            while (isActive) {
                val current = getCurrentDefaultDataSimId()
                // Emit only if changed
                if (current != lastValue) {
                    lastValue = current
                    _activeDataSim.value = current
                }
                delay(pollIntervalMs)
            }
        }
    }

    override fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun getCurrentDefaultDataSimId(): Int? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val subsId = SubscriptionManager.getDefaultDataSubscriptionId()
            if (subsId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) subsId
            else null
        } else null
}
