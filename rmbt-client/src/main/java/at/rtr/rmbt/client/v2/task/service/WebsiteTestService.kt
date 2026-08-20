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
interface WebsiteTestService {

    interface RenderingListener {
        fun onRenderFinished(test: WebsiteTestService)

        fun onDownloadStarted(test: WebsiteTestService)

        fun onTimeoutReached(test: WebsiteTestService): Boolean

        fun onError(test: WebsiteTestService): Boolean
    }

    fun getHash(): String?

    fun getStatusCode(): Int

    fun getDownloadDuration(): Long

    fun run(targetUrl: String?, timeOut: Long)

    fun isRunning(): Boolean

    fun hasFinished(): Boolean

    fun hasError(): Boolean

    fun setOnRenderingFinishedListener(listener: RenderingListener)

    fun getInstance(): WebsiteTestService

    fun getTxBytes(): Long

    fun getRxBytes(): Long

    fun getTotalTrafficBytes(): Long

    fun getResourceCount(): Int
}
