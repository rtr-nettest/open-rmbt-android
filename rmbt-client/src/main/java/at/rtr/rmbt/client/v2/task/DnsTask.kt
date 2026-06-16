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
import at.rtr.rmbt.client.helper.Dig
import at.rtr.rmbt.client.v2.task.result.QoSTestResult
import at.rtr.rmbt.client.v2.task.result.QoSTestResultEnum
import org.json.JSONObject
import org.xbill.DNS.A6Record
import org.xbill.DNS.AAAARecord
import org.xbill.DNS.ARecord
import org.xbill.DNS.CNAMERecord
import org.xbill.DNS.MXRecord
import org.xbill.DNS.Rcode
import org.xbill.DNS.ResolverConfig
import org.xbill.DNS.Section
import java.net.SocketTimeoutException
import java.util.ArrayList

class DnsTask(nnTest: QualityOfServiceTest, taskDesc: TaskDesc, threadId: Int) :
    AbstractQoSTask(nnTest, taskDesc, threadId, threadId) {

    private val record: String?
    private val host: String?
    private val resolver: String?
    private val timeout: Long

    init {
        this.record = taskDesc.getParams()[PARAM_DNS_RECORD] as String?
        this.host = taskDesc.getParams()[PARAM_DNS_HOST] as String?
        this.resolver = taskDesc.getParams()[PARAM_DNS_RESOLVER] as String?

        val value = taskDesc.getParams()[PARAM_DNS_TIMEOUT] as String?
        this.timeout = if (value != null) value.toLong() else DEFAULT_TIMEOUT
    }

    @Throws(Exception::class)
    override fun call(): QoSTestResult {
        val testResult = initQoSTestResult(QoSTestResultEnum.DNS)

        try {
            onStart(testResult)

            val start = System.nanoTime()
            val dnsResult = lookupDns(host, record, resolver, (timeout / 1000000).toInt(), testResult)
            testResult.resultMap[RESULT_ENTRY] = dnsResult
            val duration = System.nanoTime() - start
            testResult.resultMap[RESULT_DURATION] = duration
            testResult.resultMap[RESULT_RESOLVER] = resolver ?: "Standard"
            testResult.resultMap[RESULT_DNS_RECORD] = record
            testResult.resultMap[RESULT_DNS_HOST] = host
            testResult.resultMap[RESULT_DNS_TIMEOUT] = timeout
            if (dnsResult == null || dnsResult.size <= 0) {
                testResult.resultMap[RESULT_DNS_ENTRIES_FOUND] = "0"
            } else {
                testResult.resultMap[RESULT_DNS_ENTRIES_FOUND] = dnsResult.size
            }
        } catch (e: Exception) {
            throw e
        } finally {
            onEnd(testResult)
        }

        return testResult
    }

    override fun initTask() {
    }

    override fun getTestType(): QoSTestResultEnum = QoSTestResultEnum.DNS

    override fun needsQoSControlConnection(): Boolean = false

    companion object {
        const val DEFAULT_TIMEOUT = 5000000000L

        const val PARAM_DNS_HOST = "host"

        const val PARAM_DNS_RESOLVER = "resolver"

        const val PARAM_DNS_RECORD = "record"

        const val PARAM_DNS_TIMEOUT = "timeout"

        const val RESULT_STATUS = "dns_result_status"

        const val RESULT_ENTRY = "dns_result_entries"

        const val RESULT_TTL = "dns_result_ttl"

        const val RESULT_ADDRESS = "dns_result_address"

        const val RESULT_PRIORITY = "dns_result_priority"

        const val RESULT_DURATION = "dns_result_duration"

        const val RESULT_QUERY = "dns_result_info"

        const val RESULT_RESOLVER = "dns_objective_resolver"

        const val RESULT_DNS_HOST = "dns_objective_host"

        const val RESULT_DNS_RECORD = "dns_objective_dns_record"

        const val RESULT_DNS_TIMEOUT = "dns_objective_timeout"

        const val RESULT_DNS_ENTRIES_FOUND = "dns_result_entries_found"

        @JvmStatic
        fun lookupDns(domainName: String?, record: String?, resolver: String?, timeout: Int, testResult: QoSTestResult): List<JSONObject>? {
            val result: MutableList<JSONObject> = ArrayList()

            try {
                println("dns lookup: record = $record for host: $domainName, using resolver:$resolver")

                ResolverConfig.refresh() // refresh dns server

                val req = Dig.doRequest(domainName, record, resolver, timeout)

                testResult.resultMap[RESULT_QUERY] = "OK"
                testResult.resultMap[RESULT_STATUS] = Rcode.string(req.response.rcode)
                if (req.request.rcode == Rcode.NOERROR) {
                    val records = req.response.getSectionArray(Section.ANSWER)

                    if (records != null && records.isNotEmpty()) {
                        for (i in records.indices) {
                            val dnsEntry = JSONObject()
                            val r = records[i]
                            when (r) {
                                is MXRecord -> {
                                    dnsEntry.put(RESULT_PRIORITY, r.priority.toString())
                                    dnsEntry.put(RESULT_ADDRESS, r.target.toString())
                                }
                                is CNAMERecord -> dnsEntry.put(RESULT_ADDRESS, r.alias)
                                is ARecord -> dnsEntry.put(RESULT_ADDRESS, r.address.hostAddress)
                                is AAAARecord -> dnsEntry.put(RESULT_ADDRESS, r.address.hostAddress)
                                is A6Record -> dnsEntry.put(RESULT_ADDRESS, r.suffix.toString())
                                else -> dnsEntry.put(RESULT_ADDRESS, r.name)
                            }

                            dnsEntry.put(RESULT_TTL, r.ttl.toString())

                            result.add(dnsEntry)
                            println("record $i toString: $r")
                        }
                    } else {
                        return null
                    }
                } else {
                    return null
                }
            } catch (e: SocketTimeoutException) {
                testResult.resultMap[RESULT_QUERY] = "TIMEOUT"
                e.printStackTrace()
                return null
            } catch (e: Exception) {
                testResult.resultMap[RESULT_QUERY] = "ERROR"
                e.printStackTrace()
                return null
            }

            return result
        }
    }
}
