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
package at.rtr.rmbt.client.helper

/**
 * The system defaults.
 */
object Config {

    /*********************
     *
     * Default Preferences
     *
     */
    const val RMBT_SERVER_NAME = "RMBT"
    const val RMBT_CLIENT_NAME = "RMBT"
    const val RMBT_VERSION_NUMBER = "0.3"
    const val RMBT_VERSION_EXPRESSION = RMBT_CLIENT_NAME + "v([0-9.]+)"
    const val RMBT_LATEST_SERVER = "1.2.0"

    const val RMBT_CONTROL_PORT = 443
    const val RMBT_CONTROL_SSL = true
    const val RMBT_QOS_SSL = true
    const val RMBT_CONTROL_PATH = "/RMBTControlServer"
    const val RMBT_TEST_SETTINGS_REQUEST = "/testRequest"
    const val RMBT_QOS_TEST_REQUEST = "/qosTestRequest"
    const val RMBT_CONTROL_NDT_RESULT_URL = "ndtResult"
    const val RMBT_NEWS_HOST_URL = "/news"
    const val RMBT_HISTORY_HOST_URL = "/history"
    const val RMBT_TESTRESULT_HOST_URL = "/testresult"
    const val RMBT_TESTRESULT_DETAIL_HOST_URL = "/testresultdetail"
    const val RMBT_TESTRESULT_QOS_HOST_URL = "/qosTestResult"
    const val RMBT_TESTRESULT_OPENDATA_HOST_URL = "/opentests/"
    const val RMBT_SYNC_HOST_URL = "/sync"
    const val RMBT_SETTINGS_HOST_URL = "/settings"
    const val RMBT_UPDATE_RESULT_URL = "/resultUpdate"

    const val RMBT_ENCRYPTION_STRING = "TLS"

    const val MLAB_NS = "http://mlab-ns.appspot.com/ndt?format=json"
    const val NDT_FALLBACK_HOST = "ndt.iupui.donar.measurement-lab.org"

    const val SERVER_TYPE_QOS = "QoS"
    const val SERVER_TYPE_RMBT = "RMBT"
    const val SERVER_TYPE_RMBT_HTTP = "RMBThttp"
}
