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
package at.rtr.rmbt.client

import at.rtr.rmbt.client.helper.Config
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Locale
import java.util.Scanner
import java.util.regex.Pattern
import javax.net.ssl.SSLSocket

abstract class AbstractRMBTTest(
    @JvmField protected val client: RMBTClient,
    @JvmField protected val params: RMBTTestParameter,
    @JvmField protected val threadId: Int
) {

    @JvmField
    protected var `in`: InputStreamCounter? = null

    @JvmField
    protected var reader: BufferedReader? = null

    @JvmField
    protected var out: OutputStreamCounter? = null

    @JvmField
    protected var totalDown: Long = 0

    @JvmField
    protected var totalUp: Long = 0

    @JvmField
    protected var chunksize = 0

    @JvmField
    protected var buf: ByteArray? = null

    @Throws(IOException::class)
    protected fun getSocket(host: String, port: Int, isSecure: Boolean, timeOut: Int): Socket {
        val sockAddr = InetSocketAddress(host, port)

        val factory = client.sslSocketFactory
        val socket: Socket = if (factory != null && isSecure) {
            factory.createSocket()
        } else {
            Socket()
        }

        println("Connecting to $sockAddr with timout: $timeOut" + "ms " + socket + " [SSL: " + isSecure + "]")
        socket.connect(sockAddr, timeOut)

        return socket
    }

    @Throws(IOException::class)
    protected fun connect(
        testResult: TestResult?,
        host: InetAddress,
        port: Int,
        protocolVersion: String,
        response: String,
        isSecure: Boolean,
        connTimeOut: Int
    ): Socket? {
        log(String.format(Locale.US, "thread %d: connecting...", threadId))

        val s = getSocket(host.hostAddress!!, port, isSecure, connTimeOut)
        s.soTimeout = 12000

        if (testResult != null) {
            testResult.ip_local = s.localAddress
            testResult.ip_server = s.inetAddress
            testResult.port_remote = s.port
        }

        if (s is SSLSocket) {
            val session = s.session
            if (testResult != null) {
                testResult.encryption = String.format(Locale.US, "%s (%s)", session.protocol, session.cipherSuite)
            }
        }

        log(String.format(Locale.US, "thread %d: ReceiveBufferSize: '%s'.", threadId, s.receiveBufferSize))
        log(String.format(Locale.US, "thread %d: SendBufferSize: '%s'.", threadId, s.sendBufferSize))

        `in`?.let { totalDown += it.count }
        out?.let { totalUp += it.count }

        val inputCounter = InputStreamCounter(s.getInputStream())
        `in` = inputCounter
        val r = BufferedReader(InputStreamReader(inputCounter, "US-ASCII"), 4096)
        reader = r
        val outputCounter = OutputStreamCounter(s.getOutputStream())
        out = outputCounter

        var line = r.readLine()
        if (line != protocolVersion) {
            log(String.format(Locale.US, "thread %d: got '%s' expected '%s'", threadId, line, EXPECT_GREETING))
            return null
        }

        line = r.readLine()
        if (line == null || !line.startsWith("ACCEPT ")) {
            log(String.format(Locale.US, "thread %d: got '%s' expected 'ACCEPT'", threadId, line))
            return null
        }

        val send = String.format(Locale.US, "TOKEN %s\n", params.token)

        outputCounter.write(send.toByteArray(charset("US-ASCII")))

        line = r.readLine()

        if (line == null) {
            log(String.format(Locale.US, "thread %d: got no answer expected 'OK'", threadId, line))
            return null
        } else if (line != "OK") {
            log(String.format(Locale.US, "thread %d: got '%s' expected 'OK'", threadId, line))
            return null
        }

        line = r.readLine()
        val scanner = Scanner(line)
        try {
            if (response == "CHUNKSIZE") {
                if (response != scanner.next()) {
                    log(String.format(Locale.US, "thread %d: got '%s' expected 'CHUNKSIZE'", threadId, line))
                    return null
                }
                try {
                    chunksize = scanner.nextInt()
                    log(String.format(Locale.US, "thread %d: CHUNKSIZE is %d", threadId, chunksize))
                } catch (e: Exception) {
                    log(String.format(Locale.US, "thread %d: invalid CHUNKSIZE: '%s'", threadId, line))
                    return null
                }
                val currentBuf = buf
                if (currentBuf == null || currentBuf.size != chunksize) {
                    buf = ByteArray(chunksize)
                }

                s.soTimeout = 0
                return s
            } else {
                log(String.format(Locale.US, "thread %d: got '%s'", threadId, line))
                s.soTimeout = 0
                return s
            }
        } finally {
            scanner.close()
        }
    }

    @Throws(IOException::class)
    protected open fun connect(testResult: TestResult?): Socket? {
        return connect(testResult, InetAddress.getByName(params.host), params.port, EXPECT_GREETING, "CHUNKSIZE", true, 20000)
    }

    @Throws(IOException::class)
    protected fun sendMessage(message: String) {
        val send = String.format(Locale.US, message)

        println("sending command (thread " + Thread.currentThread().id + "): " + send)
        out!!.write(send.toByteArray(charset("US-ASCII")))
        out!!.flush()
    }

    protected fun log(text: CharSequence?) {
        if (client != null) {
            client.log(text)
        } else {
            if (text != null) Timber.d(text.toString())
        }
    }

    companion object {
        @JvmField
        protected val EXPECT_GREETING: String = Config.RMBT_SERVER_NAME

        @JvmField
        protected val RMBT_SERVER_PATTERN: Pattern = Pattern.compile(Config.RMBT_VERSION_EXPRESSION)
    }
}
