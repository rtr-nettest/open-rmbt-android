package at.rtr.rmbt.client

import at.rtr.rmbt.util.Helperfunctions
import at.rtr.rmbt.util.tools.TracerouteService
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.ArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

class TracerouteAndroidImpl : TracerouteService {

    class PingException(msg: String) : IOException(msg) {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    class PingDetailImpl(pingResult: String, durationNs: Long) : TracerouteService.HopDetail {
        private val transmitted: Int
        private val received: Int
        private val errors: Int
        private val packetLoss: Int
        private var time: Long
        private var fromIp: String?

        init {
            println(pingResult)

            time = durationNs
            val pingPacket = PATTERN_PING_PACKET.matcher(pingResult)

            if (pingPacket.find()) {
                transmitted = pingPacket.group(1)!!.toInt()
                received = pingPacket.group(2)!!.toInt()
                val errorsStr = pingPacket.group(4)
                errors = errorsStr?.toInt() ?: 0
                packetLoss = pingPacket.group(5)!!.toInt()
            } else {
                transmitted = 0
                received = 0
                packetLoss = 0
                errors = 0
            }

            val fromIpMatcher = PATTERN_FROM_IP.matcher(pingResult)
            if (fromIpMatcher.find()) {
                fromIp = fromIpMatcher.group(1)
                val timeStr = fromIpMatcher.group(4)
                if (timeStr != null) {
                    this.time = TimeUnit.NANOSECONDS.convert((timeStr.toFloat() * 1000f).toInt().toLong(), TimeUnit.MICROSECONDS)
                }
            } else {
                fromIp = "*"
            }
        }

        fun getTime(): Long = time

        fun setTime(time: Long) {
            this.time = time
        }

        fun getTransmitted(): Int = transmitted

        fun getReceived(): Int = received

        fun getErrors(): Int = errors

        fun getPacketLoss(): Int = packetLoss

        fun getFromIp(): String? = fromIp

        override fun toString(): String =
            "PingDetail [transmitted=$transmitted, received=$received, errors=$errors, packetLoss=$packetLoss, " +
                "time=" + (time / 1000000) + "ms, fromIp=" + fromIp + "]"

        override fun toJson(masked: Boolean?): JSONObject? {
            val json = JSONObject()
            return try {
                if (masked == true) {
                    fromIp = try {
                        val ip = InetAddress.getByName(fromIp)
                        Helperfunctions.anonymizeIp(ip, ".x")
                    } catch (e: UnknownHostException) {
                        "*"
                    }
                }
                json.put("host", fromIp)
                json.put("time", time)
                json
            } catch (e: JSONException) {
                e.printStackTrace()
                null
            }
        }

        companion object {
            @JvmField
            val PATTERN_PING_PACKET: Pattern =
                Pattern.compile("([\\d]*) packets transmitted, ([\\d]*) received, ([+-]?([\\d]*) errors, )?([\\d]*)% packet loss, time ([\\d]*)ms")

            @JvmField
            val PATTERN_FROM_IP: Pattern =
                Pattern.compile("[fF]rom ([\\.\\-_\\d\\w\\s\\(\\)]*)(:|icmp)+(.*time=([\\d\\.]*))?")
        }
    }

    private var host: String? = null
    private var maxHops = 0
    private val isRunning = AtomicBoolean(false)
    private var hasMaxHopsExceeded = true
    private var resultList: MutableList<TracerouteService.HopDetail>? = null

    override fun getHost(): String? = host

    override fun setHost(host: String?) {
        this.host = host
    }

    override fun getMaxHops(): Int = maxHops

    override fun setMaxHops(maxHops: Int) {
        this.maxHops = maxHops
    }

    @Throws(Exception::class)
    override fun call(): List<TracerouteService.HopDetail> {
        isRunning.set(true)
        var list = resultList
        if (list == null) {
            list = ArrayList()
            resultList = list
        }

        val runtime = Runtime.getRuntime()

        for (i in 1..maxHops) {
            if (Thread.interrupted() || !isRunning.get()) {
                throw InterruptedException()
            }
            val ts = System.nanoTime()
            // ping  -c <count> -t <ttl>  -W <timeout> <host>
            val mIpAddrProcess = runtime.exec("/system/bin/ping -n -c 1 -t " + i + " -W2 " + host)
            // result: From 4.5.6.7: icmp_seq=1 Time to live exceeded
            val proc = readFromProcess(mIpAddrProcess)
            val pingDetail = PingDetailImpl(proc, System.nanoTime() - ts)
            list.add(pingDetail)
            if (pingDetail.getReceived() > 0) {
                hasMaxHopsExceeded = false
                break
            }
        }

        return list
    }

    /**
     * stop the ping tool task
     * @return true if task has been stopped or false if it was not running
     */
    fun stop(): Boolean = isRunning.getAndSet(false)

    override fun hasMaxHopsExceeded(): Boolean = hasMaxHopsExceeded

    override fun setResultListObject(resultList: List<TracerouteService.HopDetail>?) {
        @Suppress("UNCHECKED_CAST")
        this.resultList = resultList as MutableList<TracerouteService.HopDetail>?
    }

    companion object {
        @JvmStatic
        @Throws(PingException::class)
        fun readFromProcess(proc: Process): String {
            var brErr: BufferedReader? = null
            var br: BufferedReader? = null
            val sbErr = StringBuilder()
            val sb = StringBuilder()

            try {
                brErr = BufferedReader(InputStreamReader(proc.errorStream))
                var currInputLine: String?

                while (brErr.readLine().also { currInputLine = it } != null) {
                    sbErr.append(currInputLine)
                    sbErr.append("\n")
                }

                if (sbErr.isNotEmpty()) {
                    throw PingException(sbErr.toString())
                }

                br = BufferedReader(InputStreamReader(proc.inputStream))

                while (br.readLine().also { currInputLine = it } != null) {
                    sb.append(currInputLine)
                    sb.append("\n")
                }
            } catch (e: PingException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    br?.close()
                } catch (e: IOException) {
                }
                try {
                    brErr?.close()
                } catch (e: IOException) {
                }
            }

            return sb.toString()
        }
    }
}
