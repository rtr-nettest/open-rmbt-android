package at.specure.eval

import at.specure.client.PingResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Collects results to a list and when evaluateAndReset is called it returns statistic and clears the list for the next list of ping values
 */
class PingEvaluator(private val pingFlow: Flow<PingResult>) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    private val results = ArrayDeque<Pair<Int, Double?>>()
    private val maxResultsSize = 1000
    private val mutex = Mutex()
    private val debugLog = true

    /**
     * Starts collecting ping results. If already collecting, does nothing.
     */
    fun start(): Flow<PingResult?> = channelFlow {
        if (job?.isActive == true) {
            if (debugLog) println("✅ Ping job already running")
            trySend(null)
            return@channelFlow
        }

        if (debugLog) println("✅ Ping job starting")
        results.clear()

        job = launch(CoroutineName("PingJob in evaluator")) {
            try {
                pingFlow.collect { result ->
                    mutex.withLock {
                        addResult(result)
                        if (debugLog) when (result) {
                            is PingResult.Success -> println("✅ Ping ${result.sequenceNumber} - RTT: ${result.rttMillis} ms")
                            is PingResult.Lost -> println("⚠️  Ping ${result.sequenceNumber} - Timeout")
                            is PingResult.ClientError -> println("❌ Ping ${result.sequenceNumber} - ${result.exception}")
                            is PingResult.ServerError -> println("❌ Ping ${result.sequenceNumber} - Server error")
                        }
                        trySend(result)
                    }
                }
                close()
            } catch (e: CancellationException) {
                if (debugLog) println("Ping collection job cancelled: ${e.message}")
                throw e
            } catch (e: Exception) {
                if (debugLog) println("❌ Error collecting pingFlow: ${e.message}")
                close(e)
            }
        }
    }

    /**
     * Adds a ping result in an ordered manner by sequence number.
     */
    fun addResult(result: PingResult?) {
        if (result == null) return

        val seq = when (result) {
            is PingResult.Success -> result.sequenceNumber to result.rttMillis
            is PingResult.Lost -> result.sequenceNumber to null
            is PingResult.ServerError -> result.sequenceNumber to null
            is PingResult.ClientError -> result.sequenceNumber to null
        }

        // Keep list ordered by sequence number (ascending)
        val existingIndex = results.indexOfFirst { it.first >= seq.first }
        if (existingIndex >= 0) {
            results.add(existingIndex, seq)
        } else {
            results.addLast(seq)
        }

        // Trim if too big
        while (results.size > maxResultsSize) {
            results.removeFirst()
        }

        if (debugLog) {
            println("Added result: seq=${seq.first}, rtt=${seq.second}, totalSize=${results.size}")
        }
    }

    /**
     * Evaluates collected results for latest N items.
     */
    suspend fun evaluateLastItems(count: Int): PingStats {
        val snapshot: List<Double?> = mutex.withLock {
            val lastNItems = results.takeLast(count)
            if (debugLog) {
                lastNItems.forEach { it
                    println("Ping - evaluateLastItems: ${it.first} - ${it.second}")
                }
            }
            lastNItems.map { it.second }
        }
        return calculateStats(snapshot)
    }

    /**
     * Evaluates collected results and clears them for new measurements.
     */
    suspend fun evaluateAndReset(): PingStats {
        val snapshot = mutex.withLock {
            val copy = results.map { it.second }
            results.clear()
            copy
        }
        return calculateStats(snapshot)
    }

    /**
     * Evaluates collected results, stops collecting, and clears data.
     */
    suspend fun evaluateAndStop(): PingStats {
        job?.cancelAndJoin()
        val snapshot = mutex.withLock {
            val copy = results.map { it.second }
            results.clear()
            copy
        }
        return calculateStats(snapshot)
    }

    /**
     * Calculate all stats from the given list of values.
     */
    private fun calculateStats(values: List<Double?>): PingStats {
        if (values.isEmpty()) return PingStats(average = null, median = null, totalCountWithoutNulls = 0, totalCountWithNulls = 0)

        val numericValues = values.filterNotNull()
        val average = numericValues.takeIf { it.isNotEmpty() }?.average()
        val median = numericValues.takeIf { it.isNotEmpty() }?.let {
            val sorted = it.sorted()
            val mid = sorted.size / 2
            if (sorted.size % 2 == 0)
                (sorted[mid - 1] + sorted[mid]) / 2.0
            else
                sorted[mid]
        }

        return PingStats(
            average = average,
            median = median,
            totalCountWithNulls = values.size,
            totalCountWithoutNulls = numericValues.size
        )
    }
}


