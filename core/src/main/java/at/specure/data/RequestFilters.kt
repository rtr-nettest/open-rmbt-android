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

        fun createRadioInfoBody(
            cellInfoList: List<CellInfoRecord>,
            signalList: List<SignalRecord>,
            chunk: SignalMeasurementChunk
        ): RadioInfoBody? {
            var radioInfo: RadioInfoBody? = if (cellInfoList.isEmpty() && signalList.isEmpty()) {
                null
            } else {
                val cells: Map<String, CellInfoBody>? = if (cellInfoList.isEmpty()) {
                    null
                } else {
                    val map = mutableMapOf<String, CellInfoBody>()
                    cellInfoList.forEach {
                        map[it.uuid] = it.toRequest()
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
                                list.add(it.toRequest(cell.uuid, false, null))
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
                if (signals != null && (signals.size > 1) && (chunk.sequenceNumber > 0)) {
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
            var signals = signals
            var lastSignalWithNegativeTime: SignalBody? = null
            Timber.d("Previous list size: ${signals?.size} + last time: ")
            if (signals != null && (signals.size > 1)) {
                signals = signals.filter {
                    if (it.timeNanos < 0) {
                        lastSignalWithNegativeTime = it
                        false
                    } else {
                        true
                    }
                }
            }
            Timber.d("Previous list size filtered negative: ${signals?.size} + last time: ")
            // add back previously removed last value with time < 0
            lastSignalWithNegativeTime?.let {
                (signals as MutableList<SignalBody>).add(0, it)
            }
            return signals
        }
    }
}