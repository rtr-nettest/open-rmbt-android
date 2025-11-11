package at.specure.measurement.coverage.presentation

import at.specure.client.PingClientConfiguration
import at.specure.client.UdpHmacPingFlow
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.eval.PingEvaluator
import at.specure.eval.PingStats
import at.specure.measurement.coverage.domain.models.PingData
import at.specure.measurement.coverage.domain.PingProcessor
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.sample
import timber.log.Timber
import javax.inject.Singleton

private const val PING_INTERVAL_MILLIS: Long = 100
private const val PING_TIMEOUT_MILLIS: Long = 2000
private const val PING_PROTOCOL_HEADER: String = "RP01"
private const val PING_PROTOCOL_SUCCESS_RESPONSE_HEADER: String = "RR01"
private const val PING_PROTOCOL_ERROR_RESPONSE_HEADER: String = "RE01"
private const val PING_EVALUATE_LAST_N_ITEMS: Int = 10

@Singleton
class RtrPingProcessor: PingProcessor {

    private var pingEvaluator: PingEvaluator? = null
    private val debug = true

    @OptIn(FlowPreview::class)
    override suspend fun startPing(coverageMeasurementSession: CoverageMeasurementSession): Flow<PingData> = flow {
        val pingHost = coverageMeasurementSession.pingServerHost
        val pingPort = coverageMeasurementSession.pingServerPort
        val pingToken = coverageMeasurementSession.pingServerToken

        if (pingHost == null || pingPort == null || pingToken == null) {
            emit(
                PingData(
                    pingStatistics = null,
                    error = IllegalStateException("Ping host, port, or token is null")
                )
            )
            return@flow
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

        pingEvaluator = PingEvaluator(UdpHmacPingFlow(configuration).pingFlow())
        pingEvaluator?.start()
            ?.sample(1000)
            ?.retryWhen { cause, attempt ->
                Timber.e(cause, "Error in ping flow, restarting attempt #$attempt")
                true // retry on every exception
            }
            ?.catch { e ->
                // This will catch any remaining exceptions that retry didn't handle
                Timber.e(e, "Error in ping flow after retries")
                emit(
                    PingData(
                        pingStatistics = null,
                        error = e
                    )
                )
            }
            ?.collect { pingResult ->
                emit(
                    PingData(
                        pingStatistics = getCurrentPingStats(),
                        error = null
                    )
                )
            }
    }

    override suspend fun stopPing(): PingStats? {
        return pingEvaluator?.evaluateAndStop()
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
