/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.specure.info.band

import at.specure.info.cell.CellChannelAttribution
import kotlin.math.roundToInt

/**
 * Contains information about cellular band and frequency available for current network
 */
data class CellBand(

    /**
     * Network channel band
     */
    val band: Int,

    /**
     * Network channel attribution for current network
     */
    val channelAttribution: CellChannelAttribution,

    /**
     * Channel Number
     * for [CellChannelAttribution.EARFCN] LTE technology - earfcn
     * for [CellChannelAttribution.UARFCN] WCDMA technology - uarfcn
     * for [CellChannelAttribution.ARFCN] GSM technology - arfcn
     */
    val channel: Int,

    /**
     * Band name
     */
    val name: String?,

    /**
     * Download frequency in mdB
     */
    val frequencyDL: Double,

    /**
     * Download frequency in mdB
     */
    val frequencyUL: Double
) : Band() {

    override val informalName: String
        get() = name ?: ""

    companion object {

        fun fromChannelNumber(channel: Int, channelAttribution: CellChannelAttribution): CellBand? {
            val data: CellBandData?
            val step: Double

            when (channelAttribution) {
                CellChannelAttribution.ARFCN -> {
                    data = getBandFromArfcn(channel)
                    step = 0.2
                }
                CellChannelAttribution.UARFCN -> {
                    data = getBandFromUarfcn(channel)
                    step = 0.2
                }
                CellChannelAttribution.EARFCN -> {
                    data = getBandFromEarfcn(channel)
                    step = 0.1
                }
            }

            return if (data == null) {
                null
            } else {
                CellBand(
                    band = data.band,
                    channelAttribution = channelAttribution,
                    channel = channel,
                    name = data.informalName,
                    frequencyDL = data.getFrequencyDL(step, channel),
                    frequencyUL = data.getFrequencyUL(step, channel)
                )
            }
        }

        /**
         * @param arfcn Frequency to check
         * @return GSMBand object for matched band, or NULL if invalid earfcn is passed
         */
        private fun getBandFromArfcn(arfcn: Int): CellBandData? {
            // we can't differentiate between UL and DL with umts?
            for (gsmBand in gsmBands) { // Loop through all lteBands
                if (gsmBand.containsChannel(arfcn)) { // If the band contains the earfcn then return it
                    return gsmBand
                }
            }
            return null
        }

        /**
         *
         * @param uarfcn Frequency to check
         * @return UMTSBand object for matched band, or NULL if invalid earfcn is passed
         */
        private fun getBandFromUarfcn(uarfcn: Int): CellBandData? {
            // we can't differentiate between UL and DL with umts?
            for (umtsBand in umtsBands) { // Loop through all lteBands
                if (umtsBand.containsChannel(uarfcn)) { // If the band contains the earfcn then return it
                    return umtsBand
                }
            }
            return null
        }

        /**
         * @param earfcn Frequency to check
         * @return LTEBand object for matched band, or NULL if invalid earfcn is passed
         */
        private fun getBandFromEarfcn(earfcn: Int): CellBandData? {
            if (earfcn in 1..17999) { // DL
                for (band in lteBands) { // Loop through all lteBands
                    if (band.containsDLChannel(earfcn)) { // If the band contains the earfcn then return it
                        return band
                    }
                }
            } else if (earfcn in 18000..65535) { // UL
                for (band in lteBands) { // Loop through all lteBands
                    if (band.containsULChannel(earfcn)) { // If the band contains the earfcn then return it
                        return band
                    }
                }
            }
            return null
        }
    }
}

private class CellBandData(
    val band: Int,
    val uploadFrequencyLowerBound: Double,
    val uploadFrequencyUpperBound: Double,
    val downloadFrequencyLowerBound: Double,
    val downloadFrequencyUpperBound: Double,
    val uploadChannelLowerBound: Double,
    val uploadChannelUpperBound: Double,
    val channelOffset: Double,
    val informalName: String?
) {
    fun containsChannel(channel: Int): Boolean {
        return containsDLChannel(channel) || containsULChannel(channel)
    }

    fun containsDLChannel(channel: Int): Boolean {
        return channel > uploadChannelLowerBound - channelOffset && channel < uploadChannelUpperBound - channelOffset
    }

    fun containsULChannel(channel: Int): Boolean {
        return channel > uploadChannelLowerBound && channel < uploadChannelUpperBound
    }

    fun getFrequencyDL(step: Double, channel: Int): Double {
        val channelOffset = if (!containsDLChannel(channel)) 0.0 else this.channelOffset
        var frequency = this.downloadFrequencyLowerBound + step * (channel - (this.uploadChannelLowerBound - channelOffset))
        frequency = (frequency * 1000).roundToInt().toDouble() / 1000
        return frequency
    }

    fun getFrequencyUL(step: Double, channel: Int): Double {
        val channelOffset = if (!containsULChannel(channel)) 0.0 else -this.channelOffset
        var frequency = this.downloadFrequencyLowerBound + step * (channel - (this.uploadChannelLowerBound - channelOffset))
        frequency = (frequency * 1000).roundToInt().toDouble() / 1000
        return frequency
    }
}

