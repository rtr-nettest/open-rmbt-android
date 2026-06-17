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
package at.rtr.rmbt.client.helper

import at.rtr.rmbt.client.Ping
import at.rtr.rmbt.client.RMBTTestParameter
import at.rtr.rmbt.client.TotalTestResult
import at.rtr.rmbt.client.ndt.UiServicesAdapter
import at.rtr.rmbt.client.v2.task.TaskDesc
import at.rtr.rmbt.client.v2.task.service.TestMeasurement.TrafficDirection
import at.rtr.rmbt.util.capability.Capabilities
import at.rtr.rmbt.util.model.shared.exception.ErrorStatus
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.util.HashSet
import java.util.Locale
import java.util.TimeZone

class ControlServerConnection {

    // url to make request
    private var hostUrl: URL? = null

    private var testEncryption = false

    private var testToken = ""
    private var testUuid = ""
    private var loopUuid = ""

    private var testTime: Long = 0

    private var testHost = ""
    private var serverType: String? = null
    private var testPort = 0
    private var remoteIp = ""
    private var serverName: String? = null
    private var provider: String? = null

    private var testDuration = 0
    private var testNumThreads = 0
    private var testNumPings = 0

    private var clientUUID = ""

    private var resultURL: URL? = null
    private var resultQoSURI: URL? = null

    private var errorMsg: String? = null

    private var hasError = false

    private var lastErrorList: MutableSet<ErrorStatus>? = null

    @JvmField
    var udpTaskDesc: TaskDesc? = null

    @JvmField
    var dnsTaskDesc: TaskDesc? = null

    @JvmField
    var ntpTaskDesc: TaskDesc? = null

    @JvmField
    var httpTaskDesc: TaskDesc? = null

    @JvmField
    var tcpTaskDesc: TaskDesc? = null

    private var lastTestResult: JSONObject? = null

    @JvmField
    var v2TaskDesc: MutableList<TaskDesc>? = null

    private var startTimeMillis: Long = 0
    private var startTimeNs: Long = 0

    init {
        val capabilities = Capabilities()
        capabilities.rmbtHttp = true
        JSONParser.setCapabilities(capabilities)
    }

    /**
     * requests the parameters for the v2 tests
     */
    fun requestQoSTestParameters(
        host: String,
        pathPrefix: String?,
        port: Int,
        encryption: Boolean,
        geoInfo: ArrayList<String>?,
        uuid: String,
        clientType: String?,
        clientName: String?,
        clientVersion: String?,
        additionalValues: JSONObject?,
        headerValue: String?
    ): String? {
        resetErrors()
        clientUUID = uuid

        hostUrl = getUrl(encryption, host, pathPrefix, port, Config.RMBT_QOS_TEST_REQUEST, headerValue)

        println("Connection to $hostUrl")

        val regData = JSONObject()

        try {
            regData.put("uuid", uuid)
            regData.put("client", clientName)
            regData.put("version", Config.RMBT_VERSION_NUMBER)
            regData.put("type", clientType)
            regData.put("softwareVersion", clientVersion)
            regData.put("softwareRevision", RevisionHelper.getVerboseRevision())
            regData.put("language", Locale.getDefault().language)
            regData.put("timezone", TimeZone.getDefault().id)
            regData.put("time", System.currentTimeMillis())

            if (geoInfo != null) {
                val locData = JSONObject()
                locData.put("time", geoInfo[0])
                locData.put("lat", geoInfo[1])
                locData.put("long", geoInfo[2])
                locData.put("accuracy", geoInfo[3])
                locData.put("altitude", geoInfo[4])
                locData.put("bearing", geoInfo[5])
                locData.put("speed", geoInfo[6])
                locData.put("provider", geoInfo[7])

                regData.accumulate("location", locData)
            }

            addToJSONObject(regData, additionalValues)
        } catch (e1: JSONException) {
            hasError = true
            errorMsg = "Error gernerating request"
        }

        val startTime = System.nanoTime()
        // getting JSON string from URL
        val response = JSONParser.sendJSONToUrl(hostUrl!!, regData, headerValue)
        val endTime = System.nanoTime()

        Timber.d("REQUEST " + hostUrl.toString() + "  " + (endTime - startTime) + "ns")

        if (response != null) {
            try {
                val errorList = response.getJSONArray("error")

                checkHasErrors(response)

                if (errorList.length() == 0) {
                    val testPort = 5233

                    val testParams = JSONParser.toMap(response.getJSONObject("objectives"))

                    v2TaskDesc = ArrayList()

                    for (e in testParams.entries) {
                        @Suppress("UNCHECKED_CAST")
                        val paramList = e.value as List<HashMap<String, Any?>>
                        for (params in paramList) {
                            val taskDesc = TaskDesc(testHost, testPort, encryption, testToken, 0, 1, 0, System.nanoTime(), params, e.key)
                            v2TaskDesc!!.add(taskDesc)
                        }
                    }
                } else {
                    hasError = true
                    for (i in 0 until errorList.length()) {
                        if (i > 0) {
                            errorMsg += "\n"
                        }
                        errorMsg += errorList.getString(i)
                    }
                }
            } catch (e: JSONException) {
                hasError = true
                errorMsg = "Error parsing server response"
                e.printStackTrace()
            }
        } else {
            hasError = true
            errorMsg = "No response"
        }

        return errorMsg
    }

