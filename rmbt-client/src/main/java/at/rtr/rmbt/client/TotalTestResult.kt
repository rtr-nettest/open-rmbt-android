/*******************************************************************************
 * Copyright 2013-2015 alladin-IT GmbH
 * Copyright 2013-2015 Rundfunk und Telekom Regulierungs-GmbH (RTR-GmbH)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.rtr.rmbt.client

import at.rtr.rmbt.client.helper.TestStatus
import at.rtr.rmbt.client.v2.task.service.TestMeasurement
import at.rtr.rmbt.client.v2.task.service.TestMeasurement.TrafficDirection

class TotalTestResult : TestResult() {
    var speed_upload = 0.0

    var speed_download = 0.0

    var bytes_download: Long = 0

    var nsec_download: Long = 0

    var bytes_upload: Long = 0

    var nsec_upload: Long = 0

    var totalDownBytes: Long = 0

    var totalUpBytes: Long = 0

    private var measurementMap: Map<TestStatus, TestMeasurement>? = null
    private var totalTrafficMeasurement: TestMeasurement? = null

    fun setMeasurementMap(trafficMap: Map<TestStatus, TestMeasurement>?) {
        this.measurementMap = trafficMap
    }

    fun getMeasurementMap(): Map<TestStatus, TestMeasurement>? = measurementMap

    fun setTotalTrafficMeasurement(measurement: TestMeasurement?) {
        this.totalTrafficMeasurement = measurement
    }

    fun getTotalTrafficMeasurement(dir: TrafficDirection): Long {
        val measurement = totalTrafficMeasurement ?: return 0
        return when (dir) {
            TrafficDirection.RX -> measurement.rxBytes
            TrafficDirection.TX -> measurement.txBytes
            TrafficDirection.TOTAL -> measurement.txBytes + measurement.rxBytes
        }
    }

    fun getTrafficByTestPart(testStatusPart: TestStatus, dir: TrafficDirection): Long {
        val measurement = measurementMap?.get(testStatusPart) ?: return 0
        return when (dir) {
            TrafficDirection.RX -> measurement.rxBytes
            TrafficDirection.TX -> measurement.txBytes
            TrafficDirection.TOTAL -> measurement.txBytes + measurement.rxBytes
        }
    }

    fun getTestMeasurementByTestPart(testStatusPart: TestStatus): TestMeasurement? =
        measurementMap?.get(testStatusPart)

    fun getDownloadSpeedBitPerSec(): Double =
        getSpeedBitPerSec(bytes_download, nsec_download).toDouble()

    fun getUploadSpeedBitPerSec(): Double =
        getSpeedBitPerSec(bytes_upload, nsec_upload).toDouble()

    fun calculateDownload(bytes: Array<LongArray?>, nsecs: Array<LongArray?>) {
        calculate(bytes, nsecs, false)
    }

    fun calculateUpload(bytes: Array<LongArray?>, nsecs: Array<LongArray?>) {
        calculate(bytes, nsecs, true)
    }

    private fun calculate(allBytes: Array<LongArray?>, allNsecs: Array<LongArray?>, upload: Boolean) {
        require(allBytes.size == allNsecs.size)

        val numThreads = allBytes.size

        var targetTime = Long.MAX_VALUE
        for (i in 0 until numThreads) {
            val nsecs = allNsecs[i]
            if (nsecs != null && nsecs.isNotEmpty()) {
                if (nsecs[nsecs.size - 1] < targetTime) targetTime = nsecs[nsecs.size - 1]
            }
        }

        var totalBytes: Long = 0

        for (i in 0 until numThreads) {
            val bytes = allBytes[i]
            val nsecs = allNsecs[i]

            if (bytes != null && nsecs != null && bytes.isNotEmpty()) {
                require(bytes.size == nsecs.size)

                var targetIdx = bytes.size
                for (j in bytes.indices) {
                    if (nsecs[j] >= targetTime) {
                        targetIdx = j
                        break
                    }
                }

                val calcBytes: Long
                if (targetIdx == bytes.size) {
                    // nsec[max] == targetTime
                    calcBytes = bytes[bytes.size - 1]
                } else {
                    val bytes1 = if (targetIdx == 0) 0 else bytes[targetIdx - 1]
                    val bytes2 = bytes[targetIdx]
                    val bytesDiff = bytes2 - bytes1

                    val nsec1 = if (targetIdx == 0) 0 else nsecs[targetIdx - 1]
                    val nsec2 = nsecs[targetIdx]
                    val nsecDiff = nsec2 - nsec1

                    val nsecCompensation = targetTime - nsec1
                    val factor = nsecCompensation.toDouble() / nsecDiff.toDouble()

                    var compensation = Math.round(bytesDiff * factor)
                    if (compensation < 0) compensation = 0
                    calcBytes = bytes1 + compensation
                }
                totalBytes += calcBytes
            }
        }

        if (upload) {
            bytes_upload = totalBytes
            nsec_upload = targetTime
            speed_upload = getUploadSpeedBitPerSec() / 1e3
        } else {
            bytes_download = totalBytes
            nsec_download = targetTime
            speed_download = getDownloadSpeedBitPerSec() / 1e3
        }
    }

    companion object {
        fun calculateAndGet(speedMap: Map<Int, List<SpeedItem>>): TotalTestResult {
            val threads = speedMap.keys.size

            var allBytes: Array<LongArray?>? = null
            var allNsecs: Array<LongArray?>? = null

            var threadCounter = 0
            for ((_, speedList) in speedMap) {
                if (allBytes == null) {
                    allBytes = arrayOfNulls(threads)
                    allNsecs = arrayOfNulls(threads)
                    for (t in 0 until threads) {
                        allBytes[t] = LongArray(speedList.size)
                        allNsecs!![t] = LongArray(speedList.size)
                    }
                }

                for (i in speedList.indices) {
                    allBytes[threadCounter]!![i] = speedList[i].bytes
                    allNsecs!![threadCounter]!![i] = speedList[i].time
                }

                threadCounter++
            }

            return calculateAndGet(allBytes ?: arrayOfNulls(0), allNsecs ?: arrayOfNulls(0), false)
        }

        fun calculateAndGet(allBytes: Array<LongArray?>, allNsecs: Array<LongArray?>, upload: Boolean): TotalTestResult {
            val totalResult = TotalTestResult()
            totalResult.calculate(allBytes, allNsecs, upload)
            return totalResult
        }
    }
}
