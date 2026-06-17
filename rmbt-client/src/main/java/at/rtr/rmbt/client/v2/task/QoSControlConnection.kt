/*******************************************************************************
 * Copyright 2015 alladin-IT GmbH
 * Copyright 2015 Rundfunk und Telekom Regulierungs-GmbH (RTR-GmbH)
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

import at.rtr.rmbt.client.AbstractRMBTTest
import at.rtr.rmbt.client.RMBTClient
import at.rtr.rmbt.client.RMBTTestParameter
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.util.TreeSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

/**
 * @author lb
 */
class QoSControlConnection(client: RMBTClient, params: RMBTTestParameter) :
    AbstractRMBTTest(client, params, 1), Runnable {

    @JvmField
    val isRunning = AtomicBoolean(true)

    @JvmField
    val couldNotConnect = AtomicBoolean(false)

    private val requestMap = ConcurrentHashMap<Int, ControlConnectionResponseCallbackHolder>()

    val concurrencyGroupSet = TreeSet<Int>()

    private var controlSocket: Socket? = null

    @Throws(IOException::class)
    fun sendTaskCommand(qosTask: AbstractQoSTask, command: String, callback: ControlConnectionResponseCallback?) {
        if (callback != null) {
            requestMap[qosTask.getId()] = ControlConnectionResponseCallbackHolder(command, callback)
        }
        sendMessage(command + " +ID" + qosTask.getId() + "\n")
    }

    override fun run() {
        try {
            while (isRunning.get()) {
                val response = reader!!.readLine()
                if (response != null) {
                    val m = ID_REGEX_PATTERN.matcher(response)
                    if (m.find()) {
                        val id = m.group(1).toInt()
                        val holder = requestMap.remove(id)
                        if (holder?.callback != null) {
                            val responseRunnable = Runnable {
                                holder.callback?.onResponse(response, holder.reqeust)
                            }

                            RMBTClient.getCommonThreadPool().execute(responseRunnable)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isRunning.set(false)
            val socket = controlSocket
            if (socket != null && !socket.isClosed) {
                try {
                    socket.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Throws(Exception::class)
    fun connect() {
        isRunning.set(true)
        try {
            controlSocket = connect(
                null, InetAddress.getByName(params.host), params.port,
                AbstractQoSTask.QOS_SERVER_PROTOCOL_VERSION, "ACCEPT", params.isEncryption, AbstractQoSTask.CONTROL_CONNECTION_TIMEOUT
            )
        } catch (e: Exception) {
            isRunning.set(false)
            couldNotConnect.set(true)
            throw e
        }
    }

    @Throws(IOException::class)
    fun close() {
        sendMessage("QUIT\n")
        isRunning.set(false)
        controlSocket!!.close()
    }

    /**
     * @author lb
     */
    class ControlConnectionResponseCallbackHolder(request: String?, callback: ControlConnectionResponseCallback?) {
        var callback: ControlConnectionResponseCallback? = callback
        var reqeust: String? = request
    }

    companion object {
        @JvmField
        val ID_REGEX_PATTERN: Pattern = Pattern.compile("\\+ID([\\d]*)")
    }
}
