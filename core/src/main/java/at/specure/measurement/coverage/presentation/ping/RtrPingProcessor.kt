package at.specure.measurement.coverage.presentation.ping

import at.specure.client.PingClientConfiguration
import at.specure.client.PingResult
import at.specure.client.UdpHmacPingFlow
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.eval.PingEvaluator
import at.specure.eval.PingStats
import at.specure.measurement.coverage.domain.models.PingData
import at.specure.measurement.coverage.domain.PingProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Singleton

private const val PING_INTERVAL_MILLIS: Long = 100
private const val PING_TIMEOUT_MILLIS: Long = 2000
private const val PING_PROTOCOL_HEADER: String = "RP01"
private const val PING_PROTOCOL_SUCCESS_RESPONSE_HEADER: String = "RR01"
private const val PING_PROTOCOL_ERROR_RESPONSE_HEADER: String = "RE01"
private const val PING_EVALUATE_LAST_N_ITEMS: Int = 10

@Singleton
class RtrPingProcessor : PingProcessor {

    private var pingEvaluator: PingEvaluator? = null
    private var pingClient: UdpHmacPingFlow? = null
    private var pingJob: Job? = null
    private val debug = true

    private val pingDataFlow = MutableSharedFlow<PingData>(replay = 0)

    @OptIn(FlowPreview::class)
    override suspend fun startPing(coverageMeasurementSession: CoverageMeasurementSession): Flow<PingData> {
        val pingHost = coverageMeasurementSession.pingServerHost
        val pingPort = coverageMeasurementSession.pingServerPort
        val pingToken = coverageMeasurementSession.pingServerToken

        if (pingHost == null || pingPort == null || pingToken == null) {
            pingDataFlow.emit(PingData(null, IllegalStateException("Ping host, port, or token is null")))
            return pingDataFlow
        }

        val configuration = PingClientConfiguration(
            host = pingHost,
            port = pingPort,
            token = pingToken,
            protocolId = PING_PROTOCOL_HEADER,
            pingIntervalMillis = PING_INTERVAL_MILLIS,
            pingTimeoutMillis = PING_TIMEOUT_MILLIS,
            successResponseHeader = PING_PROTOCOL_SUCCESS_RESPONSE_HEADER,
            errorResponseHeader = PING_PROTOCOL_ERROR_RESPONSE_HEADER
        )

        if (configuration != pingClient?.configuration) {
            pingEvaluator?.cancel()
            pingClient = UdpHmacPingFlow(configuration)
            pingEvaluator = PingEvaluator(pingClient!!.pingFlow())

            pingJob?.cancel()
            // Start collecting and emitting to the hot flow
            pingJob = CoroutineScope(Dispatchers.IO).launch {
                pingEvaluator?.start()
                    ?.onEach {
                        if (it is PingResult.ServerError) {
                            Timber.e(it.exception, "Server error in ping flow")
                            pingDataFlow.emit(PingData(getCurrentPingStats(), it.exception))
                        }
                    }
                    ?.sample(1000)
                    ?.retryWhen { cause, attempt ->
                        Timber.e(cause, "Error in ping flow, restarting attempt #$attempt")
                        delay(1000)
                        true
                    }
                    ?.catch { e ->
                        Timber.e(e, "Error in ping flow after retries")
                        pingDataFlow.emit(PingData(null, e))
                    }
                    ?.collect {
                        pingDataFlow.emit(PingData(getCurrentPingStats(), null))
                    }
            }
        }

        return pingDataFlow
    }


    override suspend fun stopPing(): PingStats? {
        val results = pingEvaluator?.evaluateAndStop()
        pingJob?.cancel()
        return results
    }

    override suspend fun getCurrentPingStats(): PingStats? {
        val pingStats = pingEvaluator?.evaluateLastItems(PING_EVALUATE_LAST_N_ITEMS)
        if (debug) {
            println("Ping stats for ${PING_EVALUATE_LAST_N_ITEMS} items: ${pingStats}")
        }
        return pingStats
    }

    override suspend fun onNewFenceStarted(): PingStats? {
        return pingEvaluator?.evaluateAndReset()
    }
}
