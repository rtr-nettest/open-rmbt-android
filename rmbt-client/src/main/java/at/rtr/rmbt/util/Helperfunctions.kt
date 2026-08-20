package at.rtr.rmbt.util;

import com.google.common.net.InetAddresses;

import java.net.InetAddress;

public class Helperfunctions {
    /**
     * Anonymize an IP address
     * @param inetAddress the IP address to be anonymized
     * @param replaceLastOctetWith the String which shall replace the last octet in IPv4
     * @return
     */
    public static String anonymizeIp(final InetAddress inetAddress, String replaceLastOctetWith) {
        try
        {
            final byte[] address = inetAddress.getAddress();
            address[address.length - 1] = 0;
            if (address.length > 4) // ipv6
            {
                for (int i = 6; i < address.length; i++)
                    address[i] = 0;
            }

            String result = InetAddresses.toAddrString(InetAddress.getByAddress(address));
            if (address.length == 4)
                result = result.replaceFirst(".0$", replaceLastOctetWith);
            return result;
        }
        catch (final Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
