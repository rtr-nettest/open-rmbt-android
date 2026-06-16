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
package at.rtr.rmbt.client.v2.task

import at.rtr.rmbt.client.RMBTTestParameter
import at.rtr.rmbt.client.helper.Config

/**
 * @author lb
 */
class TaskDesc : RMBTTestParameter {

    private val params: HashMap<String, Any>

    constructor(
        host: String?,
        port: Int,
        encryption: Boolean,
        token: String?,
        duration: Int,
        numThreads: Int,
        numPings: Int,
        startTime: Long,
        params: HashMap<String, Any>
    ) : super(host, port, encryption, token, duration, numThreads, numPings, startTime, Config.SERVER_TYPE_QOS) {
        this.params = params
    }

    constructor(
        host: String?,
        port: Int,
        encryption: Boolean,
        token: String?,
        duration: Int,
        numThreads: Int,
        numPings: Int,
        startTime: Long,
        params: HashMap<String, Any>,
        qosTestId: String
    ) : this(host, port, encryption, token, duration, numThreads, numPings, startTime, params) {
        params[QOS_TEST_IDENTIFIER_KEY] = qosTestId
    }

    fun getParams(): HashMap<String, Any> = params

    override fun toString(): String = "TaskDesc [params=$params]"

    companion object {
        const val QOS_TEST_IDENTIFIER_KEY = "qostest"
    }
}
