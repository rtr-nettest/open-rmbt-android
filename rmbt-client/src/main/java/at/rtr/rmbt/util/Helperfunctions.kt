package at.rtr.rmbt.util

import com.google.common.net.InetAddresses
import java.net.InetAddress

object Helperfunctions {
    /**
     * Anonymize an IP address
     * @param inetAddress the IP address to be anonymized
     * @param replaceLastOctetWith the String which shall replace the last octet in IPv4
     */
    fun anonymizeIp(inetAddress: InetAddress, replaceLastOctetWith: String): String? {
        return try {
            val address = inetAddress.address
            address[address.size - 1] = 0
            if (address.size > 4) { // ipv6
                for (i in 6 until address.size) address[i] = 0
            }

            var result = InetAddresses.toAddrString(InetAddress.getByAddress(address))
            if (address.size == 4) {
                result = result.replaceFirst(Regex(".0$"), replaceLastOctetWith)
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
