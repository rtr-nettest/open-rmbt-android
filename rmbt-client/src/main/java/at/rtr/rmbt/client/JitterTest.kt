/*******************************************************************************
 * Copyright 2014-2017 Specure GmbH
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

import at.rtr.rmbt.client.v2.task.service.TestSettings

/*
 * This is implementation of the VOIP test possible to run with the main test.
 * It is identical to a VoIP test except for the task identifier used to pick
 * the matching task descriptor.
 */
class JitterTest(client: RMBTClient, nnTestSettings: TestSettings?) :
    VoipTest(client, nnTestSettings, true, null, true) {

    override fun getTestId(): String = RMBTClient.TASK_JITTER
}