    private fun resetErrors() {
        lastErrorList = null
    }

    @Throws(JSONException::class)
    private fun checkHasErrors(response: JSONObject): Boolean {
        val errorList = response.getJSONArray("error")
        val errorFlags = response.optJSONArray("error_flags")
        if (errorFlags != null && errorFlags.length() > 0) {
            val list = HashSet<ErrorStatus>()
            for (i in 0 until errorFlags.length()) {
                list.add(ErrorStatus.valueOf(errorFlags.getString(i)))
            }
            lastErrorList = list
        }
        return errorList.length() > 0
    }

    fun requestNewTestConnection(
        host: String,
        pathPrefix: String?,
        port: Int,
        encryption: Boolean,
        geoInfo: ArrayList<String>?,
        uuid: String,
        clientType: String?,
        clientName: String?,
        clientVersion: String?,
        additionalValues: JSONObject?,
        headerValue: String?
    ): String? {
        resetErrors()
        var errorMsg: String? = null
        // url to make request to

        clientUUID = uuid

        hostUrl = getUrl(encryption, host, pathPrefix, port, Config.RMBT_TEST_SETTINGS_REQUEST, headerValue)

        println("Connection to $hostUrl")

        val regData = JSONObject()

        try {
            regData.put("uuid", uuid)
            regData.put("client", clientName)
            regData.put("version", Config.RMBT_VERSION_NUMBER)
            regData.put("type", clientType)
            regData.put("softwareVersion", clientVersion)
            regData.put("softwareRevision", RevisionHelper.getVerboseRevision())
            regData.put("language", Locale.getDefault().language)
            regData.put("timezone", TimeZone.getDefault().id)
            startTimeMillis = System.currentTimeMillis()
            regData.put("time", startTimeMillis)
            startTimeNs = System.nanoTime()

            if (geoInfo != null) {
                val locData = JSONObject()
                locData.put("time", geoInfo[0])
                locData.put("lat", geoInfo[1])
                locData.put("long", geoInfo[2])
                locData.put("accuracy", geoInfo[3])
                locData.put("altitude", geoInfo[4])
                locData.put("bearing", geoInfo[5])
                locData.put("speed", geoInfo[6])
                locData.put("provider", geoInfo[7])

                regData.accumulate("location", locData)
            }

            addToJSONObject(regData, additionalValues)
        } catch (e1: JSONException) {
            errorMsg = "Error gernerating request"
        }

        val startTime = System.nanoTime()
        // getting JSON string from URL
        val response = JSONParser.sendJSONToUrl(hostUrl!!, regData, headerValue)
        val endTime = System.nanoTime()

        Timber.d("REQUEST " + hostUrl.toString() + "  " + (endTime - startTime) + "ns")

        if (response != null) {
            try {
                val errorList = response.getJSONArray("error")

                checkHasErrors(response)

                if (errorList.length() == 0) {
                    clientUUID = response.optString("uuid", clientUUID)

                    testToken = response.getString("test_token")
                    loopUuid = response.optString("loop_uuid")

                    testUuid = response.getString("test_uuid")

                    testTime = System.currentTimeMillis() + 1000 * response.getLong("test_wait")

                    testHost = response.getString("test_server_address")
                    testPort = response.getInt("test_server_port")
                    serverType = response.optString("test_server_type", Config.SERVER_TYPE_RMBT)
                    testEncryption = response.getBoolean("test_server_encryption")
                    serverName = response.optString("test_server_name", null)
                    provider = response.optString("provider", null)

                    testDuration = response.getInt("test_duration")
                    testNumThreads = response.getInt("test_numthreads")
                    testNumPings = response.optInt("test_numpings", 10) // pings default to 10

                    remoteIp = response.getString("client_remote_ip")

                    resultURL = URL(response.getString("result_url"))
                    resultQoSURI = URL(response.getString("result_qos_url"))
                } else {
                    errorMsg = ""
                    for (i in 0 until errorList.length()) {
                        if (i > 0) {
                            errorMsg += "\n"
                        }
                        errorMsg += errorList.getString(i)
                    }
                }
            } catch (e: JSONException) {
                errorMsg = "Error parsing server response"
                e.printStackTrace()
            } catch (e: MalformedURLException) {
                errorMsg = "Error parsing server response"
                e.printStackTrace()
            }
        } else {
            errorMsg = "No response"
        }
        return errorMsg
    }

