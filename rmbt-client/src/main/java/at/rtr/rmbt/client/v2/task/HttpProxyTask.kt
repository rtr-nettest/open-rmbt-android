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
package at.rtr.rmbt.client.v2.task

import at.rtr.rmbt.client.QualityOfServiceTest
import at.rtr.rmbt.client.RMBTClient
import at.rtr.rmbt.client.v2.task.result.QoSTestResult
import at.rtr.rmbt.client.v2.task.result.QoSTestResultEnum
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author lb
 */
class HttpProxyTask(nnTest: QualityOfServiceTest, taskDesc: TaskDesc, threadId: Int) :
    AbstractQoSTask(nnTest, taskDesc, threadId, threadId) {

    private val target: String?
    private val range: String?
    private val connectionTimeout: Long
    private val downloadTimeout: Long

    val downloadCompleted = AtomicBoolean(false)

    val timeOutReached = AtomicBoolean(false)

    init {
        this.target = taskDesc.getParams()[PARAM_TARGET] as String?
        this.range = taskDesc.getParams()[PARAM_RANGE] as String?

        var value = taskDesc.getParams()[PARAM_CONNECTION_TIMEOUT] as String?
        this.connectionTimeout = if (value != null) value.toLong() else DEFAULT_CONNECTION_TIMEOUT

        value = taskDesc.getParams()[PARAM_DOWNLOAD_TIMEOUT] as String?
        this.downloadTimeout = if (value != null) value.toLong() else DEFAULT_DOWNLOAD_TIMEOUT
    }

    class Md5Result {
        var md5: String? = null
        var contentLength: Long = 0
        var generatingTimeNs: Long = 0
    }

    override fun call(): QoSTestResult {
        val result = initQoSTestResult(QoSTestResultEnum.HTTP_PROXY)
        try {
            result.resultMap[RESULT_RANGE] = range
            result.resultMap[RESULT_TARGET] = target

            onStart(result)

            val httpTimeoutTask = RMBTClient.getCommonThreadPool().submit(object : Callable<QoSTestResult> {
                override fun call(): QoSTestResult {
                    httpGet(result)
                    return result
                }
            })

            return httpTimeoutTask.get(downloadTimeout, TimeUnit.NANOSECONDS)
        } catch (e: TimeoutException) {
            e.printStackTrace()
            result.resultMap[RESULT_HASH] = "TIMEOUT"
        } catch (e: Exception) {
            throw e
        } finally {
            onEnd(result)
        }

        return result
    }

    private fun httpGet(result: QoSTestResult): QoSTestResult {
        val url = URL(this.target)
        val connection: HttpURLConnection
        var hash: String? = null

        try {
            val timeoutThread = Thread(Runnable {
                try {
                    println("HTTP PROXY TIMEOUT THREAD: $downloadTimeout ms")
                    Thread.sleep((downloadTimeout / 1000000).toInt().toLong())

                    if (!downloadCompleted.get()) {
                        timeOutReached.set(true)
                        println("HTTP PROXY TIMEOUT REACHED")
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            })

            timeoutThread.start()

            val start = System.nanoTime()
            connection = url.openConnection() as HttpURLConnection
            if (range != null && range.startsWith("bytes")) {
                connection.addRequestProperty("Range", range)
            }

            connection.connectTimeout = TimeUnit.MILLISECONDS.convert(connectionTimeout, TimeUnit.NANOSECONDS).toInt()
            connection.readTimeout = TimeUnit.MILLISECONDS.convert(downloadTimeout, TimeUnit.NANOSECONDS).toInt()
            connection.instanceFollowRedirects = false

            val md5 = generateChecksum(connection.inputStream)
            downloadCompleted.set(true)
            hash = md5.md5

            val duration = System.nanoTime() - start
            result.resultMap[RESULT_DURATION] = duration - md5.generatingTimeNs
            result.resultMap[RESULT_STATUS] = connection.responseCode
            result.resultMap[RESULT_LENGTH] = md5.contentLength

            val headers: String?
            if (connection.headerFields != null) {
                val sb = StringBuilder()
                for (e in connection.headerFields.entries) {
                    if (e.key != null && e.key != "null") {
                        sb.append(e.key)
                        sb.append(": ")
                        val values = e.value
                        for (i in values.indices) {
                            sb.append(values[i])
                            if (i + 1 < values.size) {
                                sb.append(",")
                            }
                        }
                        sb.append("\n")
                    }
                }
                headers = sb.toString()
            } else {
                headers = null
            }

            result.resultMap[RESULT_HEADER] = headers
        } catch (e: Exception) {
            e.printStackTrace()
            result.resultMap[RESULT_STATUS] = ""
            result.resultMap[RESULT_LENGTH] = 0
            result.resultMap[RESULT_HEADER] = ""
        } finally {
            if (timeOutReached.get()) {
                result.resultMap[RESULT_HASH] = "TIMEOUT"
            } else if (hash != null) {
                result.resultMap[RESULT_HASH] = hash
            } else {
                result.resultMap[RESULT_HASH] = "ERROR"
            }
        }

        return result
    }

    fun writeFileFromInputStream(input: InputStream, outputFile: File): Long {
        return copyInputStreamToOutputStream(input, FileOutputStream(outputFile))
    }

    fun copyInputStreamToOutputStream(input: InputStream, output: OutputStream): Long {
        val buffer = ByteArray(4096)
        var count = 0L
        var n: Int
        while (-1 != input.read(buffer).also { n = it }) {
            if (timeOutReached.get()) {
                break
            }
            output.write(buffer, 0, n)
            count += n.toLong()
        }

        downloadCompleted.set(true)
        output.close()
        return count
    }

    override fun initTask() {
    }

    override fun getTestType(): QoSTestResultEnum = QoSTestResultEnum.HTTP_PROXY

    override fun needsQoSControlConnection(): Boolean = false

    companion object {
        const val DEFAULT_CONNECTION_TIMEOUT = 5000000000L

        const val DEFAULT_DOWNLOAD_TIMEOUT = 10000000000L

        const val PARAM_TARGET = "url"

        const val PARAM_RANGE = "range"

        const val PARAM_CONNECTION_TIMEOUT = "conn_timeout"

        const val PARAM_DOWNLOAD_TIMEOUT = "download_timeout"

        const val RESULT_STATUS = "http_result_status"

        const val RESULT_DURATION = "http_result_duration"

        const val RESULT_LENGTH = "http_result_length"

        const val RESULT_HEADER = "http_result_header"

        const val RESULT_RANGE = "http_objective_range"

        const val RESULT_TARGET = "http_objective_url"

        const val RESULT_HASH = "http_result_hash"

        fun getStringFromInputStream(input: InputStream): String {
            var br: BufferedReader? = null
            val sb = StringBuilder()

            var line: String?
            try {
                br = BufferedReader(InputStreamReader(input))
                while (br.readLine().also { line = it } != null) {
                    sb.append(line)
                    sb.append("\n")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                if (br != null) {
                    try {
                        br.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }

            return sb.toString()
        }

        fun generateChecksum(input: ByteArray): String {
            val md = MessageDigest.getInstance("MD5")
            val hash = md.digest(input)

            return generateChecksumFromDigest(hash)
        }

        fun generateChecksum(file: File): Md5Result {
            return generateChecksum(FileInputStream(file))
        }

        fun generateChecksum(inputStream: InputStream): Md5Result {
            val md5 = Md5Result()
            val md = MessageDigest.getInstance("MD5")
            val dis = DigestInputStream(inputStream, md)

            val dataBytes = ByteArray(4096)

            var nread: Int
            while (dis.read(dataBytes).also { nread = it } != -1) {
                md5.contentLength += nread.toLong()
            }

            dis.close()

            val startNs = System.nanoTime()
            md5.md5 = generateChecksumFromDigest(md.digest())
            md5.generatingTimeNs = System.nanoTime() - startNs

            return md5
        }

        fun generateChecksumFromDigest(digest: ByteArray): String {
            val hexString = StringBuilder()

            for (i in digest.indices) {
                if ((0xff and digest[i].toInt()) < 0x10) {
                    hexString.append("0" + Integer.toHexString(0xFF and digest[i].toInt()))
                } else {
                    hexString.append(Integer.toHexString(0xFF and digest[i].toInt()))
                }
            }
            return hexString.toString()
        }
    }
}
