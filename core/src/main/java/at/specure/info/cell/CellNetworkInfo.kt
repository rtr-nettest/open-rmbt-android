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

package at.specure.info.cell

import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.SubscriptionInfo
import at.specure.info.TransportType
import at.specure.info.band.CellBand
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NetworkInfo

/**
 * Cellular Network information
 */
class CellNetworkInfo(

    /**
     * Provider or sim operator name
     */
    val providerName: String,

    /**
     * Cell band information of current network
     */
    val band: CellBand?,

    /**
     * Detailed Cellular Network type
     */
    val networkType: MobileNetworkType
) :
    NetworkInfo(TransportType.CELLULAR) {

    override val name: String?
        get() = providerName

    companion object {

        /**
         * Creates [CellNetworkInfo] from initial data objects
         */
        fun from(info: CellInfo, subscriptionInfo: SubscriptionInfo, networkType: MobileNetworkType): CellNetworkInfo {
            val providerName = subscriptionInfo.displayName.toString()
            return when (info) {
                is CellInfoLte -> fromLte(info, providerName, networkType)
                is CellInfoWcdma -> fromWcdma(info, providerName, networkType)
                is CellInfoGsm -> fromGsm(info, providerName, networkType)
                is CellInfoCdma -> fromCdma(info, providerName, networkType)
                else -> throw IllegalArgumentException("Unknown cell info cannot be extracted ${info::class.java.name}")
            }
        }

        private fun fromLte(info: CellInfoLte, providerName: String, networkType: MobileNetworkType): CellNetworkInfo {

            val band: CellBand? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                CellBand.fromChannelNumber(info.cellIdentity.earfcn, CellChannelAttribution.EARFCN)
            } else {
                null
            }

            return CellNetworkInfo(
                providerName = providerName,
                band = band,
                networkType = networkType
            )
        }

        private fun fromWcdma(info: CellInfoWcdma, providerName: String, networkType: MobileNetworkType): CellNetworkInfo {
            val band: CellBand? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                CellBand.fromChannelNumber(info.cellIdentity.uarfcn, CellChannelAttribution.UARFCN)
            } else {
                null
            }

            return CellNetworkInfo(
                providerName = providerName,
                band = band,
                networkType = networkType
            )
        }

        private fun fromGsm(info: CellInfoGsm, providerName: String, networkType: MobileNetworkType): CellNetworkInfo {
            val band: CellBand? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                CellBand.fromChannelNumber(info.cellIdentity.arfcn, CellChannelAttribution.ARFCN)
            } else {
                null
            }

            return CellNetworkInfo(
                providerName = providerName,
                band = band,
                networkType = networkType
            )
        }

        private fun fromCdma(info: CellInfoCdma, providerName: String, networkType: MobileNetworkType): CellNetworkInfo {
            return CellNetworkInfo(
                providerName = providerName,
                band = null,
                networkType = networkType
            )
        }
    }
}