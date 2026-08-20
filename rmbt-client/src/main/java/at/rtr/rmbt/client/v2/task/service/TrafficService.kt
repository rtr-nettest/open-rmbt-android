/*******************************************************************************
 * Copyright 2013-2016 alladin-IT GmbH
 * Copyright 2013-2016 Rundfunk und Telekom Regulierungs-GmbH (RTR-GmbH)
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
interface TrafficService {

    fun start(): Int

    fun getTxBytes(): Long

    fun getRxBytes(): Long

    fun getTotalTxBytes(): Long

    fun getTotalRxBytes(): Long

    fun getCurrentTxBytes(): Long

    fun getCurrentRxBytes(): Long

    fun update()

    fun stop()

    companion object {
        const val SERVICE_NOT_SUPPORTED = -1

        const val SERVICE_START_OK = 0
    }
}
