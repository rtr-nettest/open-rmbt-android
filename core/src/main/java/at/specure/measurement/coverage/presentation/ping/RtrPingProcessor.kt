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
import kotlinx.coroutines.cancelAndJoin
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
import kotlin.time.Duration.Companion.milliseconds

private const val PING_INTERVAL_MILLIS: Long = 100
private const val PING_TIMEOUT_MILLIS: Long = 2000
private const val PING_PROTOCOL_HEADER: String = "RP01"
private const val PING_PROTOCOL_SUCCESS_RESPONSE_HEADER: String = "RR01"
private const val PING_PROTOCOL_ERROR_RESPONSE_HEADER: String = "RE01"
private const val PING_EVALUATE_LAST_N_ITEMS: Int = 10

/**
 * Number of consecutive server errors after which the ping flow is force-restarted
 * (fresh socket + fresh evaluator state), since a [at.specure.client.PingResult.ServerError]
 * is not an exception and would otherwise never trigger [kotlinx.coroutines.flow.retryWhen].
 */
private const val PING_CONSECUTIVE_ERROR_RESTART_THRESHOLD: Int = 5

/**
 * Thrown internally to force a restart of the ping flow when the server keeps responding
 * with errors (e.g. a stale/invalid token from a previous session), so the flow doesn't
 * keep reporting null pings indefinitely.
 */
private class PingConsecutiveServerErrorsException(count: Int) :
    Exception("Restarting ping flow after $count consecutive server errors")

@Singleton
class RtrPingProcessor : PingProcessor {

    private var pingEvaluator: PingEvaluator? = null
    private var pingClient: UdpHmacPingFlow? = null
    private var pingJob: Job? = null
    private var currentSessionId: String? = null
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

        val sessionChanged = coverageMeasurementSession.localMeasurementId != currentSessionId

        if (sessionChanged || configuration != pingClient?.configuration || pingJob?.isActive != true) {
            pingEvaluator?.cancel()
            pingClient = UdpHmacPingFlow(configuration)
            pingEvaluator = PingEvaluator(pingClient!!.pingFlow())
            currentSessionId = coverageMeasurementSession.localMeasurementId

            pingJob?.cancelAndJoin()
            // Start collecting and emitting to the hot flow
            pingJob = CoroutineScope(Dispatchers.IO).launch {
                var consecutiveServerErrors = 0
                pingEvaluator?.start()
                    ?.onEach {
                        if (it is PingResult.ServerError) {
                            consecutiveServerErrors++
                            Timber.e(it.exception, "Server error in ping flow (consecutive: $consecutiveServerErrors)")
                            pingDataFlow.emit(PingData(getCurrentPingStats(), it.exception))
                            if (consecutiveServerErrors >= PING_CONSECUTIVE_ERROR_RESTART_THRESHOLD) {
                                consecutiveServerErrors = 0
                                throw PingConsecutiveServerErrorsException(PING_CONSECUTIVE_ERROR_RESTART_THRESHOLD)
                            }
                        } else {
                            consecutiveServerErrors = 0
                        }
                    }
                    ?.sample(1000.milliseconds)
                    ?.retryWhen { cause, attempt ->
                        Timber.e(cause, "Error in ping flow, restarting attempt #$attempt")
                        delay(1000.milliseconds)
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
        pingJob?.cancelAndJoin()
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
        val pingStats = pingEvaluator?.evaluateAndReset()
        if (debug) {
            println("Ping stats RESET for ${PING_EVALUATE_LAST_N_ITEMS} items: ${pingStats}")
        }
        return pingStats
    }
}
