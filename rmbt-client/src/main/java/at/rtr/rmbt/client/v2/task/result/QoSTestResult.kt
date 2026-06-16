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
package at.rtr.rmbt.client.v2.task.result

import at.rtr.rmbt.client.TestResult
import at.rtr.rmbt.client.v2.task.AbstractQoSTask
import java.util.Locale

/**
 * @author lb
 */
class QoSTestResult(
    val testType: QoSTestResultEnum,
    val qosTask: AbstractQoSTask
) : TestResult() {

    val resultMap: HashMap<String, Any> = HashMap()

    var isFatalError = false

    init {
        resultMap["test_type"] = testType.name.lowercase(Locale.US)
    }

    override fun toString(): String =
        "QoSTestResult [resultMap=$resultMap, testType=$testType, fatalError=$isFatalError]"
}
