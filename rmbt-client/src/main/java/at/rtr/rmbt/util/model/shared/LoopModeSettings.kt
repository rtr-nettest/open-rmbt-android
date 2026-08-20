/*******************************************************************************
 * Copyright 2016 Specure GmbH
 * Copyright 2016 Rundfunk und Telekom Regulierungs-GmbH (RTR-GmbH)
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
package at.rtr.rmbt.util.model.shared

import com.google.gson.annotations.SerializedName

class LoopModeSettings() {

    var uid: Long? = null

    @SerializedName("test_uuid")
    var testUuid: String? = null

    @SerializedName("client_uuid")
    var clientUuid: String? = null

    @SerializedName("max_delay")
    var maxDelay: Int = 0

    @SerializedName("max_movement")
    var maxMovement: Int = 0

    @SerializedName("max_tests")
    var maxTests: Int = 0

    @SerializedName("test_counter")
    var testCounter: Int = 0

    @SerializedName("loop_uuid")
    var loopUuid: String? = null

    constructor(
        maxDelay: Int,
        maxMovement: Int,
        maxTests: Int,
        testCounter: Int,
        loopUuid: String?
    ) : this() {
        this.maxDelay = maxDelay
        this.maxMovement = maxMovement
        this.maxTests = maxTests
        this.testCounter = testCounter
        this.loopUuid = loopUuid
    }

    override fun toString(): String =
        "LoopModeSettings [uid=$uid, testUuid=$testUuid, clientUuid=$clientUuid, maxDelay=$maxDelay, " +
            "maxMovement=$maxMovement, maxTests=$maxTests, testCounter=$testCounter]"
}
