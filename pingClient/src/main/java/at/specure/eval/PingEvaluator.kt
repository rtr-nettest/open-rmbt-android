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
    private val debugLog = false

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
                            is PingResult.ClientError -> println("❌ Ping ${result.sequenceNumber} - ${result.exception}") // TODO: restart ping on this
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
     * Adds a ping result in an ordered manner by sequence number,
     * handling overflow and replacing earlier Lost/Timeout with later Success.
     */
    fun addResult(result: PingResult?) {
        if (result == null) return

        val seq = when (result) {
            is PingResult.Success -> result.sequenceNumber to result.rttMillis
            is PingResult.Lost -> result.sequenceNumber to null
            is PingResult.ServerError -> result.sequenceNumber to null
            is PingResult.ClientError -> result.sequenceNumber to null
        }

        val seqNumber = seq.first

        // Check for existing entry with the same sequence number
        val existingIndex = results.indexOfFirst { it.first == seqNumber }
        if (existingIndex >= 0) {
            // Replace Lost/Timeout with Success if applicable
            val existing = results[existingIndex]
            if (existing.second == null && seq.second != null) {
                results[existingIndex] = seq
            }
            return
        }

        // Find insertion point by comparing sequence number relative to last item
        if (results.isEmpty() || isNewer(seqNumber, results.last().first)) {
            results.addLast(seq) // append to end
        } else {
            // Insert in correct position
            val insertIndex = results.indexOfFirst { isNewer(seq.first, it.first) }
            if (insertIndex >= 0) {
                results.add(insertIndex, seq)
            } else {
                results.addFirst(seq)
            }
        }

        // Keep list size within maxResultsSize
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

    /**
     * Returns true if 'a' is a newer sequence than 'b', taking overflow into account.
     */
    private fun isNewer(a: Int, b: Int): Boolean {
        val diff = a.toLong() - b.toLong()
        return when {
            diff > Int.MAX_VALUE / 2 -> false // a is older due to overflow
            diff < -Int.MAX_VALUE / 2 -> true // a is newer due to overflow
            else -> diff > 0
        }
    }
}
