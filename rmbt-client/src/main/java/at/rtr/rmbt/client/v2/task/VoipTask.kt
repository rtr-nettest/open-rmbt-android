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
import at.rtr.rmbt.client.v2.task.result.QoSTestResult
import at.rtr.rmbt.client.v2.task.result.QoSTestResultEnum
import at.rtr.rmbt.util.net.rtp.RealtimeTransportProtocol.PayloadType
import at.rtr.rmbt.util.net.rtp.RealtimeTransportProtocol.RtpException
import at.rtr.rmbt.util.net.rtp.RtpPacket
import at.rtr.rmbt.util.net.rtp.RtpUtil
import at.rtr.rmbt.util.net.rtp.RtpUtil.RtpControlData
import at.rtr.rmbt.util.net.udp.StreamSender.UdpStreamCallback
import java.io.DataOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.channels.DatagramChannel
import java.util.HashMap
import java.util.Random
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

/**
 * As of RFC 3550 and RFC 3551 most RTP (VoIP) Codecs have a sampling rate of 8kHz.
 *
 * @author lb
 */
class VoipTask(
    nnTest: QualityOfServiceTest,
    taskDesc: TaskDesc,
    threadId: Int,
    customTimeout: Long?,
    private val ignoreErrors: Boolean
) : AbstractQoSTask(nnTest, taskDesc, threadId, threadId) {

    private val outgoingPort: Int?
    private val incomingPort: Int?
    private val callDuration: Long
    private val timeout: Long
    private val delay: Long
    private val sampleRate: Int
    private val bitsPerSample: Int
    private val payloadType: PayloadType

    init {
        var value = taskDesc.getParams()[PARAM_DURATION] as String?
        this.callDuration = if (value != null) value.toLong() else DEFAULT_CALL_DURATION

        value = taskDesc.getParams()[PARAM_PORT] as String?
        this.incomingPort = value?.toInt()

        value = taskDesc.getParams()[PARAM_PORT_OUT] as String?
        this.outgoingPort = value?.toInt()

        if (customTimeout == null) {
            value = taskDesc.getParams()[PARAM_TIMEOUT] as String?
            this.timeout = if (value != null) value.toLong() else DEFAULT_TIMEOUT
        } else {
            this.timeout = customTimeout
        }

        value = taskDesc.getParams()[PARAM_DELAY] as String?
        this.delay = if (value != null) value.toLong() else DEFAULT_DELAY

        value = taskDesc.getParams()[PARAM_BITS_PER_SAMLE] as String?
        this.bitsPerSample = if (value != null) value.toInt() else DEFAULT_BITS_PER_SAMPLE

        value = taskDesc.getParams()[PARAM_SAMPLE_RATE] as String?
        this.sampleRate = if (value != null) value.toInt() else DEFAULT_SAMPLE_RATE

        value = taskDesc.getParams()[PARAM_PAYLOAD] as String?
        this.payloadType = if (value != null) PayloadType.getByCodecValue(value.toInt(), DEFAULT_PAYLOAD_TYPE) else DEFAULT_PAYLOAD_TYPE
    }

    override fun call(): QoSTestResult {
        val ssrc = AtomicInteger(-1)
        val result = initQoSTestResult(QoSTestResultEnum.VOIP)

        result.resultMap[RESULT_BITS_PER_SAMPLE] = bitsPerSample
        result.resultMap[RESULT_CALL_DURATION] = callDuration
        result.resultMap[RESULT_DELAY] = delay
        result.resultMap[RESULT_IN_PORT] = incomingPort
        result.resultMap[RESULT_OUT_PORT] = outgoingPort
        result.resultMap[RESULT_SAMPLE_RATE] = sampleRate
        result.resultMap[RESULT_PAYLOAD] = payloadType.value
        result.resultMap[RESULT_STATUS] = "OK"

        try {
            onStart(result)

            val r = Random()
            val initialSequenceNumber = r.nextInt(10000)
            val latch = CountDownLatch(1)
            val rtpControlDataList: MutableMap<Int, RtpControlData> = HashMap()

            val callback = object : ControlConnectionResponseCallback {
                override fun onResponse(response: String?, request: String?) {
                    if (response != null && response.startsWith("OK")) {
                        val m = VOIP_OK_PATTERN.matcher(response)
                        if (m.find()) {
                            var dgsock: DatagramSocket? = null
                            try {
                                ssrc.set(m.group(1).toInt())
                                dgsock = DatagramSocket()

                                val receiveCallback = object : UdpStreamCallback {
                                    override fun onSend(dataOut: DataOutputStream, packetNumber: Int): Boolean {
                                        // nothing to do here
                                        return true
                                    }

                                    @Synchronized
                                    override fun onReceive(dp: DatagramPacket) {
                                        val receivedNs = System.nanoTime()
                                        val data = dp.data
                                        try {
                                            val rtp = RtpPacket(data)
                                            rtpControlDataList[rtp.getSequnceNumber()] = RtpControlData(rtp, receivedNs)
                                        } catch (e: RtpException) {
                                            e.printStackTrace()
                                        }
                                    }

                                    override fun onBind(port: Int?) {
                                        result.resultMap[RESULT_IN_PORT] = port
                                    }
                                }

                                RtpUtil.runVoipStream<DatagramChannel>(
                                    null, true, InetAddress.getByName(getTestServerAddr()), outgoingPort!!, incomingPort, sampleRate, bitsPerSample,
                                    payloadType, initialSequenceNumber.toLong(), ssrc.get(),
                                    TimeUnit.MILLISECONDS.convert(callDuration, TimeUnit.NANOSECONDS),
                                    TimeUnit.MILLISECONDS.convert(delay, TimeUnit.NANOSECONDS),
                                    TimeUnit.MILLISECONDS.convert(timeout, TimeUnit.NANOSECONDS), true, receiveCallback
                                )
                            } catch (e: InterruptedException) {
                                if (!ignoreErrors) {
                                    result.resultMap[RESULT_STATUS] = "TIMEOUT"
                                    e.printStackTrace()
                                }
                            } catch (e: TimeoutException) {
                                if (!ignoreErrors) {
                                    result.resultMap[RESULT_STATUS] = "TIMEOUT"
                                    e.printStackTrace()
                                }
                            } catch (e: Exception) {
                                if (!ignoreErrors) {
                                    result.resultMap[RESULT_STATUS] = "ERROR"
                                    e.printStackTrace()
                                }
                            } finally {
                                if (dgsock != null && !dgsock.isClosed) {
                                    dgsock.close()
                                }
                            }
                        }
                    } else {
                        if (!ignoreErrors) {
                            result.resultMap[RESULT_STATUS] = "ERROR"
                        }
                    }

                    latch.countDown()
                }
            }

            /*
             * syntax: VOIPTEST 0 1 2 3 4 5 6 7
             *  0 = outgoing port (server port)
             *  1 = incoming port (client port)
             *  2 = sample rate (in Hz)
             *  3 = bits per sample
             *  4 = packet delay in ms
             *  5 = call duration (test duration) in ms
             *  6 = starting sequence number (see rfc3550, rtp header: sequence number)
             *  7 = payload type
             */
            sendCommand(
                "VOIPTEST " + outgoingPort + " " + (if (incomingPort == null) outgoingPort else incomingPort) + " " + sampleRate + " " + bitsPerSample + " " +
                    TimeUnit.MILLISECONDS.convert(delay, TimeUnit.NANOSECONDS) + " " +
                    TimeUnit.MILLISECONDS.convert(callDuration, TimeUnit.NANOSECONDS) + " " +
                    initialSequenceNumber + " " + payloadType.value,
                callback
            )

            // wait for countdownlatch or timeout:
            latch.await(timeout, TimeUnit.NANOSECONDS)

            val resultLatch = CountDownLatch(1)

            val incomingResultRequestCallback = object : ControlConnectionResponseCallback {
                override fun onResponse(response: String?, request: String?) {
                    if (response != null && response.startsWith("VOIPRESULT")) {
                        println(response)
                        val m = VOIP_RECEIVE_RESPONSE_PATTERN.matcher(response)
                        if (m.find()) {
                            val prefix = RESULT_VOIP_PREFIX + RESULT_OUTGOING_PREFIX
                            result.resultMap[prefix + RESULT_MAX_JITTER] = m.group(1).toLong()
                            result.resultMap[prefix + RESULT_MEAN_JITTER] = m.group(2).toLong()
                            result.resultMap[prefix + RESULT_MAX_DELTA] = m.group(3).toLong()
                            result.resultMap[prefix + RESULT_SKEW] = m.group(4).toLong()
                            result.resultMap[prefix + RESULT_NUM_PACKETS] = m.group(5).toLong()
                            result.resultMap[prefix + RESULT_SEQUENCE_ERRORS] = m.group(6).toLong()
                            result.resultMap[prefix + RESULT_SHORT_SEQUENTIAL] = m.group(7).toLong()
                            result.resultMap[prefix + RESULT_LONG_SEQUENTIAL] = m.group(8).toLong()
                        }
                        resultLatch.countDown()
                    }
                }
            }

            // wait a short amount of time until requesting results
            Thread.sleep(100)
            // request server results:
            if (ssrc.get() >= 0) {
                sendCommand("GET VOIPRESULT " + ssrc.get(), incomingResultRequestCallback)
                resultLatch.await(CONTROL_CONNECTION_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
            }

            val rtpResults = if (rtpControlDataList.size > 0) RtpUtil.calculateQoS(rtpControlDataList, initialSequenceNumber.toLong(), sampleRate) else null

            val prefix = RESULT_VOIP_PREFIX + RESULT_INCOMING_PREFIX
            if (rtpResults != null) {
                result.resultMap[prefix + RESULT_MAX_JITTER] = rtpResults.maxJitter
                result.resultMap[prefix + RESULT_MEAN_JITTER] = rtpResults.meanJitter
                result.resultMap[prefix + RESULT_MAX_DELTA] = rtpResults.maxDelta
                result.resultMap[prefix + RESULT_SKEW] = rtpResults.skew
                result.resultMap[prefix + RESULT_NUM_PACKETS] = rtpResults.receivedPackets
                result.resultMap[prefix + RESULT_SEQUENCE_ERRORS] = rtpResults.outOfOrder
                result.resultMap[prefix + RESULT_SHORT_SEQUENTIAL] = rtpResults.minSequential
                result.resultMap[prefix + RESULT_LONG_SEQUENTIAL] = rtpResults.maxSequencial
            } else {
                result.resultMap[prefix + RESULT_MAX_JITTER] = null
                result.resultMap[prefix + RESULT_MEAN_JITTER] = null
                result.resultMap[prefix + RESULT_MAX_DELTA] = null
                result.resultMap[prefix + RESULT_SKEW] = null
                result.resultMap[prefix + RESULT_NUM_PACKETS] = 0
                result.resultMap[prefix + RESULT_SEQUENCE_ERRORS] = null
                result.resultMap[prefix + RESULT_SHORT_SEQUENTIAL] = null
                result.resultMap[prefix + RESULT_LONG_SEQUENTIAL] = null
            }

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

    override fun getTestType(): QoSTestResultEnum = QoSTestResultEnum.VOIP

    override fun needsQoSControlConnection(): Boolean = true

    companion object {
        private val VOIP_RECEIVE_RESPONSE_PATTERN: Pattern =
            Pattern.compile("VOIPRESULT (-?[\\d]*) (-?[\\d]*) (-?[\\d]*) (-?[\\d]*) (-?[\\d]*) (-?[\\d]*) (-?[\\d]*) (-?[\\d]*)")

        private val VOIP_OK_PATTERN: Pattern = Pattern.compile("OK ([\\d]*)")

        private const val DEFAULT_TIMEOUT = 3000000000L // 3s

        private const val DEFAULT_CALL_DURATION = 1000000000L // 1s

        private const val DEFAULT_DELAY = 20000000L // 20ms

        private const val DEFAULT_SAMPLE_RATE = 8000 // 8kHz

        private const val DEFAULT_BITS_PER_SAMPLE = 8 // 8 bits per sample

        private val DEFAULT_PAYLOAD_TYPE = PayloadType.PCMA

        const val PARAM_BITS_PER_SAMLE = "bits_per_sample"

        const val PARAM_SAMPLE_RATE = "sample_rate"

        const val PARAM_DURATION = "call_duration" // call duration in ns

        const val PARAM_PORT = "in_port"

        const val PARAM_PORT_OUT = "out_port"

        const val PARAM_TIMEOUT = "timeout"

        const val PARAM_DELAY = "delay"

        const val PARAM_PAYLOAD = "payload"

        const val RESULT_PAYLOAD = "voip_objective_payload"

        const val RESULT_IN_PORT = "voip_objective_in_port"

        const val RESULT_OUT_PORT = "voip_objective_out_port"

        const val RESULT_CALL_DURATION = "voip_objective_call_duration"

        const val RESULT_BITS_PER_SAMPLE = "voip_objective_bits_per_sample"

        const val RESULT_SAMPLE_RATE = "voip_objective_sample_rate"

        const val RESULT_DELAY = "voip_objective_delay"

        const val RESULT_TIMEOUT = "voip_objective_timeout"

        const val RESULT_STATUS = "voip_result_status"

        const val RESULT_VOIP_PREFIX = "voip_result"

        const val RESULT_INCOMING_PREFIX = "_in_"

        const val RESULT_OUTGOING_PREFIX = "_out_"

        const val RESULT_SHORT_SEQUENTIAL = "short_seq"

        const val RESULT_LONG_SEQUENTIAL = "long_seq"

        const val RESULT_MAX_JITTER = "max_jitter"

        const val RESULT_MEAN_JITTER = "mean_jitter"

        const val RESULT_MAX_DELTA = "max_delta"

        const val RESULT_SKEW = "skew"

        const val RESULT_NUM_PACKETS = "num_packets"

        const val RESULT_SEQUENCE_ERRORS = "sequence_error"
    }
}
