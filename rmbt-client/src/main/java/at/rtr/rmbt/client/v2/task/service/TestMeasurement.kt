/*******************************************************************************
 * Copyright 2013-2014 alladin-IT GmbH
 * Copyright 2013-2014 Rundfunk und Telekom Regulierungs-GmbH (RTR-GmbH)
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
package at.rtr.rmbt.client.v2.task.service

/**
 * @author lb
 */
class TestMeasurement {
    enum class TrafficDirection {
        TX,
        RX,
        TOTAL
    }

    var rxBytes: Long = 0
    var txBytes: Long = 0
    private var isRunning = false
    private var service: TrafficService? = null
    var timeStampStart: Long = 0
    var timeStampStop: Long = 0

    // debug purpose:
    private var id: String? = null

    /**
     * @param id is only for debug purpose
     * @param service the [TrafficService], may be `null`
     */
    constructor(id: String?, service: TrafficService?) {
        this.service = service
        this.id = id
    }

    constructor(rxBytes: Long, txBytes: Long, timeStampStart: Long, timeStampStop: Long) {
        this.rxBytes = rxBytes
        this.txBytes = txBytes
        this.timeStampStart = timeStampStart
        this.timeStampStop = timeStampStop
    }

    /**
     * @param threadId a thread id (only for debug purpose)
     */
    @Synchronized
    fun start(threadId: Int) {
        if (!isRunning) {
            isRunning = true
            this.timeStampStart = System.nanoTime()
            service?.let { s ->
                s.start()
                println("TRAFFICSERVICE '$id' STARTED BY THREAD $threadId")
            }
        }
    }

    /**
     * @param threadId a thread id (only for debug purpose)
     */
    @Synchronized
    fun stop(threadId: Int) {
        if (isRunning) {
            isRunning = false
            this.timeStampStop = System.nanoTime()
            service?.let { s ->
                s.stop()
                println("TRAFFICSERVICE '$id' STOPPED BY THREAD $threadId RX/TX: ${s.getRxBytes()}/${s.getTxBytes()}")
                rxBytes = s.getRxBytes()
                txBytes = s.getTxBytes()
            }
        }
    }

    override fun toString(): String =
        "TestMeasurement [rxBytes=$rxBytes, txBytes=$txBytes, isRunning=$isRunning, service=$service, " +
            "timeStampStart=$timeStampStart, timeStampStop=$timeStampStop, id=$id]"
}