private fun MutableSet<CellBandData>.add(
    band: Int,
    uploadFrequencyLowerBound: Double,
    uploadFrequencyUpperBound: Double,
    downloadFrequencyLowerBound: Double,
    downloadFrequencyUpperBound: Double,
    uploadChannelLowerBound: Double,
    uploadChannelUpperBound: Double,
    channelOffset: Double,
    informalName: String?
) {
    add(
        CellBandData(
            band = band,
            uploadFrequencyLowerBound = uploadFrequencyLowerBound,
            uploadFrequencyUpperBound = uploadFrequencyUpperBound,
            downloadFrequencyLowerBound = downloadFrequencyLowerBound,
            downloadFrequencyUpperBound = downloadFrequencyUpperBound,
            uploadChannelLowerBound = uploadChannelLowerBound,
            uploadChannelUpperBound = uploadChannelUpperBound,
            channelOffset = channelOffset,
            informalName = informalName
        )
    )
}

/**
 * Static list of all LTE Bands
 * taken from the 3gpp 36.101 standard
 * http://www.3gpp.org/ftp//Specs/archive/36_series/36.101/
 * latest update e30 (2017-03-28)
 */
private val lteBands: Set<CellBandData> = mutableSetOf<CellBandData>().apply {
    add(1, 1920.0, 1980.0, 2110.0, 2170.0, 18000.0, 18599.0, 18000.0, "2100 MHz")
    add(2, 1850.0, 1910.0, 1930.0, 1990.0, 18600.0, 19199.0, 18000.0, "PCS A-F blocks 1,9 GHz")
    add(3, 1710.0, 1785.0, 1805.0, 1880.0, 19200.0, 19949.0, 18000.0, "1800 MHz")
    add(4, 1710.0, 1755.0, 2110.0, 2155.0, 19950.0, 20399.0, 18000.0, "AWS-1 1.7+2.1 GHz")
    add(5, 824.0, 849.0, 869.0, 894.0, 20400.0, 20649.0, 18000.0, "850MHz (was CDMA)")
    add(6, 830.0, 840.0, 875.0, 885.0, 20650.0, 20749.0, 18000.0, "850MHz subset (was CDMA)")
    add(7, 2500.0, 2570.0, 2620.0, 2690.0, 20750.0, 21449.0, 18000.0, "2600 MHz")
    add(8, 880.0, 915.0, 925.0, 960.0, 21450.0, 21799.0, 18000.0, "900 MHz")
    add(9, 1749.9, 1784.9, 1844.9, 1879.9, 21800.0, 22149.0, 18000.0, "DCS1800 subset")
    add(10, 1710.0, 1770.0, 2110.0, 2170.0, 22150.0, 22749.0, 18000.0, "Extended AWS/AWS-2/AWS-3")
    add(11, 1427.9, 1447.9, 1475.9, 1495.9, 22750.0, 22949.0, 18000.0, "1.5 GHz lower")
    add(12, 699.0, 716.0, 729.0, 746.0, 23010.0, 23179.0, 18000.0, "700 MHz lower A(BC) blocks")
    add(13, 777.0, 787.0, 746.0, 756.0, 23180.0, 23279.0, 18000.0, "700 MHz upper C block")
    add(14, 788.0, 798.0, 758.0, 768.0, 23280.0, 23379.0, 18000.0, "700 MHz upper D block")
    add(17, 704.0, 716.0, 734.0, 746.0, 23730.0, 23849.0, 18000.0, "700 MHz lower BC blocks")
    add(18, 815.0, 830.0, 860.0, 875.0, 23850.0, 23999.0, 18000.0, "800 MHz lower")
    add(19, 830.0, 845.0, 875.0, 890.0, 24000.0, 24149.0, 18000.0, "800 MHz upper")
    add(20, 832.0, 862.0, 791.0, 821.0, 24150.0, 24449.0, 18000.0, "800 MHz")
    add(21, 1447.9, 1462.9, 1495.9, 1510.9, 24450.0, 24599.0, 18000.0, "1.5 GHz upper")
    add(22, 3410.0, 3490.0, 3510.0, 3590.0, 24600.0, 25399.0, 18000.0, "3.5 GHz")
    add(23, 2000.0, 2020.0, 2180.0, 2200.0, 25500.0, 25699.0, 18000.0, "2 GHz S-Band")
    add(24, 1626.5, 1660.5, 1525.0, 1559.0, 25700.0, 26039.0, 18000.0, "1.6 GHz L-Band")
    add(25, 1850.0, 1915.0, 1930.0, 1995.0, 26040.0, 26689.0, 18000.0, "PCS A-G blocks 1900")
    add(26, 814.0, 849.0, 859.0, 894.0, 26690.0, 27039.0, 18000.0, "ESMR+ 850 (was: iDEN)")
    add(27, 807.0, 824.0, 852.0, 869.0, 27040.0, 27209.0, 18000.0, "800 MHz SMR (was iDEN)")
    add(28, 703.0, 748.0, 758.0, 803.0, 27210.0, 27659.0, 18000.0, "700 MHz")
    add(29, 0.0, 0.0, 717.0, 728.0, 0.0, 0.0, -9660.0, "700 lower DE blocks (suppl. DL)")
    add(30, 2305.0, 2315.0, 2350.0, 2360.0, 27660.0, 27759.0, 17890.0, "2.3GHz WCS")
    add(31, 452.5, 457.5, 462.5, 467.5, 27760.0, 27809.0, 17890.0, "IMT 450 MHz")
    add(32, 0.0, 0.0, 1452.0, 1496.0, 0.0, 0.0, -9920.0, "1.5 GHz L-Band (suppl. DL)")
    add(33, 1900.0, 1920.0, 1900.0, 1920.0, 36000.0, 36199.0, 0.0, "2 GHz TDD lower")
    add(34, 2010.0, 2025.0, 2010.0, 2025.0, 36200.0, 36349.0, 0.0, "2 GHz TDD upper")
    add(35, 1850.0, 1910.0, 1850.0, 1910.0, 36350.0, 36949.0, 0.0, "1,9 GHz TDD lower")
    add(36, 1930.0, 1990.0, 1930.0, 1990.0, 36950.0, 37549.0, 0.0, "1.9 GHz TDD upper")
    add(37, 1910.0, 1930.0, 1910.0, 1930.0, 37550.0, 37749.0, 0.0, "PCS TDD")
    add(38, 2570.0, 2620.0, 2570.0, 2620.0, 37750.0, 38249.0, 0.0, "2600 MHz TDD")
    add(39, 1880.0, 1920.0, 1880.0, 1920.0, 38250.0, 38649.0, 0.0, "IMT 1.9 GHz TDD (was TD-SCDMA)")
    add(40, 2300.0, 2400.0, 2300.0, 2400.0, 38650.0, 39649.0, 0.0, "2300 MHz")
    add(41, 2496.0, 2690.0, 2496.0, 2690.0, 39650.0, 41589.0, 0.0, "Expanded TDD 2.6 GHz")
    add(42, 3400.0, 3600.0, 3400.0, 3600.0, 41590.0, 43589.0, 0.0, "3,4-3,6 GHz")
    add(43, 3600.0, 3800.0, 3600.0, 3800.0, 43590.0, 45589.0, 0.0, "3.6-3,8 GHz")
    add(44, 703.0, 803.0, 703.0, 803.0, 45590.0, 46589.0, 0.0, "700 MHz APT TDD")
    add(45, 1447.0, 1467.0, 1447.0, 1467.0, 46590.0, 46789.0, 0.0, "1500 MHZ")
    add(46, 5150.0, 5925.0, 5150.0, 5925.0, 46790.0, 54539.0, 0.0, "TD Unlicensed")
    add(47, 5855.0, 5925.0, 5855.0, 5925.0, 54540.0, 55239.0, 0.0, "Vehicle to Everything (V2X) TDD")
    add(65, 1920.0, 2010.0, 2110.0, 2200.0, 131072.0, 131971.0, 65536.0, "Extended IMT 2100")
    add(66, 1710.0, 1780.0, 2110.0, 2200.0, 131972.0, 132671.0, 65536.0, "AWS-3")
    add(67, 0.0, 0.0, 738.0, 758.0, 0.0, 0.0, -67336.0, "700 EU (Suppl. DL)")
    add(68, 698.0, 728.0, 753.0, 783.0, 132672.0, 132971.0, 65136.0, "700 ME")
    add(69, 0.0, 0.0, 2570.0, 2620.0, 0.0, 0.0, -67836.0, "IMT-E FDD CA")
    add(70, 1695.0, 1710.0, 1995.0, 2020.0, 132972.0, 133121.0, 64636.0, "AWS-4")
    add(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, null)
}

