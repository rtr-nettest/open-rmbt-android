/*******************************************************************************
 * Copyright 2017 Rundfunk und Telekom Regulierungs-GmbH (RTR-GmbH)
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
package at.rtr.rmbt.util

object BandCalculationUtil {

    /**
     * @param earfcn Frequency to check
     * @return LTEBand object for matched band, or NULL if invalid earfcn is passed
     */
    @JvmStatic
    fun getBandFromEarfcn(earfcn: Int): FrequencyInformation<LTEBand>? {
        if (earfcn in 1..17999) { // DL
            for (band in lteBands.values) {
                if (band.containsDLChannel(earfcn.toDouble())) {
                    return FrequencyInformation(earfcn, band)
                }
            }
        } else if (earfcn in 18000..65535) { // UL
            for (band in lteBands.values) {
                if (band.containsULChannel(earfcn.toDouble())) {
                    return FrequencyInformation(earfcn, band)
                }
            }
        }

        // Invalid input
        return null
    }

    @JvmStatic
    fun getBandFromNrarfcn(nrarfcn: Int): FrequencyInformation<NRBand>? {
        // different calculation - get frequency from nrarfcn directly, then assign band
        val frequencyMHz = getNrFrequencyFromNrArfcn(nrarfcn)
        if (frequencyMHz == 0.0) {
            return null
        }

        // check if a band contains this frequency
        for (band in nrBands.values) {
            if (band.containsULFrequency(frequencyMHz)) {
                return FrequencyInformation(nrarfcn, band)
            }
        }
        return null
    }

    /**
     * @param uarfcn Frequency to check
     * @return UMTSBand object for matched band, or NULL if invalid uarfcn is passed
     */
    @JvmStatic
    fun getBandFromUarfcn(uarfcn: Int): FrequencyInformation<UMTSBand>? {
        // we can't differentiate between UL and DL with umts?
        for (umtsBand in umtsBands.values) {
            if (umtsBand.containsChannel(uarfcn.toDouble())) {
                return FrequencyInformation(uarfcn, umtsBand)
            }
        }

        // Invalid input
        return null
    }

    /**
     * @param arfcn Frequency to check
     * @return GSMBand object for matched band, or NULL if invalid arfcn is passed
     */
    @JvmStatic
    fun getBandFromArfcn(arfcn: Int): FrequencyInformation<GSMBand>? {
        // we can't differentiate between UL and DL with umts?
        for (gsmBand in gsmBands) {
            if (gsmBand.containsChannel(arfcn.toDouble())) {
                return FrequencyInformation(arfcn, gsmBand)
            }
        }

        // Invalid input
        return null
    }

    @JvmStatic
    fun getBandFromWifiFrequency(frequency: Int): WifiBand? {
        return wifiBands[frequency]
    }

    private fun getNrFrequencyFromNrArfcn(nrarfcn: Int): Double {
        val frequencyOffset: Double
        val deltaFrequencyKHz: Double
        val nRefOffs: Double
        if (nrarfcn in 0 until 600000) {
            frequencyOffset = 0.0
            deltaFrequencyKHz = 5.0
            nRefOffs = 0.0
        } else if (nrarfcn in 600000 until 2016667) {
            frequencyOffset = 3000.0
            deltaFrequencyKHz = 15.0
            nRefOffs = 600000.0
        } else if (nrarfcn in 2016667 until 3279165) {
            frequencyOffset = 24250.08
            deltaFrequencyKHz = 60.0
            nRefOffs = 2016667.0
        } else {
            // invalid input
            return 0.0
        }

        // FREF = FREF-Offs + ΔFGlobal (NREF – NREF-Offs)
        return frequencyOffset + (deltaFrequencyKHz / 1000f) * (nrarfcn - nRefOffs)
    }

    class FrequencyInformation<U : Band>(private val earfcn: Int, private val bandValue: U) {

        val frequencyDL: Double
            get() = bandValue.getFrequencyDL(earfcn.toDouble())

        fun getBand(): Int = bandValue.getBand()

        fun getInformalName(): String? = bandValue.getInformalName()

        override fun toString(): String = "Earfcn: $earfcn, Band: $bandValue"
    }

    abstract class Band protected constructor(
        private val bandNumber: Int,
        private val uploadFrequencyLowerBound: Double,
        private val uploadFrequencyUpperBound: Double,
        private val downloadFrequencyLowerBound: Double,
        private val downloadFrequencyUpperBound: Double,
        private val uploadChannelLowerBound: Double,
        private val uploadChannelUpperBound: Double,
        private val channelOffset: Double, // difference between (upload_channel_lower_bound - download_channel_lower_bound)
        private val informalName: String?
    ) {

        abstract fun getStep(): Double

        /**
         * Checks whether an upload frequency is contained in band object
         */
        fun containsChannel(channel: Double): Boolean {
            return containsDLChannel(channel) || containsULChannel(channel)
        }

        fun containsDLChannel(channel: Double): Boolean {
            return channel > (uploadChannelLowerBound - channelOffset) && channel <= (uploadChannelUpperBound - channelOffset)
        }

        fun containsULChannel(channel: Double): Boolean {
            return channel > uploadChannelLowerBound && channel <= uploadChannelUpperBound
        }

        fun containsULFrequency(frequencyMHz: Double): Boolean {
            return frequencyMHz >= uploadFrequencyLowerBound && frequencyMHz <= uploadFrequencyUpperBound
        }

        fun containsDLFrequency(frequencyMHz: Double): Boolean {
            return frequencyMHz >= downloadFrequencyLowerBound && frequencyMHz <= downloadFrequencyUpperBound
        }

        open fun getFrequencyDL(channel: Double): Double {
            val channelOffsetLocal = if (!containsDLChannel(channel)) 0.0 else this.channelOffset
            var frequency = this.downloadFrequencyLowerBound + getStep() * (channel - (this.uploadChannelLowerBound - channelOffsetLocal))
            frequency = Math.round(frequency * 1000).toDouble() / 1000
            return frequency
        }

        fun getFrequencyUL(channel: Double): Double {
            val channelOffsetLocal = if (!containsULChannel(channel)) 0.0 else -this.channelOffset
            var frequency = this.downloadFrequencyLowerBound + getStep() * (channel - (this.uploadChannelLowerBound - channelOffsetLocal))
            frequency = Math.round(frequency * 1000).toDouble() / 1000
            return frequency
        }

        fun getBand(): Int = bandNumber

        fun getInformalName(): String? = informalName
    }

    class NRBand(
        band: Int,
        uploadFrequencyLowerBound: Double,
        uploadFrequencyUpperBound: Double,
        downloadFrequencyLowerBound: Double,
        downloadFrequencyUpperBound: Double,
        informalName: String?
    ) : Band(
        band, uploadFrequencyLowerBound, uploadFrequencyUpperBound, downloadFrequencyLowerBound,
        downloadFrequencyUpperBound, 0.0, 0.0, 0.0, informalName
    ) {
        override fun getStep(): Double = 0.0 // not applicable for NR

        override fun getFrequencyDL(channel: Double): Double = getNrFrequencyFromNrArfcn(channel.toInt())
    }

    class LTEBand(
        band: Int,
        uploadFrequencyLowerBound: Double,
        uploadFrequencyUpperBound: Double,
        downloadFrequencyLowerBound: Double,
        downloadFrequencyUpperBound: Double,
        uploadChannelLowerBound: Double,
        uploadChannelUpperBound: Double,
        channelOffset: Double,
        informalName: String?
    ) : Band(
        band, uploadFrequencyLowerBound, uploadFrequencyUpperBound, downloadFrequencyLowerBound,
        downloadFrequencyUpperBound, uploadChannelLowerBound, uploadChannelUpperBound, channelOffset, informalName
    ) {
        override fun getStep(): Double = 0.1
    }

    class UMTSBand(
        band: Int,
        uploadFrequencyLowerBound: Double,
        uploadFrequencyUpperBound: Double,
        downloadFrequencyLowerBound: Double,
        downloadFrequencyUpperBound: Double,
        uploadChannelLowerBound: Double,
        uploadChannelUpperBound: Double,
        channelOffset: Double,
        informalName: String?
    ) : Band(
        band, uploadFrequencyLowerBound, uploadFrequencyUpperBound, downloadFrequencyLowerBound,
        downloadFrequencyUpperBound, uploadChannelLowerBound, uploadChannelUpperBound, channelOffset, informalName
    ) {
        override fun getStep(): Double = 0.2
    }

    class GSMBand(
        band: Int,
        uploadFrequencyLowerBound: Double,
        uploadFrequencyUpperBound: Double,
        downloadFrequencyLowerBound: Double,
        downloadFrequencyUpperBound: Double,
        uploadChannelLowerBound: Double,
        uploadChannelUpperBound: Double,
        channelOffset: Double,
        informalName: String?
    ) : Band(
        band, uploadFrequencyLowerBound, uploadFrequencyUpperBound, downloadFrequencyLowerBound,
        downloadFrequencyUpperBound, uploadChannelLowerBound, uploadChannelUpperBound, channelOffset, informalName
    ) {
        override fun getStep(): Double = 0.2
    }

    class WifiBand(
        private val frequency: Int,
        private val channelNumber: Int,
        private val informalName: String?
    ) {
        fun getInformalName(): String? = informalName

        fun getChannelNumber(): Int = channelNumber

        fun getFrequency(): Int = frequency
    }

    private val nrBands: HashMap<Int, NRBand> = LinkedHashMap<Int, NRBand>().apply {
        put(1, NRBand(1, 1920.0, 1980.0, 2110.0, 2170.0, "2100 MHz"))
        put(3, NRBand(3, 1710.0, 1785.0, 1805.0, 1880.0, "1800 MHz"))
        put(8, NRBand(8, 880.0, 915.0, 925.0, 960.0, "900 MHz"))
        put(7, NRBand(7, 2500.0, 2570.0, 2620.0, 2690.0, "2600 MHz"))
        put(78, NRBand(78, 3300.0, 3800.0, 3300.0, 3800.0, "3600 MHz"))
        put(75, NRBand(75, 0.0, 0.0, 1432.0, 1517.0, "1500 MHz"))
        put(258, NRBand(258, 24250.0, 27500.0, 24250.0, 27500.0, "26 GHz"))
        put(20, NRBand(20, 832.0, 862.0, 791.0, 821.0, "800 MHz"))
        put(28, NRBand(28, 703.0, 748.0, 758.0, 803.0, "700 MHz"))
        put(40, NRBand(40, 2300.0, 2400.0, 2300.0, 2400.0, "TD 2300 MHz"))
        put(2, NRBand(2, 1850.0, 1910.0, 1930.0, 1990.0, "1900 MHz"))
        put(5, NRBand(5, 824.0, 849.0, 869.0, 894.0, "850 MHz"))
        put(12, NRBand(12, 699.0, 716.0, 729.0, 746.0, "700 MHz US A"))
        put(25, NRBand(25, 1850.0, 1915.0, 1930.0, 1995.0, "1900 MHz +"))
        put(34, NRBand(34, 2010.0, 2025.0, 2010.0, 2025.0, "TD 2 GHz upper"))
        put(38, NRBand(38, 2570.0, 2620.0, 2570.0, 2620.0, "TD 2600 MHz"))
        put(39, NRBand(39, 1880.0, 1920.0, 1880.0, 1920.0, "TD 1900 MHz"))
        put(41, NRBand(41, 2496.0, 2690.0, 2496.0, 2690.0, "TD 2.6 GHz +"))
        put(50, NRBand(50, 1432.0, 1517.0, 1432.0, 1517.0, "TD 1500 MHz +"))
        put(51, NRBand(51, 1427.0, 1432.0, 1427.0, 1432.0, "TD 1500 MHz -"))
        put(66, NRBand(66, 1710.0, 1780.0, 2110.0, 2200.0, "AWS-3"))
        put(70, NRBand(70, 1695.0, 1710.0, 1995.0, 2020.0, "AWS-4"))
        put(71, NRBand(71, 663.0, 698.0, 617.0, 652.0, "600 MHz US"))
        put(74, NRBand(74, 1427.0, 1470.0, 1475.0, 1518.0, "L-Band"))
        put(76, NRBand(76, 0.0, 0.0, 1427.0, 1432.0, "500 MHz -"))
        put(77, NRBand(77, 3300.0, 4200.0, 3300.0, 4200.0, "3600 MHz+"))
        put(79, NRBand(79, 4400.0, 5000.0, 4400.0, 5000.0, "4500 MHz"))
        put(80, NRBand(80, 1710.0, 1785.0, 0.0, 0.0, "SUL 1800 MHz+"))
        put(81, NRBand(81, 880.0, 915.0, 0.0, 0.0, "SUL 900 MHz"))
        put(82, NRBand(82, 832.0, 862.0, 0.0, 0.0, "SUL 800 MHz"))
        put(83, NRBand(83, 703.0, 748.0, 0.0, 0.0, "SUL 700 MHz"))
        put(84, NRBand(84, 1920.0, 1980.0, 0.0, 0.0, "SUL 2100 MHz"))
        put(86, NRBand(86, 1710.0, 1780.0, 0.0, 0.0, "SUL 1800 MHz"))
        put(257, NRBand(257, 26500.0, 29500.0, 26500.0, 29500.0, "28 GHz"))
        put(260, NRBand(260, 37000.0, 40000.0, 37000.0, 40000.0, "39 GHz US"))
        put(261, NRBand(261, 27500.0, 28350.0, 27500.0, 28350.0, "28 GHz US"))
    }

    /**
     * Static list of all LTE Bands taken from the 3gpp 36.101 standard.
     */
    private val lteBands: HashMap<Int, LTEBand> = HashMap<Int, LTEBand>().apply {
        put(1, LTEBand(1, 1920.0, 1980.0, 2110.0, 2170.0, 18000.0, 18599.0, 18000.0, "2100 MHz"))
        put(2, LTEBand(2, 1850.0, 1910.0, 1930.0, 1990.0, 18600.0, 19199.0, 18000.0, "PCS A-F blocks 1,9 GHz"))
        put(3, LTEBand(3, 1710.0, 1785.0, 1805.0, 1880.0, 19200.0, 19949.0, 18000.0, "1800 MHz"))
        put(4, LTEBand(4, 1710.0, 1755.0, 2110.0, 2155.0, 19950.0, 20399.0, 18000.0, "AWS-1 1.7+2.1 GHz"))
        put(5, LTEBand(5, 824.0, 849.0, 869.0, 894.0, 20400.0, 20649.0, 18000.0, "850MHz (was CDMA)"))
        put(6, LTEBand(6, 830.0, 840.0, 875.0, 885.0, 20650.0, 20749.0, 18000.0, "850MHz subset (was CDMA)"))
        put(7, LTEBand(7, 2500.0, 2570.0, 2620.0, 2690.0, 20750.0, 21449.0, 18000.0, "2600 MHz"))
        put(8, LTEBand(8, 880.0, 915.0, 925.0, 960.0, 21450.0, 21799.0, 18000.0, "900 MHz"))
        put(9, LTEBand(9, 1749.9, 1784.9, 1844.9, 1879.9, 21800.0, 22149.0, 18000.0, "DCS1800 subset"))
        put(10, LTEBand(10, 1710.0, 1770.0, 2110.0, 2170.0, 22150.0, 22749.0, 18000.0, "Extended AWS/AWS-2/AWS-3"))
        put(11, LTEBand(11, 1427.9, 1447.9, 1475.9, 1495.9, 22750.0, 22949.0, 18000.0, "1.5 GHz lower"))
        put(12, LTEBand(12, 699.0, 716.0, 729.0, 746.0, 23010.0, 23179.0, 18000.0, "700 MHz lower A(BC) blocks"))
        put(13, LTEBand(13, 777.0, 787.0, 746.0, 756.0, 23180.0, 23279.0, 18000.0, "700 MHz upper C block"))
        put(14, LTEBand(14, 788.0, 798.0, 758.0, 768.0, 23280.0, 23379.0, 18000.0, "700 MHz upper D block"))
        put(17, LTEBand(17, 704.0, 716.0, 734.0, 746.0, 23730.0, 23849.0, 18000.0, "700 MHz lower BC blocks"))
        put(18, LTEBand(18, 815.0, 830.0, 860.0, 875.0, 23850.0, 23999.0, 18000.0, "800 MHz lower"))
        put(19, LTEBand(19, 830.0, 845.0, 875.0, 890.0, 24000.0, 24149.0, 18000.0, "800 MHz upper"))
        put(20, LTEBand(20, 832.0, 862.0, 791.0, 821.0, 24150.0, 24449.0, 18000.0, "800 MHz"))
        put(21, LTEBand(21, 1447.9, 1462.9, 1495.9, 1510.9, 24450.0, 24599.0, 18000.0, "1.5 GHz upper"))
        put(22, LTEBand(22, 3410.0, 3490.0, 3510.0, 3590.0, 24600.0, 25399.0, 18000.0, "3.5 GHz"))
        put(23, LTEBand(23, 2000.0, 2020.0, 2180.0, 2200.0, 25500.0, 25699.0, 18000.0, "2 GHz S-Band"))
        put(24, LTEBand(24, 1626.5, 1660.5, 1525.0, 1559.0, 25700.0, 26039.0, 18000.0, "1.6 GHz L-Band"))
        put(25, LTEBand(25, 1850.0, 1915.0, 1930.0, 1995.0, 26040.0, 26689.0, 18000.0, "PCS A-G blocks 1900"))
        put(26, LTEBand(26, 814.0, 849.0, 859.0, 894.0, 26690.0, 27039.0, 18000.0, "ESMR+ 850 (was: iDEN)"))
        put(27, LTEBand(27, 807.0, 824.0, 852.0, 869.0, 27040.0, 27209.0, 18000.0, "800 MHz SMR (was iDEN)"))
        put(28, LTEBand(28, 703.0, 748.0, 758.0, 803.0, 27210.0, 27659.0, 18000.0, "700 MHz"))
        put(29, LTEBand(29, 0.0, 0.0, 717.0, 728.0, 0.0, 0.0, -9660.0, "700 lower DE blocks (suppl. DL)"))
        put(30, LTEBand(30, 2305.0, 2315.0, 2350.0, 2360.0, 27660.0, 27759.0, 17890.0, "2.3GHz WCS"))
        put(31, LTEBand(31, 452.5, 457.5, 462.5, 467.5, 27760.0, 27809.0, 17890.0, "IMT 450 MHz"))
        put(32, LTEBand(32, 0.0, 0.0, 1452.0, 1496.0, 0.0, 0.0, -9920.0, "1.5 GHz L-Band (suppl. DL)"))
        put(33, LTEBand(33, 1900.0, 1920.0, 1900.0, 1920.0, 36000.0, 36199.0, 0.0, "2 GHz TDD lower"))
        put(34, LTEBand(34, 2010.0, 2025.0, 2010.0, 2025.0, 36200.0, 36349.0, 0.0, "2 GHz TDD upper"))
        put(35, LTEBand(35, 1850.0, 1910.0, 1850.0, 1910.0, 36350.0, 36949.0, 0.0, "1,9 GHz TDD lower"))
        put(36, LTEBand(36, 1930.0, 1990.0, 1930.0, 1990.0, 36950.0, 37549.0, 0.0, "1.9 GHz TDD upper"))
        put(37, LTEBand(37, 1910.0, 1930.0, 1910.0, 1930.0, 37550.0, 37749.0, 0.0, "PCS TDD"))
        put(38, LTEBand(38, 2570.0, 2620.0, 2570.0, 2620.0, 37750.0, 38249.0, 0.0, "2600 MHz TDD"))
        put(39, LTEBand(39, 1880.0, 1920.0, 1880.0, 1920.0, 38250.0, 38649.0, 0.0, "IMT 1.9 GHz TDD (was TD-SCDMA)"))
        put(40, LTEBand(40, 2300.0, 2400.0, 2300.0, 2400.0, 38650.0, 39649.0, 0.0, "2300 MHz"))
        put(41, LTEBand(41, 2496.0, 2690.0, 2496.0, 2690.0, 39650.0, 41589.0, 0.0, "Expanded TDD 2.6 GHz"))
        put(42, LTEBand(42, 3400.0, 3600.0, 3400.0, 3600.0, 41590.0, 43589.0, 0.0, "3,4-3,6 GHz"))
        put(43, LTEBand(43, 3600.0, 3800.0, 3600.0, 3800.0, 43590.0, 45589.0, 0.0, "3.6-3,8 GHz"))
        put(44, LTEBand(44, 703.0, 803.0, 703.0, 803.0, 45590.0, 46589.0, 0.0, "700 MHz APT TDD"))
        put(45, LTEBand(45, 1447.0, 1467.0, 1447.0, 1467.0, 46590.0, 46789.0, 0.0, "1500 MHz"))
        put(46, LTEBand(46, 5150.0, 5925.0, 5150.0, 5925.0, 46790.0, 54539.0, 0.0, "TD Unlicensed"))
        put(47, LTEBand(47, 5855.0, 5925.0, 5855.0, 5925.0, 54540.0, 55239.0, 0.0, "V2X TDD"))
        put(65, LTEBand(65, 1920.0, 2010.0, 2110.0, 2200.0, 131072.0, 131971.0, 65536.0, "Extended IMT 2100"))
        put(66, LTEBand(66, 1710.0, 1780.0, 2110.0, 2200.0, 131972.0, 132671.0, 65536.0, "AWS-3"))
        put(67, LTEBand(67, 0.0, 0.0, 738.0, 758.0, 0.0, 0.0, -67336.0, "700 EU SDL"))
        put(68, LTEBand(68, 698.0, 728.0, 753.0, 783.0, 132672.0, 132971.0, 65136.0, "700 ME"))
        put(69, LTEBand(69, 0.0, 0.0, 2570.0, 2620.0, 0.0, 0.0, -67836.0, "IMT-E FDD CA"))
        put(70, LTEBand(70, 1695.0, 1710.0, 1995.0, 2020.0, 132972.0, 133121.0, 64636.0, "AWS-4"))
        put(0, LTEBand(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, null))
    }

    /**
     * Static list of all UMTS Bands taken from the 3GPP TS 25.101 standard.
     */
    private val umtsBands: HashMap<Int, UMTSBand> = HashMap<Int, UMTSBand>().apply {
        put(1, UMTSBand(1, 1922.4, 1977.6, 2112.4, 2167.6, 9612.0, 9888.0, -950.0, "2100 MHz"))
        put(2, UMTSBand(2, 1852.4, 1907.6, 1932.4, 1987.6, 9262.0, 9538.0, -400.0, "1900 MHz PCS"))
        put(3, UMTSBand(3, 1712.4, 1782.6, 1807.4, 1877.6, 937.0, 1288.0, -225.0, "1800 MHz DCS"))
        put(5, UMTSBand(5, 826.4, 846.6, 871.4, 891.6, 4132.0, 4233.0, -225.0, "850 MHz"))
        put(6, UMTSBand(6, 832.4, 837.6, 877.4, 882.6, 4162.0, 4188.0, -225.0, "850 MHz Japan"))
        put(7, UMTSBand(7, 2502.4, 2567.6, 2622.4, 2687.6, 2012.0, 2338.0, -225.0, "2600 MHz"))
        put(8, UMTSBand(8, 882.4, 912.6, 927.4, 957.6, 2712.0, 2863.0, -225.0, "900 MHz"))
        put(9, UMTSBand(9, 1752.4, 1782.4, 1847.4, 1877.4, 8762.0, 8912.0, -475.0, "1800 MHz Japan"))
        put(10, UMTSBand(10, 1712.4, 1767.6, 2112.4, 2167.6, 2887.0, 3163.0, -225.0, "AWS-1+"))
        put(4, UMTSBand(4, 1712.4, 1752.6, 2112.4, 2152.6, 1312.0, 1513.0, -225.0, "AWS-1"))
        put(11, UMTSBand(11, 1430.4, 1445.4, 1478.4, 1493.4, 3487.0, 3562.0, -225.0, "1500 MHz Lower"))
        put(12, UMTSBand(12, 701.4, 713.6, 731.4, 743.6, 3617.0, 3678.0, -225.0, "700 MHz US a"))
        put(13, UMTSBand(13, 779.4, 784.6, 748.4, 753.6, 3792.0, 3818.0, -225.0, "700 MHz US c"))
        put(14, UMTSBand(14, 790.4, 795.6, 760.4, 765.6, 3892.0, 3918.0, -225.0, "700 MHz US PS"))
        put(19, UMTSBand(19, 832.4, 842.6, 877.4, 887.6, 312.0, 363.0, -400.0, "800 MHz Japan"))
        put(20, UMTSBand(20, 834.4, 859.6, 793.4, 818.6, 4287.0, 4413.0, -225.0, "800 MHz EU DD"))
        put(21, UMTSBand(21, 1450.4, 1460.4, 1498.4, 1508.4, 462.0, 512.0, -400.0, "1500 MHz Upper"))
        put(22, UMTSBand(22, 3412.4, 3487.6, 3512.4, 3587.6, 4437.0, 4813.0, -225.0, "3500 MHz"))
        put(25, UMTSBand(25, 1852.4, 1912.6, 1932.4, 1992.6, 4887.0, 5188.0, -225.0, "1900+ MHz"))
        put(26, UMTSBand(26, 816.4, 846.6, 861.4, 891.6, 5537.0, 5688.0, -225.0, "850+ MHz"))
    }

    /**
     * Static list of all GSM Bands taken from the 3GPP TS 45.005 standard.
     * order by priority (if one arfcn is contained in multiple bands)
     */
    private val gsmBands: List<GSMBand> = ArrayList<GSMBand>().apply {
        add(GSMBand(5, 824.2, 848.8, 869.2, 893.8, 128.0, 251.0, 0.0, "GSM 850"))
        add(GSMBand(8, 890.2, 914.8, 935.2, 959.8, 1.0, 124.0, 0.0, "GSM 900"))
        add(GSMBand(8, 890.0, 914.8, 935.0, 959.8, 0.0, 124.0, 0.0, "GSM 900"))
        add(GSMBand(8, 880.2, 889.8, 925.2, 934.8, 975.0, 1023.0, 0.0, "GSM 900"))
        add(GSMBand(8, 890.0, 914.8, 935.0, 959.8, 0.0, 124.0, 0.0, "GSM 900"))
        add(GSMBand(8, 876.2, 889.8, 921.2, 934.8, 955.0, 1023.0, 0.0, "GSM 900"))
        add(GSMBand(3, 1710.2, 1784.8, 1805.2, 1879.8, 512.0, 885.0, 0.0, "GSM 1800"))
        add(GSMBand(2, 1850.2, 1909.8, 1930.2, 1989.8, 512.0, 810.0, 0.0, "GSM 1900"))
    }

    private val wifiBands: HashMap<Int, WifiBand> = HashMap<Int, WifiBand>().apply {
        // 2,4 GHz
        put(2412, WifiBand(2412, 1, "2.4 GHz"))
        put(2417, WifiBand(2417, 2, "2.4 GHz"))
        put(2422, WifiBand(2422, 3, "2.4 GHz"))
        put(2427, WifiBand(2427, 4, "2.4 GHz"))
        put(2432, WifiBand(2432, 5, "2.4 GHz"))
        put(2437, WifiBand(2437, 6, "2.4 GHz"))
        put(2442, WifiBand(2442, 7, "2.4 GHz"))
        put(2447, WifiBand(2447, 8, "2.4 GHz"))
        put(2452, WifiBand(2452, 9, "2.4 GHz"))
        put(2457, WifiBand(2457, 10, "2.4 GHz"))
        put(2462, WifiBand(2462, 11, "2.4 GHz"))
        put(2467, WifiBand(2467, 12, "2.4 GHz"))
        put(2472, WifiBand(2472, 13, "2.4 GHz"))
        put(2484, WifiBand(2484, 14, "2.4 GHz"))
        // 5 GHz
        put(5160, WifiBand(5160, 32, "5 GHz"))
        put(5170, WifiBand(5170, 34, "5 GHz"))
        put(5180, WifiBand(5180, 36, "5 GHz"))
        put(5190, WifiBand(5190, 38, "5 GHz"))
        put(5200, WifiBand(5200, 40, "5 GHz"))
        put(5210, WifiBand(5210, 42, "5 GHz"))
        put(5220, WifiBand(5220, 44, "5 GHz"))
        put(5230, WifiBand(5230, 46, "5 GHz"))
        put(5240, WifiBand(5240, 48, "5 GHz"))
        put(5250, WifiBand(5250, 50, "5 GHz"))
        put(5260, WifiBand(5260, 52, "5 GHz"))
        put(5270, WifiBand(5270, 54, "5 GHz"))
        put(5280, WifiBand(5280, 56, "5 GHz"))
        put(5290, WifiBand(5290, 58, "5 GHz"))
        put(5300, WifiBand(5300, 60, "5 GHz"))
        put(5310, WifiBand(5310, 62, "5 GHz"))
        put(5320, WifiBand(5320, 64, "5 GHz"))
        put(5340, WifiBand(5340, 68, "5 GHz"))
        put(5480, WifiBand(5480, 96, "5 GHz"))
        put(5500, WifiBand(5500, 100, "5 GHz"))
        put(5510, WifiBand(5510, 102, "5 GHz"))
        put(5520, WifiBand(5520, 104, "5 GHz"))
        put(5530, WifiBand(5530, 106, "5 GHz"))
        put(5540, WifiBand(5540, 108, "5 GHz"))
        put(5550, WifiBand(5550, 110, "5 GHz"))
        put(5560, WifiBand(5560, 112, "5 GHz"))
        put(5570, WifiBand(5570, 114, "5 GHz"))
        put(5580, WifiBand(5580, 116, "5 GHz"))
        put(5590, WifiBand(5590, 118, "5 GHz"))
        put(5600, WifiBand(5600, 120, "5 GHz"))
        put(5610, WifiBand(5610, 122, "5 GHz"))
        put(5620, WifiBand(5620, 124, "5 GHz"))
        put(5630, WifiBand(5630, 126, "5 GHz"))
        put(5640, WifiBand(5640, 128, "5 GHz"))
        put(5660, WifiBand(5660, 132, "5 GHz"))
        put(5670, WifiBand(5670, 134, "5 GHz"))
        put(5680, WifiBand(5680, 136, "5 GHz"))
        put(5690, WifiBand(5690, 138, "5 GHz"))
        put(5700, WifiBand(5700, 140, "5 GHz"))
        put(5710, WifiBand(5710, 142, "5 GHz"))
        put(5720, WifiBand(5720, 144, "5 GHz"))
        put(5745, WifiBand(5745, 149, "5 GHz"))
        put(5755, WifiBand(5755, 151, "5 GHz"))
        put(5765, WifiBand(5765, 153, "5 GHz"))
        put(5775, WifiBand(5775, 155, "5 GHz"))
        put(5785, WifiBand(5785, 157, "5 GHz"))
        put(5795, WifiBand(5795, 159, "5 GHz"))
        put(5805, WifiBand(5805, 161, "5 GHz"))
        put(5825, WifiBand(5825, 165, "5 GHz"))
        put(5845, WifiBand(5845, 169, "5 GHz"))
        put(5865, WifiBand(5865, 173, "5 GHz"))
        put(4915, WifiBand(4915, 183, "5 GHz"))
        put(4920, WifiBand(4920, 184, "5 GHz"))
        put(4925, WifiBand(4925, 185, "5 GHz"))
        put(4935, WifiBand(4935, 187, "5 GHz"))
        put(4940, WifiBand(4940, 188, "5 GHz"))
        put(4945, WifiBand(4945, 189, "5 GHz"))
        put(4960, WifiBand(4960, 192, "5 GHz"))
        put(4980, WifiBand(4980, 196, "5 GHz"))
        // 6 GHz - 6425-7125 MHz, EU: 5945 - 6425 MHz
        put(5955, WifiBand(5955, 1, "6 GHz"))
        put(5975, WifiBand(5975, 2, "6 GHz"))
        put(5995, WifiBand(5995, 3, "6 GHz"))
        put(6015, WifiBand(6015, 4, "6 GHz"))
        put(6035, WifiBand(6035, 5, "6 GHz"))
        put(6055, WifiBand(6055, 6, "6 GHz"))
        put(6075, WifiBand(6075, 7, "6 GHz"))
        put(6095, WifiBand(6095, 8, "6 GHz"))
        put(6115, WifiBand(6115, 9, "6 GHz"))
        put(6135, WifiBand(6135, 10, "6 GHz"))
        put(6155, WifiBand(6155, 11, "6 GHz"))
        put(6175, WifiBand(6175, 12, "6 GHz"))
        put(6195, WifiBand(6195, 13, "6 GHz"))
        put(6215, WifiBand(6215, 14, "6 GHz"))
        put(6235, WifiBand(6235, 15, "6 GHz"))
        put(6255, WifiBand(6255, 16, "6 GHz"))
        put(6275, WifiBand(6275, 17, "6 GHz"))
        put(6295, WifiBand(6295, 18, "6 GHz"))
        put(6315, WifiBand(6315, 19, "6 GHz"))
        put(6335, WifiBand(6335, 20, "6 GHz"))
        put(6355, WifiBand(6355, 21, "6 GHz"))
        put(6375, WifiBand(6375, 22, "6 GHz"))
        put(6395, WifiBand(6395, 23, "6 GHz"))
        put(6415, WifiBand(6415, 24, "6 GHz"))
        put(6435, WifiBand(6435, 25, "6 GHz"))
        put(6455, WifiBand(6455, 26, "6 GHz"))
        put(6475, WifiBand(6475, 27, "6 GHz"))
        put(6495, WifiBand(6495, 28, "6 GHz"))
        put(6515, WifiBand(6515, 29, "6 GHz"))
        put(6535, WifiBand(6535, 30, "6 GHz"))
        put(6555, WifiBand(6555, 31, "6 GHz"))
        put(6575, WifiBand(6575, 32, "6 GHz"))
        put(6595, WifiBand(6595, 33, "6 GHz"))
        put(6615, WifiBand(6615, 34, "6 GHz"))
        put(6635, WifiBand(6635, 35, "6 GHz"))
        put(6655, WifiBand(6655, 36, "6 GHz"))
        put(6675, WifiBand(6675, 37, "6 GHz"))
        put(6695, WifiBand(6695, 38, "6 GHz"))
        put(6715, WifiBand(6715, 39, "6 GHz"))
        put(6735, WifiBand(6735, 40, "6 GHz"))
        put(6755, WifiBand(6755, 41, "6 GHz"))
        put(6775, WifiBand(6775, 42, "6 GHz"))
        put(6795, WifiBand(6795, 43, "6 GHz"))
        put(6815, WifiBand(6815, 44, "6 GHz"))
        put(6835, WifiBand(6835, 45, "6 GHz"))
        put(6855, WifiBand(6855, 46, "6 GHz"))
        put(6875, WifiBand(6875, 47, "6 GHz"))
        put(6895, WifiBand(6895, 48, "6 GHz"))
        put(6915, WifiBand(6915, 49, "6 GHz"))
        put(6935, WifiBand(6935, 50, "6 GHz"))
        put(6955, WifiBand(6955, 51, "6 GHz"))
        put(6975, WifiBand(6975, 52, "6 GHz"))
        put(6995, WifiBand(6995, 53, "6 GHz"))
        put(7015, WifiBand(7015, 54, "6 GHz"))
        put(7035, WifiBand(7035, 55, "6 GHz"))
        put(7055, WifiBand(7055, 56, "6 GHz"))
        put(7075, WifiBand(7075, 57, "6 GHz"))
        put(7095, WifiBand(7095, 58, "6 GHz"))
        put(7115, WifiBand(7115, 59, "6 GHz"))
        // 60 GHz
        put(58320, WifiBand(58320, 1, "60 GHz"))
        put(60480, WifiBand(60480, 2, "60 GHz"))
        put(62640, WifiBand(62640, 3, "60 GHz"))
        put(64800, WifiBand(64800, 4, "60 GHz"))
        put(66960, WifiBand(66960, 5, "60 GHz"))
        put(69120, WifiBand(69120, 6, "60 GHz"))
    }
}
