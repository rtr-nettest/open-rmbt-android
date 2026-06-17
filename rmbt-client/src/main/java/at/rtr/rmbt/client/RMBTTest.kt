/*******************************************************************************
 * Copyright 2013-2015 alladin-IT GmbH
 * Copyright 2013-2017 Rundfunk und Telekom Regulierungs-GmbH (RTR-GmbH)
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
import at.rtr.rmbt.client.helper.TestStatus
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.Socket
import java.util.ArrayList
import java.util.Collections
import java.util.InputMismatchException
import java.util.Locale
import java.util.Scanner
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern
import javax.net.ssl.SSLSocket

class RMBTTest(
    client: RMBTClient,
    params: RMBTTestParameter,
    threadId: Int,
    private val barrier: CyclicBarrier,
    storeResults: Int,
    private val minDiffTime: Long,
    private val fallbackToOneThread: AtomicBoolean
) : AbstractRMBTTest(client, params, threadId), Callable<ThreadTestResult?> {

    private val doDownload = true
    private val doUpload = true

    private val curTransfer = AtomicLong()
    private val curTime = AtomicLong()

    private val maxCoarseResults: Int = storeResults
    private val maxFineResults: Int = storeResults

    private inner class SingleResult {
        private val fine: Results = Results(maxFineResults)
        private val coarse: Results = Results(maxCoarseResults)

        private var fineResults = 0
        private var coarseResults = 0

        override fun toString(): String =
            "SingleResult [fine=$fine, coarse=$coarse, fineResults=$fineResults, coarseResults=$coarseResults]"

        fun addResult(newBytes: Long, newNsec: Long) {
            var addToCoarse = coarseResults == 0
            if (!addToCoarse) {
                val diffTime = newNsec - coarse.nsec[(coarseResults - 1) % coarse.nsec.size]
                if (diffTime > minDiffTime) {
                    addToCoarse = true
                }
            }

            if (coarse.bytes.isNotEmpty()) {
                if (addToCoarse) {
                    val coarsePos = coarseResults++ % coarse.bytes.size
                    coarse.bytes[coarsePos] = newBytes
                    coarse.nsec[coarsePos] = newNsec
                }

                val finePos = fineResults++ % fine.bytes.size
                fine.bytes[finePos] = newBytes
                fine.nsec[finePos] = newNsec
            }
        }

        fun getBytes(): Long {
            return if (fineResults == 0) 0 else fine.bytes[(fineResults - 1) % fine.bytes.size]
        }

        fun getNsec(): Long {
            return if (fineResults == 0) 0 else fine.nsec[(fineResults - 1) % fine.nsec.size]
        }

        fun getAllResults(): Results {
            val numResultsCoarse = Math.min(coarseResults, maxCoarseResults)
            val numResultsFine = Math.min(fineResults, maxFineResults)
            val numResults = numResultsCoarse + numResultsFine

            var resultBytes = LongArray(numResults)
            var resultNsec = LongArray(numResults)

            var results = 0
            var posCoarse = coarseResults - numResultsCoarse
            var posFine = fineResults - numResultsFine

            while (results < numResults && (posCoarse < coarseResults || posFine < fineResults)) {
                val coarseAvail = posCoarse < coarseResults
                val fineAvail = posFine < fineResults
                val thisCoarse = if (coarseAvail) coarse.nsec[posCoarse % coarse.nsec.size] else -1
                val thisFine = if (fineAvail) fine.nsec[posFine % fine.nsec.size] else -1

                if ((thisFine <= thisCoarse || thisCoarse == -1L) && fineAvail) {
                    resultNsec[results] = thisFine
                    resultBytes[results++] = fine.bytes[posFine++ % fine.bytes.size]

                    if (thisFine == thisCoarse && coarseAvail) {
                        posCoarse++
                    }
                } else if ((thisCoarse < thisFine || thisFine == -1L) && coarseAvail) {
                    resultNsec[results] = thisCoarse
                    resultBytes[results++] = coarse.bytes[posCoarse++ % coarse.bytes.size]
                } else {
                    // shouldn't happen; avoid endless loop
                    break
                }
            }

            if (results < numResults) {
                val newResultBytes = LongArray(results)
                val newResultNsec = LongArray(results)
                System.arraycopy(resultBytes, 0, newResultBytes, 0, results)
                System.arraycopy(resultNsec, 0, newResultNsec, 0, results)
                resultBytes = newResultBytes
                resultNsec = newResultNsec
            }
            return Results(resultBytes, resultNsec)
        }

        fun addCoarseSpeedItems(list: MutableList<SpeedItem>, upload: Boolean, thread: Int) {
            var lastNsec: Long = 0
            val numResultsCoarse = Math.min(coarseResults, maxCoarseResults)
            for (i in 0 until numResultsCoarse) {
                val nsec = coarse.nsec[i % coarse.nsec.size]
                val bytes = coarse.bytes[i % coarse.bytes.size]
                val item = SpeedItem(upload, thread, nsec, bytes)
                client.onSpeedDataChanged(thread, bytes, nsec, upload)
                list.add(item)
                lastNsec = nsec
            }

            val nsec = getNsec()
            if (nsec > lastNsec) {
                val bytes = getBytes()
                val item = SpeedItem(upload, thread, nsec, bytes)
                client.onSpeedDataChanged(thread, bytes, nsec, upload)
                list.add(item)
            }
        }
    }

    class CurrentSpeed {
        var trans: Long = 0

        var time: Long = 0

        override fun toString(): String = "CurrentSpeed [trans=$trans, time=$time]"
    }

    fun getCurrentSpeed(result: CurrentSpeed?): CurrentSpeed {
        val r = result ?: CurrentSpeed()
        r.trans = curTransfer.get()
        r.time = curTime.get()
        return r
    }

    override fun connect(testResult: TestResult?): Socket? {
        val tr = testResult!!
        log(String.format(Locale.US, "thread %d: connecting...", threadId))

        val inetAddress = InetAddress.getByName(params.host)

        println("connecting to: " + inetAddress.hostName + ":" + params.port)
        val s = getSocket(inetAddress.hostAddress!!, params.port, true, 20000)

        tr.ip_local = s.localAddress
        tr.ip_server = s.inetAddress

        tr.port_remote = s.port

        if (s is SSLSocket) {
            val session = s.session
            tr.encryption = String.format(Locale.US, "%s (%s)", session.protocol, session.cipherSuite)
        }

        log(String.format(Locale.US, "thread %d: ReceiveBufferSize: '%s'.", threadId, s.receiveBufferSize))
        log(String.format(Locale.US, "thread %d: SendBufferSize: '%s'.", threadId, s.sendBufferSize))

        `in`?.let { totalDown += it.count }
        out?.let { totalUp += it.count }

        val inCounter = InputStreamCounter(s.getInputStream())
        `in` = inCounter
        val rdr = BufferedReader(InputStreamReader(inCounter, "US-ASCII"), 4096)
        reader = rdr
        val outCounter = OutputStreamCounter(s.getOutputStream())
        out = outCounter

        var line: String?

        // Server type RMBThttp -> The client has to do a HTTP request and upgrade the connection
        if (params.serverType == Config.SERVER_TYPE_RMBT_HTTP) {
            log(String.format(Locale.US, "thread %d: requesting HTTP upgrade", threadId))
            // Request RMBT test
            val request = String.format(
                "GET /rmbt HTTP/1.1\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Upgrade: RMBT\r\n" +
                    "RMBT-Version: %s\r\n" +
                    "\r\n",
                Config.RMBT_LATEST_SERVER
            )

            outCounter.write(request.toByteArray(charset("US-ASCII")))
            outCounter.flush()

            line = rdr.readLine()

            // Read the HTTP response (terminated with an empty newline)
            if (!line!!.contains("101")) { // HTTP status code 101 Switching Protocols
                log(String.format(Locale.US, "thread %d: got '%s' expected '%s'", threadId, line, EXPECT_GREETING))
                return null
            }
            while (line != "\r\n" && !line!!.isEmpty()) {
                line = rdr.readLine()
            }
        }
        // At this point, the communication is based on RMBT
        line = rdr.readLine()
        if (line!!.contains(EXPECT_GREETING)) {
            line = line.trim()
            val matcher = RMBT_SERVER_PATTERN.matcher(line)
            if (matcher.find()) {
                tr.client_version = matcher.group(1)
            }
        } else {
            log(String.format(Locale.US, "thread %d: got '%s' expected '%s'", threadId, line, EXPECT_GREETING))
            return null
        }

        line = rdr.readLine()
        if (!line!!.startsWith("ACCEPT ")) {
            log(String.format(Locale.US, "thread %d: got '%s' expected 'ACCEPT'", threadId, line))
            return null
        }

        val send = String.format(Locale.US, "TOKEN %s\n", params.token)

        outCounter.write(send.toByteArray(charset("US-ASCII")))

        line = rdr.readLine()

        if (line == null) {
            log(String.format(Locale.US, "thread %d: got no answer expected 'OK'", threadId, line))
            return null
        } else if (line != "OK") {
            log(String.format(Locale.US, "thread %d: got '%s' expected 'OK'", threadId, line))
            return null
        }

        line = rdr.readLine()
        val scanner = Scanner(line)
        try {
            if ("CHUNKSIZE" != scanner.next()) {
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
            return s
        } finally {
            scanner.close()
        }
    }

    override fun call(): ThreadTestResult? {
        log(String.format(Locale.US, "thread %d: started.", threadId))
        val testResult = ThreadTestResult()
        var s: Socket? = null
        try {
            s = connect(testResult)
            if (s == null) {
                throw Exception("error during connect to test server")
            }

            log(String.format(Locale.US, "thread %d: connected, waiting for rest...", threadId))
            barrier.await()

            // ***** short download *****
            run {
                val targetTimeEnd = System.nanoTime() + params.pretestDuration * nsecsL
                var chunks = 1
                do {
                    downloadChunks(chunks)
                    chunks *= 2
                } while (System.nanoTime() < targetTimeEnd)

                if (chunks <= 4) {
                    // connection is quite slow, we'll only use 1 thread
                    fallbackToOneThread.set(true)
                }
            }

            val fallbackToOneThreadLocal: Boolean
            setStatus(TestStatus.PING)
            // ***** ping *****
            run {
                barrier.await()

                startTrafficService(TestStatus.PING)

                fallbackToOneThreadLocal = fallbackToOneThread.get()

                if (fallbackToOneThreadLocal && threadId != 0) {
                    return null
                }

                val numPings = params.numPings
                var shortestPing = Long.MAX_VALUE
                var medianPing = Long.MAX_VALUE
                val pings = ArrayList<Long>()
                val timeStart = System.nanoTime()
                if (threadId == 0) { // only one thread pings!
                    var i = 0
                    while (i < numPings) {
                        val ping = ping()
                        if (ping != null) {
                            client.updatePingStatus(timeStart, i + 1, System.nanoTime())

                            pings.add(ping.server)
                            if (ping.client < shortestPing) {
                                shortestPing = ping.client
                            }

                            client.onPingDataChanged(ping.client, ping.server, ping.timeNs)
                            testResult.pings.add(ping)

                            val timeElapsed = (System.nanoTime() - timeStart) / 1000000
                            if (params.doPingIntervalMilliseconds > timeElapsed && i >= numPings - 1) {
                                i--
                            }
                        }
                        i++
                    }

                    // median
                    Collections.sort(pings)
                    val middle = pings.size / 2
                    medianPing = if (pings.size % 2 == 0) {
                        val medianA = pings[middle]
                        val medianB = pings[middle - 1]
                        (medianA + medianB) / 2
                    } else {
                        pings[middle + 1]
                    }
                    // display median ping
                    client.setPing(medianPing)
                }
                testResult.ping_shortest = shortestPing
                testResult.ping_median = medianPing
            }

            // ***** jitter and packet loss *****
            if (client.isEnabledJitterAndPacketLossTest && !client.taskDescList.isNullOrEmpty()) {
                if (threadId == 0) {
                    client.performVoipTest()
                }
                barrier.await()
            }

            if (doDownload) {
                val duration = params.duration

                setStatus(TestStatus.DOWN)
                // ***** download *****

                if (!fallbackToOneThreadLocal) {
                    barrier.await()
                }

                stopTrafficService(TestStatus.PING)
                startTrafficService(TestStatus.DOWN)

                curTransfer.set(0)
                curTime.set(0)

                val result = SingleResult()
                val reinitSocket = download(duration, 0, result)
                if (reinitSocket) {
                    s.close()
                    s = connect(testResult)
                    log(String.format(Locale.US, "thread %d: reconnected", threadId))
                    if (s == null) {
                        throw Exception("error during connect to test server")
                    }
                }

                testResult.down = result.getAllResults()
                result.addCoarseSpeedItems(testResult.speedItems, false, threadId)

                curTransfer.set(result.getBytes())
                curTime.set(result.getNsec())
            }

            if (doUpload) {
                val duration = params.duration

                setStatus(TestStatus.INIT_UP)
                // ***** short upload *****
                run {
                    if (!fallbackToOneThreadLocal) {
                        barrier.await()
                    }

                    stopTrafficService(TestStatus.DOWN)

                    curTransfer.set(0)
                    curTime.set(0)

                    val targetTimeEnd = System.nanoTime() + params.pretestDuration * nsecsL
                    var chunks = 1
                    do {
                        uploadChunks(chunks)
                        chunks *= 2
                    } while (System.nanoTime() < targetTimeEnd)
                }

                // ***** upload *****
                setStatus(TestStatus.UP)

                startTrafficService(TestStatus.UP)

                curTransfer.set(0)
                curTime.set(0)

                if (!fallbackToOneThreadLocal) {
                    barrier.await()
                }

                val result = SingleResult()

                upload(duration, result)

                testResult.up = result.getAllResults()
                result.addCoarseSpeedItems(testResult.speedItems, true, threadId)

                `in`?.let { totalDown += it.count }
                out?.let { totalUp += it.count }

                testResult.totalDownBytes = totalDown
                testResult.totalUpBytes = totalUp

                curTransfer.set(result.getBytes())
                curTime.set(result.getNsec())

                stopTrafficService(TestStatus.UP)
            }
        } catch (e: BrokenBarrierException) {
            client.log("interrupted (BBE)")
            Thread.currentThread().interrupt()
        } catch (e: InterruptedException) {
            client.log("interrupted")
            Thread.currentThread().interrupt()
        } catch (e: Exception) {
            client.log(e)
            client.abortTest(true)
        } finally {
            if (s != null) {
                try {
                    s.close()
                } catch (e: IOException) {
                    client.log(e)
                }
            }
        }
        return testResult
    }

    private fun downloadChunks(chunks: Int) {
        if (Thread.interrupted()) {
            throw InterruptedException()
        }

        require(chunks >= 1)

        log(String.format(Locale.US, "thread %d: getting %d chunk(s)", threadId, chunks))

        val rdr = reader!!
        val outStream = out!!
        val inStream = `in`!!
        val buffer = buf!!

        var line = rdr.readLine() ?: throw IllegalStateException("connection lost")
        if (!line.startsWith("ACCEPT ")) {
            log(String.format(Locale.US, "thread %d: got '%s' expected 'ACCEPT'", threadId, line))
            throw IllegalStateException()
        }

        var send = String.format(Locale.US, "GETCHUNKS %d\n", chunks)
        outStream.write(send.toByteArray(charset("US-ASCII")))
        outStream.flush()

        var totalRead: Long = 0
        var read: Int
        var lastByte = 0.toByte()
        do {
            if (Thread.interrupted()) {
                throw InterruptedException()
            }
            read = inStream.read(buffer)
            if (read > 0) {
                val posLast = chunksize - 1 - (totalRead % chunksize).toInt()
                if (read > posLast) {
                    lastByte = buffer[posLast]
                }
                totalRead += read.toLong()
            }
        } while (read > 0 && lastByte != 0xff.toByte())

        send = "OK\n"
        outStream.write(send.toByteArray(charset("US-ASCII")))
        outStream.flush()

        rdr.readLine() // read TIME line
    }

    private fun download(seconds: Int, additionalWait: Int, result: SingleResult): Boolean {
        if (Thread.interrupted()) {
            throw InterruptedException()
        }

        require(seconds >= 1)

        log(String.format(Locale.US, "thread %d: download test %d seconds", threadId, seconds))

        val rdr = reader!!
        val outStream = out!!
        val inStream = `in`!!
        val buffer = buf!!

        var line = rdr.readLine() ?: throw IllegalStateException("connection lost")
        if (!line.startsWith("ACCEPT ")) {
            log(String.format(Locale.US, "thread %d: got '%s' expected 'ACCEPT'", threadId, line))
            throw IllegalStateException()
        }

        val timeStart = System.nanoTime()
        val timeLatestEnd = timeStart + (seconds + additionalWait) * nsecsL

        var send = String.format(Locale.US, "GETTIME %d\n", seconds)
        outStream.write(send.toByteArray(charset("US-ASCII")))
        outStream.flush()

        var totalRead: Long = 0
        var read: Int
        var lastByte = 0.toByte()

        do {
            if (Thread.interrupted()) {
                throw InterruptedException()
            }
            read = inStream.read(buffer)
            if (read > 0) {
                val posLast = chunksize - 1 - (totalRead % chunksize).toInt()
                if (read > posLast) {
                    lastByte = buffer[posLast]
                }
                totalRead += read.toLong()

                val nsec = System.nanoTime() - timeStart

                result.addResult(totalRead, nsec)
                curTransfer.set(totalRead)
                curTime.set(nsec)
            }
        } while (read > 0 && lastByte != 0xff.toByte() && System.nanoTime() <= timeLatestEnd)

        val timeEnd = System.nanoTime()

        if (read <= 0) {
            log(String.format(Locale.US, "thread %d: error while receiving data", threadId))
            throw IllegalStateException()
        }

        val nsec = timeEnd - timeStart
        result.addResult(totalRead, nsec)
        curTransfer.set(totalRead)
        curTime.set(nsec)

        if (lastByte != 0xff.toByte()) {
            return true
        }

        send = "OK\n"
        outStream.write(send.toByteArray(charset("US-ASCII")))
        outStream.flush()

        line = rdr.readLine() ?: throw IllegalStateException("connection lost")
        val sc = Scanner(line)
        sc.findInLine("TIME (\\d+)")
        sc.close()
        return false
    }

    private fun uploadChunks(chunks: Int) {
        if (Thread.interrupted()) {
            throw InterruptedException()
        }

        require(chunks >= 1)

        log(String.format(Locale.US, "thread %d: putting %d chunk(s)", threadId, chunks))

        val rdr = reader!!
        val outStream = out!!
        val buffer = buf!!

        var line = rdr.readLine() ?: throw IllegalStateException("connection lost")
        if (!line.startsWith("ACCEPT ")) {
            log(String.format(Locale.US, "thread %d: got '%s' expected 'ACCEPT'", threadId, line))
            throw IllegalStateException()
        }

        outStream.write("PUTNORESULT\n".toByteArray(charset("US-ASCII")))
        outStream.flush()

        line = rdr.readLine() ?: throw IllegalStateException("connection lost")
        if (line != "OK") {
            throw IllegalStateException()
        }

        buffer[chunksize - 1] = 0.toByte() // set last byte to continue value

        for (i in 0 until chunks) {
            if (i == chunks - 1) {
                buffer[chunksize - 1] = 0xff.toByte() // set last byte to termination value
            }
            outStream.write(buffer, 0, chunksize)
        }

        rdr.readLine() // TIME line
    }

    private fun upload(seconds: Int, result: SingleResult): Boolean {
        if (Thread.interrupted()) {
            throw InterruptedException()
        }

        if (seconds < 1 && !params.isEncryption) {
            throw IllegalArgumentException()
        }

        log(String.format(Locale.US, "thread %d: upload test %d seconds", threadId, seconds))

        var enoughTimeTmp = (seconds - UPLOAD_MAX_DISCARD_TIME) * nsecsL
        if (enoughTimeTmp < 0) {
            enoughTimeTmp = 0
        }
        val enoughTime = enoughTimeTmp

        val rdr = reader!!
        val outStream = out!!
        val buffer = buf!!

        var line = rdr.readLine() ?: throw IllegalStateException("connection lost")
        if (!line.startsWith("ACCEPT ")) {
            log(String.format(Locale.US, "thread %d: got '%s' expected 'ACCEPT'", threadId, line))
            throw IllegalStateException()
        }

        outStream.write("PUT\n".toByteArray(charset("US-ASCII")))
        outStream.flush()

        line = rdr.readLine() ?: throw IllegalStateException("connection lost")
        if (line != "OK") {
            throw IllegalStateException()
        }

        val terminateRxIfEnough = AtomicBoolean(false)
        val terminateRxAtAllEvents = AtomicBoolean(false)

        val futureRx = RMBTClient.getCommonThreadPool().submit(object : Callable<Boolean> {
            override fun call(): Boolean {
                val patternFull = Pattern.compile("TIME (\\d+) BYTES (\\d+)")
                val patternTime = Pattern.compile("TIME (\\d+)")

                val sc = Scanner(rdr)
                try {
                    sc.useDelimiter("\n")
                    var terminate = false
                    do {
                        var next: String? = null
                        try {
                            next = sc.next(patternFull)
                        } catch (e: InputMismatchException) {
                        }

                        if (next == null) {
                            next = sc.next(patternTime)
                            if (next == null) {
                                println(sc.nextLine())
                                throw IllegalStateException()
                            }
                            return false
                        }

                        val match = sc.match()
                        if (match.groupCount() == 2) {
                            val nsec = match.group(1).toLong()
                            val bytes = match.group(2).toLong()
                            result.addResult(bytes, nsec)
                            curTransfer.set(bytes)
                            curTime.set(nsec)
                        }

                        if (terminateRxAtAllEvents.get()) {
                            terminate = true
                        }
                        if (terminateRxIfEnough.get() && curTime.get() > enoughTime) {
                            terminate = true
                        }
                    } while (!terminate)
                    return true
                } finally {
                    sc.close()
                }
            }
        })

        val maxnsecs = seconds * 1000000000L
        buffer[chunksize - 1] = 0x00.toByte() // set last byte to continue value

        val bufTx = buffer.clone()
        val terminateTx = AtomicBoolean(false)
        val futureTx = RMBTClient.getCommonThreadPool().submit(object : Callable<Void?> {
            override fun call(): Void? {
                while (true) {
                    if (Thread.interrupted()) {
                        throw InterruptedException()
                    }
                    if (terminateTx.get()) {
                        // last package
                        bufTx[chunksize - 1] = 0xff.toByte() // set last byte to termination value
                        outStream.write(bufTx, 0, chunksize)
                        // forces buffered bytes to be written out.
                        outStream.flush()
                        return null
                    } else {
                        outStream.write(bufTx, 0, chunksize)
                    }
                }
            }
        })

        var returnValue: Boolean? = null
        try {
            try {
                futureTx.get(maxnsecs, TimeUnit.NANOSECONDS)
            } catch (e: TimeoutException) {
                try {
                    terminateTx.set(true)
                    futureTx.get(250, TimeUnit.MILLISECONDS)
                } catch (e2: TimeoutException) {
                    futureTx.cancel(true)
                }
            }

            Thread.sleep(100)

            terminateRxIfEnough.set(true)

            try {
                returnValue = futureRx.get(UPLOAD_MAX_WAIT_SECS, TimeUnit.SECONDS)
            } catch (e: TimeoutException) {
                try {
                    terminateRxAtAllEvents.set(true)
                    returnValue = futureRx.get(250, TimeUnit.MILLISECONDS)
                } catch (e2: TimeoutException) {
                    futureRx.cancel(true)
                }
            }
        } catch (e: ExecutionException) {
            if (e.cause is IOException) {
                throw e.cause as IOException
            } else {
                e.printStackTrace()
            }
        }

        if (returnValue == null) {
            returnValue = true
        }
        return returnValue
    }

    private fun ping(): Ping? {
        log(String.format(Locale.US, "thread %d: ping test", threadId))

        val pingTimeNs = System.nanoTime()

        val rdr = reader!!
        val outStream = out!!

        var line = rdr.readLine()
        if (!line!!.startsWith("ACCEPT ")) {
            log(String.format(Locale.US, "thread %d: got '%s' expected 'ACCEPT'", threadId, line))
            return null
        }

        val data = "PING\n".toByteArray(charset("US-ASCII"))
        val timeStart = System.nanoTime()
        outStream.write(data)
        outStream.flush()
        line = rdr.readLine()
        val timeEnd = System.nanoTime()
        outStream.write("OK\n".toByteArray(charset("US-ASCII")))
        outStream.flush()
        if (line != "PONG") {
            return null
        }

        line = rdr.readLine()
        val sc = Scanner(line)
        sc.findInLine("TIME (\\d+)")
        sc.close()

        val diffClient = timeEnd - timeStart
        val diffServer = sc.match().group(1).toLong()

        val pingClient = diffClient / 1e6
        val pingServer = diffServer / 1e6

        log(String.format(Locale.US, "thread %d - client: %.3f ms ping", threadId, pingClient))
        log(String.format(Locale.US, "thread %d - server: %.3f ms ping", threadId, pingServer))
        return Ping(diffClient, diffServer, pingTimeNs)
    }

    private fun setStatus(status: TestStatus) {
        if (threadId == 0) {
            client.status = status
        }
    }

    private fun startTrafficService(status: TestStatus) {
        client.startTrafficService(threadId, status)
    }

    private fun stopTrafficService(status: TestStatus) {
        client.stopTrafficMeasurement(threadId, status)
    }

    companion object {
        private const val nsecsL = 1000000000L

        private const val UPLOAD_MAX_DISCARD_TIME = 1 * nsecsL
        private const val UPLOAD_MAX_WAIT_SECS = 3L
    }
}