/**
 * Static list of all UMTS Bands
 * taken from the 3GPP TS 25.101 standard
 * ftp://ftp.3gpp.org/Specs/latest/Rel-14/25_series/25101-e00.zip
 * latest update 2016-06
 *
 * order by priority (if one uarfcn is contained in multiple bands)
 */
private val umtsBands: Set<CellBandData> = mutableSetOf<CellBandData>().apply {
    add(1, 1922.4, 1977.6, 2112.4, 2167.6, 9612.0, 9888.0, -950.0, "2100 MHz")
    add(2, 1852.4, 1907.6, 1932.4, 1987.6, 9262.0, 9538.0, -400.0, "1900 MHz PCS")
    add(3, 1712.4, 1782.6, 1807.4, 1877.6, 937.0, 1288.0, -225.0, "1800 MHz DCS")
    add(4, 1712.4, 1752.6, 2112.4, 2152.6, 1312.0, 1513.0, -225.0, "AWS-1")
    add(5, 826.4, 846.6, 871.4, 891.6, 4132.0, 4233.0, -225.0, "850 MHz")
    add(6, 832.4, 837.6, 877.4, 882.6, 4162.0, 4188.0, -225.0, "850 MHz Japan")
    add(7, 2502.4, 2567.6, 2622.4, 2687.6, 2012.0, 2338.0, -225.0, "2600 MHz")
    add(8, 882.4, 912.6, 927.4, 957.6, 2712.0, 2863.0, -225.0, "900 MHz")
    add(9, 1752.4, 1782.4, 1847.4, 1877.4, 8762.0, 8912.0, -475.0, "1800 MHz Japan")
    add(10, 1712.4, 1767.6, 2112.4, 2167.6, 2887.0, 3163.0, -225.0, "AWS-1+")
    add(11, 1430.4, 1445.4, 1478.4, 1493.4, 3487.0, 3562.0, -225.0, "1500 MHz Lower")
    add(12, 701.4, 713.6, 731.4, 743.6, 3617.0, 3678.0, -225.0, "700 MHz US a")
    add(13, 779.4, 784.6, 748.4, 753.6, 3792.0, 3818.0, -225.0, "700 MHz US c")
    add(14, 790.4, 795.6, 760.4, 765.6, 3892.0, 3918.0, -225.0, "700 MHz US PS")
    add(19, 832.4, 842.6, 877.4, 887.6, 312.0, 363.0, -400.0, "800 MHz Japan")
    add(20, 834.4, 859.6, 793.4, 818.6, 4287.0, 4413.0, -225.0, "800 MHz EU DD")
    add(21, 1450.4, 1460.4, 1498.4, 1508.4, 462.0, 512.0, -400.0, "1500 MHz Upper")
    add(22, 3412.4, 3487.6, 3512.4, 3587.6, 4437.0, 4813.0, -225.0, "3500 MHz")
    add(25, 1852.4, 1912.6, 1932.4, 1992.6, 4887.0, 5188.0, -225.0, "1900+ MHz")
    add(26, 816.4, 846.6, 861.4, 891.6, 5537.0, 5688.0, -225.0, "850+ MHz")
}

