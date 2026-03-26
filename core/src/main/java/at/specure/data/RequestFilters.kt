package at.specure.data

import at.rmbt.client.control.CellInfoBody
import at.rmbt.client.control.RadioInfoBody
import at.rmbt.client.control.SignalBody
import at.specure.data.entity.CellInfoRecord
import at.specure.data.entity.SignalMeasurementChunk
import at.specure.data.entity.SignalRecord
import timber.log.Timber
import kotlin.math.abs

class RequestFilters {

    companion object {

        fun createRadioInfoBodyFast(
            cellInfoList: List<CellInfoRecord>,
            signalList: List<SignalRecord>,
            chunk: SignalMeasurementChunk?,
            useComparisonCellUuid: Boolean = false
        ): RadioInfoBody? {

            if (cellInfoList.isEmpty() && signalList.isEmpty()) return null

            // ---- CELLS (build once, O(n)) ----
            val cellsMap: MutableMap<String, CellInfoBody>? =
                if (cellInfoList.isEmpty()) {
                    null
                } else {
                    val map = HashMap<String, CellInfoBody>(cellInfoList.size)
                    for (cell in cellInfoList) {
                        val key = if (useComparisonCellUuid) cell.comparisonUuid else cell.uuid
                        map[key] = cell.toRequest()
                    }
                    if (map.isEmpty()) null else map
                }

            // ---- SIGNALS (single-pass, optimized) ----
            var signalsResult: MutableList<SignalBody>? = null

            if (!signalList.isEmpty() && cellsMap != null) {
                val result = ArrayList<SignalBody>(signalList.size)

                // For distinctBy(timeNanos, networkTypeId)
                val seen = HashSet<Pair<Long, Int>>(signalList.size)

                var lastNegative: SignalBody? = null

                val applyChunkFilter = chunk != null && chunk.sequenceNumber > 0

                for (i in signalList.indices) {
                    val signalRecord = signalList[i]

                    val cell = cellsMap[signalRecord.cellUuid] ?: continue

                    val signal = signalRecord.toRequest(cell.uuid, null)

                    // ---- negative timestamp handling ----
                    if (signal.timeNanos < 0) {
                        lastNegative = signal
                        continue
                    }

                    // ---- 60s filtering (only if needed) ----
                    if (applyChunkFilter && i < signalList.lastIndex) {
                        val next = signalList[i + 1]
                        val diff = kotlin.math.abs(next.timeNanos) - kotlin.math.abs(signal.timeNanos)
                        if (diff > 60_000_000_000) {
                            continue
                        }
                    }

                    // ---- distinctBy ----
                    val key:Pair<Long, Int> = signal.timeNanos to (signal.networkTypeId ?: 0)
                    if (seen.add(key)) {
                        result.add(signal)
                    }
                }

                // ---- re-add last negative timestamp ----
                if (lastNegative != null) {
                    result.add(0, lastNegative)
                }

                if (result.isNotEmpty()) {
                    signalsResult = result
                }
            }

            return RadioInfoBody(
                cellsMap?.values?.toList(),
                signalsResult
            )
        }

        fun createRadioInfoBody(
            cellInfoList: List<CellInfoRecord>,
            signalList: List<SignalRecord>,
            chunk: SignalMeasurementChunk?,
            useComparisonCellUuid: Boolean = false
        ): RadioInfoBody? {
            var radioInfo: RadioInfoBody? = if (cellInfoList.isEmpty() && signalList.isEmpty()) {
                null
            } else {
                val cells: Map<String, CellInfoBody>? = if (cellInfoList.isEmpty()) {
                    null
                } else {
                    val map = mutableMapOf<String, CellInfoBody>()
                    cellInfoList.forEach {
                        if (useComparisonCellUuid) {
                            map[it.comparisonUuid] = it.toRequest()
                        } else {
                            map[it.uuid] = it.toRequest()
                        }
                    }
                    if (map.isEmpty()) null else map
                }

                var signals: List<SignalBody>? = if (signalList.isEmpty()) {
                    null
                } else {
                    val list = mutableListOf<SignalBody>()
                    if (cells == null) {
                        null
                    } else {
                        signalList.forEach {
                            val cell = cells[it.cellUuid]
                            if (cell != null) {
                                list.add(it.toRequest(cell.uuid,  null))
                            }
//                            else {
//                                list.add(it.toRequest("", false, null))
//                            }
                        }
                        if (list.isEmpty()) null else list
                    }
                }
                Timber.i("Old list size: ${signals?.size}")
                // remove last signal from previous chunk
                if (signals != null && (signals.size > 1) && (chunk != null && chunk.sequenceNumber > 0)) {
                    signals = signals.filterIndexed { index, it ->
                        if (index == signals?.lastIndex) {
                            true
                        } else {
                            val newValueUnder60s = abs(signals!![index + 1].timeNanos) - abs(it.timeNanos) <= 60000000000
                            if (newValueUnder60s) {
                                true
                            } else {
                                Timber.i("Filtered out: $it")
                                false
                            }
                        }
                    }
                }

                signals = removeOldRedundantSignalValuesWithNegativeTimestamp(signals)?.distinctBy { listOf(it.timeNanos, it.networkTypeId) }

                Timber.i("New list size: ${signals?.size} + last time: ")

                RadioInfoBody(cells?.entries?.map { it.value }, signals)
            }
            return radioInfo
        }

        fun removeOldRedundantSignalValuesWithNegativeTimestamp(signals: List<SignalBody>?): List<SignalBody>? {
            // remove values with negative timestamp and keep only last one
            var signalsLocal = signals
            var lastSignalWithNegativeTime: SignalBody? = null
            Timber.d("Previous list size: ${signalsLocal?.size} + last time: ")
            if (signalsLocal != null && (signalsLocal.size > 1)) {
                signalsLocal = signalsLocal.filter {
                    if (it.timeNanos < 0) {
                        lastSignalWithNegativeTime = it
                        false
                    } else {
                        true
                    }
                }
            }
            Timber.d("Previous list size filtered negative: ${signalsLocal?.size} + last time: ")
            // add back previously removed last value with time < 0
            lastSignalWithNegativeTime?.let {
                (signalsLocal as MutableList<SignalBody>).add(0, it)
            }
            return signalsLocal
        }
    }
}