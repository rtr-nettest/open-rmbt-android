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
package at.rtr.rmbt.client.ndt

import net.measurementlab.ndt.UiServices

class UiServicesAdapter : UiServices {
    var s2cspd: Double? = null

    var c2sspd: Double? = null

    var avgrtt: Double? = null

    val sbMain = StringBuffer()

    val sbStat = StringBuffer()

    val sbDiag = StringBuffer()

    var startTimeNs: Long = 0
        private set

    var stopTimeNs: Long = 0
        private set

    fun arePrimaryResultsSet(): Boolean {
        return s2cspd != null && c2sspd != null
    }

    override fun appendString(str: String?, viewId: Int) {
        if (str == null) {
            return
        }
        when (viewId) {
            UiServices.MAIN_VIEW -> sbMain.append(str)
            UiServices.STAT_VIEW -> sbStat.append(str)
            UiServices.DIAG_VIEW -> sbDiag.append(str)
        }
    }

    override fun incrementProgress() {
    }

    override fun onBeginTest() {
        this.startTimeNs = System.nanoTime()
        println("NDT START:" + this.startTimeNs)
    }

    override fun onEndTest() {
        this.stopTimeNs = System.nanoTime()
        println("NDT END:" + this.stopTimeNs)
    }

    override fun onFailure(errorMessage: String?) {
    }

    override fun onPacketQueuingDetected() {
    }

    override fun onLoginSent() {
    }

    override fun logError(str: String?) {
    }

    override fun updateStatus(status: String?) {
    }

    override fun updateStatusPanel(status: String?) {
    }

    override fun wantToStop(): Boolean {
        return false
    }

    override fun getClientApp(): String {
        return "RTR-NetTest"
    }

    override fun setVariable(name: String?, value: Int) {
    }

    override fun setVariable(name: String?, value: Double) {
        if (name == null) {
            return
        }
        when {
            "pub_avgrtt" == name -> avgrtt = value
            "pub_c2sspd" == name -> c2sspd = value
            "pub_s2cspd" == name -> s2cspd = value
        }
    }

    override fun setVariable(name: String?, value: Any?) {
    }
}
