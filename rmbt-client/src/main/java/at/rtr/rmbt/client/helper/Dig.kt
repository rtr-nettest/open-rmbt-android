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
package at.rtr.rmbt.client.helper

import org.xbill.DNS.DClass
import org.xbill.DNS.ExtendedFlags
import org.xbill.DNS.Message
import org.xbill.DNS.Name
import org.xbill.DNS.Rcode
import org.xbill.DNS.Record
import org.xbill.DNS.Resolver
import org.xbill.DNS.ReverseMap
import org.xbill.DNS.Section
import org.xbill.DNS.SimpleResolver
import org.xbill.DNS.TSIG
import org.xbill.DNS.Type
import java.io.IOException
import java.net.InetAddress

object Dig {
    private var name: Name? = null
    private var type = Type.A
    private var dclass = DClass.IN

    private fun usage() {
        println("Usage: dig [@server] name [<type>] [<class>] [options]")
        System.exit(0)
    }

    private fun doQuery(response: Message, ms: Long) {
        println("; java dig 0.0")
        println(response)
        println(";; Query time: $ms ms")
    }

    private fun doAXFR(response: Message) {
        println("; java dig 0.0 <> $name axfr")
        if (response.isSigned) {
            print(";; TSIG ")
            if (response.isVerified) {
                println("ok")
            } else {
                println("failed")
            }
        }

        if (response.rcode != Rcode.NOERROR) {
            println(response)
            return
        }

        val records = response.getSectionArray(Section.ANSWER)
        for (i in records.indices) {
            println(records[i])
        }

        print(";; done (")
        print(response.header.getCount(Section.ANSWER))
        print(" records, ")
        print(response.header.getCount(Section.ADDITIONAL))
        println(" additional)")
    }

    fun run(argv: Array<String>) {
        var server: String? = null
        var arg: Int
        val query: Message
        val response: Message
        val rec: Record
        var res: SimpleResolver? = null
        var printQuery = false
        val startTime: Long
        val endTime: Long

        if (argv.isEmpty()) {
            usage()
        }

        try {
            arg = 0
            if (argv[arg].startsWith("@")) {
                server = argv[arg++].substring(1)
            }

            res = if (server != null) SimpleResolver(server) else SimpleResolver()

            val nameString = argv[arg++]
            if (nameString == "-x") {
                name = ReverseMap.fromAddress(argv[arg++])
                type = Type.PTR
                dclass = DClass.IN
            } else {
                name = Name.fromString(nameString, Name.root)
                type = Type.value(argv[arg])
                if (type < 0) {
                    type = Type.A
                } else {
                    arg++
                }

                dclass = DClass.value(argv[arg])
                if (dclass < 0) {
                    dclass = DClass.IN
                } else {
                    arg++
                }
            }

            while (argv[arg].startsWith("-") && argv[arg].length > 1) {
                when (argv[arg][1]) {
                    'p' -> {
                        val portStr = if (argv[arg].length > 2) argv[arg].substring(2) else argv[++arg]
                        val port = portStr.toInt()
                        if (port < 0 || port > 65536) {
                            println("Invalid port")
                            return
                        }
                        res.setPort(port)
                    }
                    'b' -> {
                        val addrStr = if (argv[arg].length > 2) argv[arg].substring(2) else argv[++arg]
                        val addr: InetAddress
                        try {
                            addr = InetAddress.getByName(addrStr)
                        } catch (e: Exception) {
                            println("Invalid address")
                            return
                        }
                        res.setLocalAddress(addr)
                    }
                    'k' -> {
                        val key = if (argv[arg].length > 2) argv[arg].substring(2) else argv[++arg]
                        res.setTSIGKey(TSIG.fromString(key))
                    }
                    't' -> res.setTCP(true)
                    'i' -> res.setIgnoreTruncation(true)
                    'e' -> {
                        val ednsStr = if (argv[arg].length > 2) argv[arg].substring(2) else argv[++arg]
                        val edns = ednsStr.toInt()
                        if (edns < 0 || edns > 1) {
                            println("Unsupported EDNS level: $edns")
                            return
                        }
                        res.setEDNS(edns)
                    }
                    'd' -> res.setEDNS(0, 0, ExtendedFlags.DO)
                    'q' -> printQuery = true
                    else -> {
                        print("Invalid option: ")
                        println(argv[arg])
                    }
                }
                arg++
            }
        } catch (e: ArrayIndexOutOfBoundsException) {
            if (name == null) {
                usage()
            }
        }
        if (res == null) {
            res = SimpleResolver()
        }

        rec = Record.newRecord(name, type, dclass)
        query = Message.newQuery(rec)
        if (printQuery) {
            println(query)
        }
        startTime = System.currentTimeMillis()
        response = res.send(query)
        endTime = System.currentTimeMillis()

        if (type == Type.AXFR) {
            doAXFR(response)
        } else {
            doQuery(response, endTime - startTime)
        }
    }

    fun doRequest(domain: String?, record: String?, timeout: Int): DnsRequest {
        return doRequest(domain, record, null, timeout)
    }

    fun doRequest(domain: String?, record: String?, resolver: String?, timeout: Int): DnsRequest {
        val res: Resolver = if (resolver != null) SimpleResolver(resolver) else SimpleResolver()

        val rec = Record.newRecord(Name.fromString(domain, Name.root), Type.value(record), DClass.IN)
        val query = Message.newQuery(rec)

        val startTime = System.currentTimeMillis()
        res.setTimeout(0, timeout)
        val response = res.send(query)
        val endTime = System.currentTimeMillis()

        return DnsRequest(response, query, res, endTime - startTime)
    }

    class DnsRequest(
        val response: Message,
        val request: Message,
        val resolver: Resolver,
        val duration: Long
    )
}
