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
package at.rtr.rmbt.client.v2.task.service

import at.rtr.rmbt.client.QualityOfServiceTest
import at.rtr.rmbt.client.v2.task.AbstractQoSTask
import at.rtr.rmbt.client.v2.task.service.TestProgressListener.TestProgressEvent
import at.rtr.rmbt.util.tools.TracerouteService
import java.io.File
import java.net.InetAddress

class TestSettings {
    var isUseSsl = false
    var startTimeNs: Long = 0
    var cacheFolder: File? = null
    var trafficService: TrafficService? = null
    var websiteTestService: WebsiteTestService? = null
    var tracerouteServiceClazz: Class<out TracerouteService>? = null
    private val testProgressListenerList: MutableList<TestProgressListener> = ArrayList()
    var defaultDnsResolvers: List<InetAddress>? = ArrayList()

    constructor()

    constructor(startTimeNs: Long) {
        this.startTimeNs = startTimeNs
    }

    fun getTestProgressListener(): MutableList<TestProgressListener> = testProgressListenerList

    fun addTestProgressListener(listener: TestProgressListener) {
        if (!testProgressListenerList.contains(listener)) {
            testProgressListenerList.add(listener)
        }
    }

    fun dispatchTestProgressEvent(event: TestProgressEvent, test: AbstractQoSTask) {
        dispatchTestProgressEvent(event, test, null)
    }

    fun dispatchTestProgressEvent(event: TestProgressEvent, test: AbstractQoSTask, qosTest: QualityOfServiceTest?) {
        when (event) {
            TestProgressEvent.ON_START -> for (listener in testProgressListenerList) {
                listener.onQoSTestStart(test)
            }
            TestProgressEvent.ON_END -> for (listener in testProgressListenerList) {
                listener.onQoSTestEnd(test)
            }
            TestProgressEvent.ON_CREATED -> if (qosTest != null) {
                for (listener in testProgressListenerList) {
                    listener.onQoSCreated(qosTest)
                }
            }
        }
    }

    override fun toString(): String =
        "TestSettings [useSsl=$isUseSsl, startTimeNs=$startTimeNs, cacheFolder=$cacheFolder, " +
            "trafficService=$trafficService, websiteTestService=$websiteTestService, " +
            "testProgressListenerList=$testProgressListenerList]"
}