/**
 * Static list of all GSM Bands
 * taken from the 3GPP TS 45.005 standard
 * http://www.3gpp.org/ftp/Specs/archive/45_series/45.005/45005-e10.zip
 * latest update 2017-06
 *
 * order by priority (if one uarfcn is contained in multiple bands)
 */
private val gsmBands: Set<CellBandData> = mutableSetOf<CellBandData>().apply {
    add(5, 824.2, 848.8, 869.2, 893.8, 128.0, 251.0, 0.0, "GSM 850")
    add(8, 890.2, 914.8, 935.2, 959.8, 1.0, 124.0, 0.0, "GSM 900")
    add(8, 890.0, 914.8, 935.0, 959.8, 0.0, 124.0, 0.0, "GSM 900")
    add(8, 880.2, 889.8, 925.2, 934.8, 975.0, 1023.0, 0.0, "GSM 900")
    add(8, 890.0, 914.8, 935.0, 959.8, 0.0, 124.0, 0.0, "GSM 900")
    add(8, 876.2, 889.8, 921.2, 934.8, 955.0, 1023.0, 0.0, "GSM 900")
    add(3, 1710.2, 1784.8, 1805.2, 1879.8, 512.0, 885.0, 0.0, "GSM 1800")
    add(2, 1850.2, 1909.8, 1930.2, 1989.8, 512.0, 810.0, 0.0, "GSM 1900")
}