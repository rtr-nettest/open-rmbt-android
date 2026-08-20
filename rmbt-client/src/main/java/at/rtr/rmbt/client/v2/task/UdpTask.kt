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
package at.rtr.rmbt.client.v2.task

import at.rtr.rmbt.client.QualityOfServiceTest
import at.rtr.rmbt.client.RMBTClient
import at.rtr.rmbt.client.v2.task.result.QoSTestResult
import at.rtr.rmbt.client.v2.task.result.QoSTestResultEnum
import at.rtr.rmbt.util.net.udp.NioUdpStreamSender
import at.rtr.rmbt.util.net.udp.StreamSender.UdpStreamCallback
import at.rtr.rmbt.util.net.udp.StreamSender.UdpStreamSenderSettings
import at.rtr.rmbt.util.net.udp.UdpStreamReceiver
import at.rtr.rmbt.util.net.udp.UdpStreamReceiver.UdpStreamReceiverSettings
import java.io.DataOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.channels.DatagramChannel
import java.util.TreeSet
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.regex.Pattern

/**
 * @author lb
 */
class UdpTask(nnTest: QualityOfServiceTest, taskDesc: TaskDesc, threadId: Int) :
    AbstractQoSTask(nnTest, taskDesc, threadId, threadId) {

    private val packetCountIncoming: Int?
    private val packetCountOutgoing: Int?
    private var outgoingPort: Int?
    private val incomingPort: Int?
    private val timeout: Long
    private val delay: Long

    init {
        var value = taskDesc.getParams()[PARAM_NUM_PACKETS_INCOMING] as String?
        this.packetCountIncoming = value?.toInt()

        value = taskDesc.getParams()[PARAM_NUM_PACKETS_OUTGOING] as String?
        this.packetCountOutgoing = value?.toInt()

        value = taskDesc.getParams()[PARAM_PORT] as String?
        this.incomingPort = value?.toInt()

        value = taskDesc.getParams()[PARAM_PORT_OUT] as String?
        this.outgoingPort = value?.toInt()

        value = taskDesc.getParams()[PARAM_TIMEOUT] as String?
        this.timeout = if (value != null) value.toLong() else DEFAULT_TIMEOUT

        value = taskDesc.getParams()[PARAM_DELAY] as String?
        this.delay = if (value != null) value.toLong() else DEFAULT_DELAY
    }

    /**
     * @author lb
     */
    class UdpPacketData(var remotePort: Int, var numPackets: Int, var dupNumPackets: Int) {
        var rcvServerResponse = 0

        override fun toString(): String =
            "UdpPacketData [remotePort=$remotePort, numPackets=$numPackets, dupNumPackets=$dupNumPackets, " +
                "rcvServerResponse=$rcvServerResponse]"
    }

    override fun call(): QoSTestResult {
        val result = initQoSTestResult(QoSTestResultEnum.UDP)
        try {
            onStart(result)

            var socket: DatagramSocket? = null

            val outgoingPacketData = UdpPacketData(0, 0, 0)
            val incomingPacketData = UdpPacketData(0, 0, 0)

            try {
                val outgoingLatch = CountDownLatch(1)

                // run UDP OUT test:
                if (packetCountOutgoing != null) {
                    val outgoingRequestCallback = object : ControlConnectionResponseCallback {
                        override fun onResponse(response: String?, request: String?) {
                            try {
                                if (request != null && request.startsWith("GET UDPPORT")) {
                                    if (response != null && !response.startsWith("ERR")) {
                                        outgoingPort = response.toInt()
                                        sendCommand("UDPTEST OUT " + outgoingPort + " " + packetCountOutgoing, this)
                                    }
                                } else if (request != null && request.startsWith("UDPTEST OUT")) {
                                    if (response != null && response.startsWith("OK")) {
                                        val udpOutTimeoutTask = RMBTClient.getCommonThreadPool().submit(object : Callable<UdpPacketData> {
                                            override fun call(): UdpPacketData {
                                                sendUdpPackets(outgoingPacketData)
                                                return outgoingPacketData
                                            }
                                        })

                                        try {
                                            udpOutTimeoutTask.get(timeout, TimeUnit.NANOSECONDS)
                                        } catch (e: Exception) {
                                            System.err.println("UDP Outgoing Timeout reached!")
                                            e.printStackTrace()
                                            udpOutTimeoutTask.cancel(true)
                                        }

                                        outgoingLatch.countDown()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    if (outgoingPort == null) {
                        sendCommand("GET UDPPORT", outgoingRequestCallback)
                    } else {
                        sendCommand("UDPTEST OUT " + outgoingPort + " " + packetCountOutgoing, outgoingRequestCallback)
                    }

                    if (!outgoingLatch.await(timeout, TimeUnit.NANOSECONDS)) {
                        println("OUT " + outgoingPort + " TIMEOUT REACHED: " + outgoingPacketData)
                    }

                    // request results;
                    val outgoingResultLatch = CountDownLatch(1)
                    val outgoingResultRequestCallback = object : ControlConnectionResponseCallback {
                        override fun onResponse(response: String?, request: String?) {
                            if (response != null && response.startsWith("RCV")) {
                                println("UDPTASK OUT :" + outgoingPort + " -> " + response)

                                val m = QOS_RECEIVE_RESPONSE_PATTERN.matcher(response)
                                if (m.find()) {
                                    outgoingPacketData.rcvServerResponse = m.group(1).toInt()
                                }

                                outgoingResultLatch.countDown()
                            }
                        }
                    }

                    sendCommand("GET UDPRESULT OUT $outgoingPort", outgoingResultRequestCallback)
                    outgoingResultLatch.await(CONTROL_CONNECTION_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
                }

                // run UDP IN test:
                if (packetCountIncoming != null && incomingPort != null) {
                    val dgSocket = DatagramSocket(incomingPort)
                    socket = dgSocket
                    sendCommand("UDPTEST IN " + incomingPort + " " + packetCountIncoming, null)
                    dgSocket.soTimeout = (timeout / 1000000).toInt()

                    val udpInTimeoutTask = RMBTClient.getCommonThreadPool().submit(object : Callable<UdpPacketData> {
                        override fun call(): UdpPacketData {
                            receiveUdpPackets(dgSocket, packetCountIncoming, incomingPacketData)
                            return incomingPacketData
                        }
                    })

                    try {
                        udpInTimeoutTask.get(timeout, TimeUnit.NANOSECONDS)
                    } catch (e: TimeoutException) {
                        System.err.println("UDP Incoming Timeout reached!")
                        udpInTimeoutTask.cancel(true)
                    }

                    val incomingLatch = CountDownLatch(1)
                    val incomingResultRequestCallback = object : ControlConnectionResponseCallback {
                        override fun onResponse(response: String?, request: String?) {
                            if (response != null && response.startsWith("RCV")) {
                                println("UDPTASK IN :" + incomingPort + " -> " + response)
                                val m = QOS_RECEIVE_RESPONSE_PATTERN.matcher(response)
                                if (m.find()) {
                                    incomingPacketData.rcvServerResponse = m.group(1).toInt()
                                }
                                incomingLatch.countDown()
                            }
                        }
                    }

                    // wait a short amount of time until requesting results
                    Thread.sleep(150)
                    // request server results:
                    sendCommand("GET UDPRESULT IN $incomingPort", incomingResultRequestCallback)
                    incomingLatch.await(CONTROL_CONNECTION_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                val s = socket
                if (s != null && s.isConnected) {
                    s.close()
                }
            }

            if (packetCountOutgoing != null) {
                println("OUT " + outgoingPort + ": " + outgoingPacketData)
                result.resultMap[RESULT_NUM_PACKETS_OUTGOING] = packetCountOutgoing
                result.resultMap[RESULT_PORT_OUTGOING] = outgoingPort
                result.resultMap[RESULT_OUTGOING_PACKETS] = outgoingPacketData.rcvServerResponse
                result.resultMap[RESULT_NUM_PACKETS_OUTGOING_RESPONSE] = outgoingPacketData.numPackets

                val outgoingPackets = outgoingPacketData.numPackets
                val lostPackets = packetCountOutgoing - outgoingPackets

                println("UDP Test: outgoing all: $outgoingPackets, lost: $lostPackets")
                if (lostPackets > 0) {
                    val packetLossRate = ((lostPackets.toFloat() / packetCountOutgoing.toFloat()) * 100f).toInt()
                    result.resultMap[RESULT_OUTGOING_PLR] = packetLossRate.toString()
                } else {
                    result.resultMap[RESULT_OUTGOING_PLR] = "0"
                }
            }

            if (packetCountIncoming != null && incomingPort != null) {
                println("IN " + incomingPort + ": " + incomingPacketData)
                val incomingPackets = incomingPacketData.rcvServerResponse

                result.resultMap[RESULT_NUM_PACKETS_INCOMING] = packetCountIncoming
                result.resultMap[RESULT_PORT_INCOMING] = incomingPort
                result.resultMap[RESULT_INCOMING_PACKETS] = incomingPacketData.numPackets
                result.resultMap[RESULT_NUM_PACKETS_INCOMING_RESPONSE] = incomingPackets

                val lostPackets = packetCountIncoming - incomingPackets
                if (lostPackets > 0) {
                    val packetLossRate = ((lostPackets.toFloat() / packetCountIncoming.toFloat()) * 100f).toInt()
                    result.resultMap[RESULT_INCOMING_PLR] = packetLossRate.toString()
                } else {
                    result.resultMap[RESULT_INCOMING_PLR] = "0"
                }
            }

            result.resultMap[RESULT_DELAY] = delay
            result.resultMap[RESULT_TIMEOUT] = timeout

            return result
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            onEnd(result)
        }
    }

    override fun initTask() {
    }

    fun sendUdpPackets(packetData: UdpPacketData): DatagramChannel? {
        val udpSettings = UdpStreamSenderSettings<DatagramChannel>(
            null, true,
            InetAddress.getByName(getTestServerAddr()), outgoingPort!!, packetCountOutgoing!!, delay,
            timeout, TimeUnit.NANOSECONDS, false, 10000
        )

        val udpStreamSender = NioUdpStreamSender(udpSettings, object : UdpStreamCallback {
            val packetsReceived = TreeSet<Int>()
            val duplicatePackets = TreeSet<Int>()

            override fun onSend(dataOut: DataOutputStream, packetNumber: Int): Boolean {
                println("UDP OUT Test: sending packet #$packetNumber")
                dataOut.writeByte(UDP_TEST_AWAIT_RESPONSE_IDENTIFIER)
                dataOut.writeByte(packetNumber)
                dataOut.write(params.uuid!!.toByteArray())
                dataOut.write(System.currentTimeMillis().toString().toByteArray())
                return true
            }

            @Synchronized
            override fun onReceive(dp: DatagramPacket) {
                val buffer = dp.data
                val packetNumber = buffer[1].toInt()

                println("UDP OUT Test: received packet: #$packetNumber -> $buffer")
                // check udp packet:
                if (buffer[0].toInt() != UDP_TEST_RESPONSE) {
                    udpSettings.socket?.close()
                    throw IOException("bad UDP IN TEST packet identifier")
                }

                // check for duplicate packets:
                if (packetsReceived.contains(packetNumber)) {
                    duplicatePackets.add(packetNumber)
                    if (ABORT_ON_DUPLICATE_UDP_PACKETS) {
                        udpSettings.socket?.close()
                        throw IOException("duplicate UDP IN TEST packet id")
                    } else {
                        println("duplicate UDP IN TEST packet id")
                    }
                } else {
                    packetsReceived.add(packetNumber)
                }

                packetData.numPackets = packetsReceived.size
                packetData.dupNumPackets = duplicatePackets.size
            }

            override fun onBind(port: Int?) {
                println("UDP; Binding on port $port")
            }
        })

        return udpStreamSender.send()
    }

    fun receiveUdpPackets(socket: DatagramSocket, packets: Int, packetData: UdpPacketData) {
        val packetsReceived = TreeSet<Int>()
        val duplicatePackets = TreeSet<Int>()

        try {
            val timeOutMs = TimeUnit.MILLISECONDS.convert(timeout, TimeUnit.NANOSECONDS).toInt()
            socket.soTimeout = timeOutMs

            val settings = UdpStreamReceiverSettings(socket, packets, true)

            val udpStreamReceiver = UdpStreamReceiver(settings, object : UdpStreamCallback {

                override fun onSend(dataOut: DataOutputStream, packetNumber: Int): Boolean {
                    dataOut.writeByte(UDP_TEST_RESPONSE)
                    dataOut.writeByte(packetNumber)
                    dataOut.write(params.uuid!!.toByteArray())
                    dataOut.write(System.currentTimeMillis().toString().toByteArray())
                    return true
                }

                override fun onReceive(dp: DatagramPacket) {
                    val data = dp.data
                    val packetNumber = data[1].toInt()

                    println("UDP IN Test: received packet #$packetNumber on port: " + socket.localPort + " -> " + data)

                    // check udp packet:
                    if (data[0].toInt() != UDP_TEST_ONE_DIRECTION_IDENTIFIER && data[0].toInt() != UDP_TEST_AWAIT_RESPONSE_IDENTIFIER) {
                        throw IOException("bad UDP IN TEST packet identifier")
                    }

                    // check for duplicate packets:
                    if (packetsReceived.contains(packetNumber)) {
                        duplicatePackets.add(packetNumber)
                        if (ABORT_ON_DUPLICATE_UDP_PACKETS) {
                            throw IOException("duplicate UDP IN TEST packet id")
                        }
                    } else {
                        packetsReceived.add(packetNumber)
                    }

                    packetData.dupNumPackets = duplicatePackets.size
                    packetData.numPackets = packetsReceived.size
                }

                override fun onBind(port: Int?) {
                }
            })

            udpStreamReceiver.receive()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (!socket.isClosed) {
                socket.close()
            }
        }
    }

    override fun getTestType(): QoSTestResultEnum = QoSTestResultEnum.UDP

    override fun needsQoSControlConnection(): Boolean = true

    companion object {
        private const val ABORT_ON_DUPLICATE_UDP_PACKETS = false

        private val QOS_RECEIVE_RESPONSE_PATTERN: Pattern = Pattern.compile("RCV ([\\d]*) ([\\d]*)")

        private const val DEFAULT_TIMEOUT = 3000000000L

        private const val DEFAULT_DELAY = 300000000L

        private const val UDP_TEST_ONE_DIRECTION_IDENTIFIER = 1

        private const val UDP_TEST_AWAIT_RESPONSE_IDENTIFIER = 3

        private const val UDP_TEST_RESPONSE = 2

        const val PARAM_NUM_PACKETS_INCOMING = "in_num_packets"

        const val PARAM_NUM_PACKETS_OUTGOING = "out_num_packets"

        const val PARAM_PORT = "in_port"

        const val PARAM_PORT_OUT = "out_port"

        const val PARAM_TIMEOUT = "timeout"

        const val PARAM_DELAY = "delay"

        const val RESULT_OUTGOING_PACKETS = "udp_result_out_num_packets"

        const val RESULT_INCOMING_PACKETS = "udp_result_in_num_packets"

        const val RESULT_INCOMING_PLR = "udp_result_in_packet_loss_rate"

        const val RESULT_NUM_PACKETS_INCOMING_RESPONSE = "udp_result_in_response_num_packets"

        const val RESULT_OUTGOING_PLR = "udp_result_out_packet_loss_rate"

        const val RESULT_NUM_PACKETS_OUTGOING_RESPONSE = "udp_result_out_response_num_packets"

        const val RESULT_PORT_OUTGOING = "udp_objective_out_port"

        const val RESULT_PORT_INCOMING = "udp_objective_in_port"

        const val RESULT_NUM_PACKETS_INCOMING = "udp_objective_in_num_packets"

        const val RESULT_NUM_PACKETS_OUTGOING = "udp_objective_out_num_packets"

        const val RESULT_DELAY = "udp_objective_delay"

        const val RESULT_TIMEOUT = "udp_objective_timeout"
    }
}
