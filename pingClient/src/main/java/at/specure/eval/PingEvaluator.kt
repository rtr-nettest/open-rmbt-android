package at.specure.eval

import at.specure.client.PingResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToLong

/**
 * Collects results to a list and when evaluateAndReset is called it returns statistic and clears the list for the next list of ping values
 */
class PingEvaluator(private val pingFlow: Flow<PingResult>) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    private val results = mutableListOf<Long?>()
    private val mutex = Mutex()

    /**
     * Starts collecting ping results. If already collecting, does nothing.
     */
    fun start() {
        if (job?.isActive == true) {
            println("✅ Ping job already running")
            return
        } // already running
        println("✅ Ping job starting")
        results.clear()
        job = scope.launch {
            pingFlow.collect { result ->
                mutex.withLock {
                    results.add(result.getRTTMillis())
//                    when (result) {
//                        is PingResult.Success -> results.add(result.rttMillis)
//                        is PingResult.Lost -> results.add(null)
//                        is PingResult.ClientError -> results.add(null)
//                        is PingResult.ServerError -> results.add(null)
//                    }
                    when (result) {
                        is PingResult.Success -> println("✅ Ping ${result.sequenceNumber} - RTT: ${result.rttMillis} ms")
                        is PingResult.Lost -> println("⚠️  Ping ${result.sequenceNumber} - Timeout")
                        is PingResult.ClientError -> println("❌ Ping ${result.sequenceNumber} - ${result.exception}")
                        is PingResult.ServerError -> println("❌ Ping ${result.sequenceNumber} - Server error")
                    }
                }
            }
        }
    }

    /**
     * Evaluates collected results and clears them for new measurements.
     */
    suspend fun evaluateAndReset(): PingStats {
        val snapshot: List<Long?> = mutex.withLock {
            val copy = results.toList()
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
        val snapshot: List<Long?> = mutex.withLock {
            val copy = results.toList()
            results.clear()
            copy
        }
        return calculateStats(snapshot)
    }

    /**
     * Calculate all stats from the given list of values.
     */
    private fun calculateStats(values: List<Long?>): PingStats {
        if (values.isEmpty()) return PingStats(average = null, median = null, totalCountWithoutNulls = 0, totalCountWithNulls = 0)

        val numericValues = values.filterNotNull()

        val average = if (numericValues.isEmpty()) null
        else numericValues.average().roundToLong()

        val median = if (numericValues.isEmpty()) null
        else {
            val sorted = numericValues.sorted()
            val mid = sorted.size / 2
            if (sorted.size % 2 == 0) {
                ((sorted[mid - 1] + sorted[mid]) / 2.0).roundToLong()
            } else {
                sorted[mid]
            }
        }

        return PingStats(
            average = average,
            median = median,
            totalCountWithNulls = values.size,
            totalCountWithoutNulls = values.filterNotNull().size
        )
    }

}