    /**
     * Best effort: Try to set a test as "aborted"
     */
    fun abortStartedTest() {
        try {
            val jsonObject = JSONObject()
            jsonObject.put("uuid", clientUUID)
            jsonObject.put("test_uuid", testUuid)
            jsonObject.put("aborted", true)

            JSONParser.sendJSONToUrl(hostUrl!!.toURI().resolve(Config.RMBT_CONTROL_PATH + Config.RMBT_UPDATE_RESULT_URL).toURL(), jsonObject)
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun sendTestResult(result: TotalTestResult, additionalValues: JSONObject?, headerValue: String?): String? {
        var errorMsg: String? = null
        lastTestResult = null

        if (resultURL != null) {
            val testData = JSONObject()

            try {
                testData.put("client_uuid", clientUUID)
                testData.put("client_name", Config.RMBT_CLIENT_NAME)
                testData.put("client_version", result.client_version)
                testData.put("client_language", Locale.getDefault().language)

                testData.put("time", System.currentTimeMillis())

                testData.put("test_token", testToken)

                testData.put("test_port_remote", result.port_remote)
                testData.put("test_bytes_download", result.bytes_download)
                testData.put("test_bytes_upload", result.bytes_upload)
                testData.put("test_total_bytes_download", result.totalDownBytes)
                testData.put("test_total_bytes_upload", result.totalUpBytes)
                testData.put("test_encryption", result.encryption)
                testData.put("test_ip_local", result.ip_local!!.hostAddress)
                testData.put("test_ip_server", result.ip_server!!.hostAddress)
                testData.put("test_nsec_download", result.nsec_download)
                testData.put("test_nsec_upload", result.nsec_upload)
                testData.put("test_num_threads", result.num_threads)
                testData.put("test_speed_download", Math.floor(result.speed_download + 0.5).toLong())
                testData.put("test_speed_upload", Math.floor(result.speed_upload + 0.5).toLong())
                testData.put("test_ping_shortest", result.ping_shortest)

                // total bytes on interface
                testData.put("test_if_bytes_download", result.getTotalTrafficMeasurement(TrafficDirection.RX))
                testData.put("test_if_bytes_upload", result.getTotalTrafficMeasurement(TrafficDirection.TX))
                // bytes during download test
                testData.put("testdl_if_bytes_download", result.getTrafficByTestPart(TestStatus.DOWN, TrafficDirection.RX))
                testData.put("testdl_if_bytes_upload", result.getTrafficByTestPart(TestStatus.DOWN, TrafficDirection.TX))
                // bytes during upload test
                testData.put("testul_if_bytes_download", result.getTrafficByTestPart(TestStatus.UP, TrafficDirection.RX))
                testData.put("testul_if_bytes_upload", result.getTrafficByTestPart(TestStatus.UP, TrafficDirection.TX))

                // relative timestamps:
                val dlMeasurement = result.getTestMeasurementByTestPart(TestStatus.DOWN)
                if (dlMeasurement != null) {
                    testData.put("time_dl_ns", dlMeasurement.timeStampStart - startTimeNs)
                }
                val ulMeasurement = result.getTestMeasurementByTestPart(TestStatus.UP)
                if (ulMeasurement != null) {
                    testData.put("time_ul_ns", ulMeasurement.timeStampStart - startTimeNs)
                }

                val pingData = JSONArray()

                if (result.pings.isNotEmpty()) {
                    for (ping in result.pings) {
                        val pingItem = JSONObject()
                        pingItem.put("value", ping.client)
                        pingItem.put("value_server", ping.server)
                        pingItem.put("time_ns", ping.timeNs - startTimeNs)
                        pingData.put(pingItem)
                    }
                }

                testData.put("pings", pingData)

                val speedDetail = JSONArray()

                for (item in result.speedItems) {
                    speedDetail.put(item.toJSON())
                }

                testData.put("speed_detail", speedDetail)

                addToJSONObject(testData, additionalValues)
            } catch (e1: JSONException) {
                errorMsg = "Error gernerating request"
                e1.printStackTrace()
            }

            // getting JSON string from URL
            var response = JSONParser.sendJSONToUrl(resultURL!!, testData, headerValue)

            var i = 0
            while (response == null && i < 4) {
                // try again
                val connectTimeout = JSONParser.CONNECT_TIMEOUT + Math.round(Math.pow(3.0, i.toDouble()) * 1000).toInt()
                println("Submitting the results failed, trying again with $connectTimeout ms timeout")
                response = JSONParser.sendJSONToUrl(resultURL!!, testData, connectTimeout, headerValue)
                i++
            }

            if (response != null) {
                try {
                    val errorList = response.getJSONArray("error")

                    if (errorList.length() == 0) {
                        lastTestResult = testData
                    } else {
                        for (j in 0 until errorList.length()) {
                            if (j > 0) {
                                errorMsg += "\n"
                            }
                            errorMsg += errorList.getString(j)
                        }
                    }
                } catch (e: JSONException) {
                    errorMsg = "Error parsing server response"
                    e.printStackTrace()
                }
            } else {
                errorMsg = "Result submission failed"
            }
        } else {
            errorMsg = "No URL to send the Data to."
        }

        return errorMsg
    }

    fun sendQoSResult(result: TotalTestResult?, qosTestResult: JSONArray?, headerValue: String?): String? {
        var errorMsg: String? = null
        println("sending qos results to $resultQoSURI")
        if (resultQoSURI != null) {
            val testData = JSONObject()

            try {
                testData.put("client_uuid", clientUUID)
                testData.put("client_name", Config.RMBT_CLIENT_NAME)
                testData.put("client_language", Locale.getDefault().language)

                testData.put("time", System.currentTimeMillis())

                testData.put("test_token", testToken)

                testData.put("qos_result", qosTestResult)
            } catch (e1: JSONException) {
                errorMsg = "Error gernerating request"
                e1.printStackTrace()
            }

            // getting JSON string from URL
            val response = JSONParser.sendJSONToUrl(resultQoSURI!!, testData, headerValue)

            if (response != null) {
                try {
                    val errorList = response.getJSONArray("error")

                    if (errorList.length() == 0) {
                        // all is fine
                    } else {
                        for (i in 0 until errorList.length()) {
                            if (i > 0) {
                                errorMsg += "\n"
                            }
                            errorMsg += errorList.getString(i)
                        }
                    }
                } catch (e: JSONException) {
                    errorMsg = "Error parsing server response"
                    e.printStackTrace()
                }
            }
        } else {
            errorMsg = "No URL to send the Data to."
        }

        return errorMsg
    }

    fun sendNDTResult(
        host: String,
        pathPrefix: String?,
        port: Int,
        encryption: Boolean,
        clientUUID: String,
        data: UiServicesAdapter,
        testUuid: String?,
        headerValue: String?
    ) {
        hostUrl = getUrl(encryption, host, pathPrefix, port, Config.RMBT_TEST_SETTINGS_REQUEST, headerValue)
        this.clientUUID = clientUUID
        sendNDTResult(data, testUuid, headerValue)
    }

    fun sendNDTResult(data: UiServicesAdapter, testUuid: String?, headerValue: String?) {
        val testData = JSONObject()

        try {
            testData.put("client_uuid", clientUUID)
            testData.put("client_language", Locale.getDefault().language)
            if (testUuid != null) {
                testData.put("test_uuid", testUuid)
            } else {
                testData.put("test_uuid", this.testUuid)
            }
            testData.put("s2cspd", data.s2cspd)
            testData.put("c2sspd", data.c2sspd)
            testData.put("avgrtt", data.avgrtt)
            testData.put("main", data.sbMain.toString())
            testData.put("stat", data.sbStat.toString())
            testData.put("diag", data.sbDiag.toString())
            testData.put("time_ns", data.startTimeNs - startTimeNs)
            testData.put("time_end_ns", data.stopTimeNs - startTimeNs)

            JSONParser.sendJSONToUrl(hostUrl!!.toURI().resolve(Config.RMBT_CONTROL_NDT_RESULT_URL).toURL(), testData, headerValue)

            println(testData)
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    fun getRemoteIp(): String = remoteIp

    fun getClientUUID(): String = clientUUID

    fun getServerName(): String? = serverName

    fun getLoopUuid(): String? {
        if (loopUuid != null && loopUuid.isNotEmpty()) {
            return loopUuid
        }
        return null
    }

    fun getProvider(): String? = provider

    fun getTestTime(): Long = testTime

    /**
     * this time stamp is only a relative timestamp (see: [System.nanoTime])
     */
    fun getStartTimeNs(): Long = startTimeNs

    /**
     * this is the starting (= timestamp of the test request) UNIX timestamp (see: [System.currentTimeMillis])
     */
    fun getStartTimeMillis(): Long = startTimeMillis

    fun getTestUuid(): String = testUuid

    fun getTestToken(): String = testToken

    fun getLastTestResult(): JSONObject? = lastTestResult

    fun getTestParameter(overrideParams: RMBTTestParameter?): RMBTTestParameter {
        var host = testHost
        var port = testPort
        var encryption = testEncryption
        var duration = testDuration
        var numThreads = testNumThreads
        val numPings = testNumPings

        if (overrideParams != null) {
            if (overrideParams.host != null && overrideParams.port > 0) {
                host = overrideParams.host!!
                encryption = overrideParams.isEncryption
                port = overrideParams.port
            }
            if (overrideParams.duration > 0) {
                duration = overrideParams.duration
            }
            if (overrideParams.numThreads > 0) {
                numThreads = overrideParams.numThreads
            }
        }
        return RMBTTestParameter(host, port, encryption, testToken, duration, numThreads, numPings, testTime, serverType)
    }

    fun getLastErrorList(): Set<ErrorStatus>? = lastErrorList

    companion object {
        private fun getUrl(
            encryption: Boolean,
            host: String,
            pathPrefix: String?,
            port: Int,
            path: String,
            headerValue: String?
        ): URL? {
            return try {
                val protocol = if (encryption) "https" else "http"
                val defaultPort = if (encryption) 443 else 80
                val totalPath = if (headerValue != null && headerValue.isNotEmpty()) {
                    (pathPrefix ?: "") + path
                } else {
                    (pathPrefix ?: "") + Config.RMBT_CONTROL_PATH + path
                }

                if (defaultPort == port) {
                    URL(protocol, host, totalPath)
                } else {
                    URL(protocol, host, port, totalPath)
                }
            } catch (e: MalformedURLException) {
                null
            }
        }

        @JvmStatic
        @Throws(JSONException::class)
        fun addToJSONObject(data: JSONObject, additionalValues: JSONObject?) {
            if (additionalValues != null && additionalValues.length() > 0) {
                val attr = additionalValues.names()!!
                for (i in 0 until attr.length()) {
                    data.put(attr.getString(i), additionalValues.get(attr.getString(i)))
                }
            }
        }
    }
}