/*
  I  ❌ Ping -1095230504 - java.io.IOException: sendto failed: ENETUNREACH (Network is unreachable)
2025-11-11 14:58:53.749 17169-17204 System.out              at.alladin.rmbt.android              I  ⚠️  Ping -1095230504 - Timeout
2025-11-11 14:58:53.847 17169-17326 System.out              at.alladin.rmbt.android              I  ❌ Ping -1095230503 - java.io.IOException: sendto failed: ENETUNREACH (Network is unreachable)
2025-11-11 14:58:53.851 17169-17326 System.out              at.alladin.rmbt.android              I  ⚠️  Ping -1095230503 - Timeout
2025-11-11 14:58:53.956 17169-17204 System.out              at.alladin.rmbt.android              I  ❌ Ping -1095230502 - java.io.IOException: sendto failed: ENETUNREACH (Network is unreachable)
2025-11-11 14:58:53.963 17169-17204 System.out              at.alladin.rmbt.android              I  ⚠️  Ping -1095230502 - Timeout
2025-11-11 14:58:54.061 17169-17217 System.out              at.alladin.rmbt.android              I  ❌ Ping -1095230501 - java.io.IOException: sendto failed: ENETUNREACH (Network is unreachable)
2025-11-11 14:58:54.068 17169-17217 System.out              at.alladin.rmbt.android              I  ⚠️  Ping -1095230501 - Timeout
2025-11-11 14:58:54.163 17169-17217 System.out              at.alladin.rmbt.android              I  ❌ Ping -1095230500 - java.io.IOException: sendto failed: ENETUNREACH (Network is unreachable)
2025-11-11 14:58:54.168 17169-17217 System.out              at.alladin.rmbt.android              I  ⚠️  Ping -1095230500 - Timeout
2025-11-11 14:58:54.264 17169-17204 System.out              at.alladin.rmbt.android              I  ❌ Ping -1095230499 - java.io.IOException: sendto failed: ENETUNREACH (Network is unreachable)
2025-11-11 14:58:54.265 17169-17204 System.out              at.alladin.rmbt.android              I  ⚠️  Ping -1095230499 - Timeout
2025-11-11 14:58:54.366 17169-17218 System.out              at.alladin.rmbt.android              I  ❌ Ping -1095230498 - java.io.IOException: sendto failed: ENETUNREACH (Network is unreachable)
2025-11-11 14:58:54.369 17169-17218 System.out              at.alladin.rmbt.android              I  ⚠️  Ping -1095230498 - Timeout
2025-11-11 14:58:54.468 17169-17217 System.out              at.alladin.rmbt.android              I  ❌ Ping -1095230497 - java.io.IOException: sendto failed: ENETUNREACH (Network is unreachable)
2025-11-11 14:58:54.469 17169-17217 System.out              at.alladin.rmbt.android              I  ⚠️  Ping -1095230497 - Timeout
2025-11-11 14:58:54.572 17169-17204 System.out              at.alladin.rmbt.android              I  ⚠️  Ping -1095230496 - Timeout
2025-11-11 14:58:54.609 17169-17217 System.out              at.alladin.rmbt.android              I  ✅ Ping -1095230496 - RTT: 38.261618 ms
2025-11-11 14:58:54.673 17169-17218 System.out              at.alladin.rmbt.android              I  ⚠️  Ping -1095230495 - Timeout
2025-11-11 14:58:54.711 17169-17214 System.out              at.alladin.rmbt.android              I  ✅ Ping -1095230495 - RTT: 37.446358 ms
2025-11-11 14:58:54.736 17169-17204 System.out              at.alladin.rmbt.android              I  Ping - evaluateLastItems: -1095230499 - null
2025-11-11 14:58:54.736 17169-17204 System.out              at.alladin.rmbt.android              I  Ping - evaluateLastItems: -1095230499 - null
2025-11-11 14:58:54.736 17169-17204 System.out              at.alladin.rmbt.android              I  Ping - evaluateLastItems: -1095230498 - null
2025-11-11 14:58:54.736 17169-17204 System.out              at.alladin.rmbt.android              I  Ping - evaluateLastItems: -1095230498 - null
2025-11-11 14:58:54.736 17169-17204 System.out              at.alladin.rmbt.android              I  Ping - evaluateLastItems: -1095230497 - null
2025-11-11 14:58:54.736 17169-17204 System.out              at.alladin.rmbt.android              I  Ping - evaluateLastItems: -1095230497 - null
2025-11-11 14:58:54.736 17169-17204 System.out              at.alladin.rmbt.android              I  Ping - evaluateLastItems: -1095230496 - 38.261618
2025-11-11 14:58:54.736 17169-17204 System.out              at.alladin.rmbt.android              I  Ping - evaluateLastItems: -1095230496 - null
2025-11-11 14:58:54.737 17169-17204 System.out              at.alladin.rmbt.android              I  Ping - evaluateLastItems: -1095230495 - 37.446358
2025-11-11 14:58:54.737 17169-17204 System.out              at.alladin.rmbt.android              I  Ping - evaluateLastItems: -1095230495 - null
 */