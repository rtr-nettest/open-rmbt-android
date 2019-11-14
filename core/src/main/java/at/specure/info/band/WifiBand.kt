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

/**
 * Contains information about wlan band and frequency available for current network
 */
data class WifiBand(
    /**
     * wlan frequency in Hz
     */
    val frequency: Int,

    /**
     * Channel number of current network
     */
    val channelNumber: Int,

    /**
     * Frequency group of current network in human-readable format
     */
    val name: String
) : Band() {

    override val informalName: String
        get() = name

    companion object {

        private val band_2_4GHz: Map<Int, Int> = mutableMapOf<Int, Int>().also {
            it[2412] = 1
            it[2417] = 2
            it[2422] = 3
            it[2427] = 4
            it[2432] = 5
            it[2437] = 6
            it[2442] = 7
            it[2447] = 8
            it[2452] = 9
            it[2457] = 10
            it[2462] = 11
            it[2467] = 12
            it[2472] = 13
            it[2484] = 14
        }

        private val band_5GHz: Map<Int, Int> = mutableMapOf<Int, Int>().also {
            it[5160] = 32
            it[5170] = 34
            it[5180] = 36
            it[5190] = 38
            it[5200] = 40
            it[5210] = 42
            it[5220] = 44
            it[5230] = 46
            it[5240] = 48
            it[5250] = 50
            it[5260] = 52
            it[5270] = 54
            it[5280] = 56
            it[5290] = 58
            it[5300] = 60
            it[5310] = 62
            it[5320] = 64
            it[5340] = 68
            it[5480] = 96
            it[5500] = 100
            it[5510] = 102
            it[5520] = 104
            it[5530] = 106
            it[5540] = 108
            it[5550] = 110
            it[5560] = 112
            it[5570] = 114
            it[5580] = 116
            it[5590] = 118
            it[5600] = 120
            it[5610] = 122
            it[5620] = 124
            it[5630] = 126
            it[5640] = 128
            it[5660] = 132
            it[5670] = 134
            it[5680] = 136
            it[5690] = 138
            it[5700] = 140
            it[5710] = 142
            it[5720] = 144
            it[5745] = 149
            it[5755] = 151
            it[5765] = 153
            it[5775] = 155
            it[5785] = 157
            it[5795] = 159
            it[5805] = 161
            it[5825] = 165
            it[5845] = 169
            it[5865] = 173
            it[4915] = 183
            it[4920] = 184
            it[4925] = 185
            it[4935] = 187
            it[4940] = 188
            it[4945] = 189
            it[4960] = 192
            it[4980] = 196
        }

        private val band_60GHz: Map<Int, Int> = mutableMapOf<Int, Int>().also {
            it[58320] = 1
            it[60480] = 2
            it[62640] = 3
            it[64800] = 4
            it[66960] = 5
            it[69120] = 6
        }

        /**
         * Returns [WifiBand] by frequency value
         */
        fun fromFrequency(frequency: Int): WifiBand {
            return when {
                band_2_4GHz.containsKey(frequency) -> WifiBand(frequency, band_2_4GHz[frequency] ?: error("$frequency not found"), "2.4 GHz")
                band_5GHz.containsKey(frequency) -> WifiBand(frequency, band_5GHz[frequency] ?: error("$frequency not found"), "5 GHz")
                band_60GHz.containsKey(frequency) -> WifiBand(frequency, band_60GHz[frequency] ?: error("$frequency not found"), "60 GHz")
                else -> throw IllegalArgumentException("Wifi band not found for $frequency frequency")
            }
        }
    }
}