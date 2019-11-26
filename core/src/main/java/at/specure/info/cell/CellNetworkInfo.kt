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
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityWcdma
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
import java.util.UUID

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
    val networkType: MobileNetworkType,

    val mnc: Int?,

    val mcc: Int?,

    val locationId: Int?,

    val areaCode: Int?,

    val scramblingCode: Int?,

    /**
     * Random generated cell UUID
     */
    cellUUID: String
) :
    NetworkInfo(TransportType.CELLULAR, cellUUID) {

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
                networkType = networkType,
                mcc = info.cellIdentity.mccCompat(),
                mnc = info.cellIdentity.mncCompat(),
                locationId = info.cellIdentity.tac,
                areaCode = info.cellIdentity.ci,
                scramblingCode = info.cellIdentity.pci,
                cellUUID = UUID.nameUUIDFromBytes((providerName + band.hashCode()).toByteArray()).toString()
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
                networkType = networkType,
                mcc = info.cellIdentity.mccCompat(),
                mnc = info.cellIdentity.mncCompat(),
                locationId = info.cellIdentity.lac,
                areaCode = info.cellIdentity.cid,
                scramblingCode = info.cellIdentity.psc,
                cellUUID = UUID.nameUUIDFromBytes((providerName + band.hashCode()).toByteArray()).toString()
            )
        }

        private fun fromGsm(info: CellInfoGsm, providerName: String, networkType: MobileNetworkType): CellNetworkInfo {
            val band: CellBand? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                CellBand.fromChannelNumber(info.cellIdentity.arfcn, CellChannelAttribution.ARFCN)
            } else {
                null
            }

            val scramblingCode: Int? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) info.cellIdentity.bsic else null

            return CellNetworkInfo(
                providerName = providerName,
                band = band,
                networkType = networkType,
                mcc = info.cellIdentity.mccCompat(),
                mnc = info.cellIdentity.mncCompat(),
                locationId = info.cellIdentity.lac,
                areaCode = info.cellIdentity.cid,
                scramblingCode = scramblingCode,
                cellUUID = UUID.nameUUIDFromBytes((providerName + band.hashCode()).toByteArray()).toString()
            )
        }

        private fun fromCdma(info: CellInfoCdma, providerName: String, networkType: MobileNetworkType): CellNetworkInfo {

            return CellNetworkInfo(
                providerName = providerName,
                band = null,
                networkType = networkType,
                mcc = null,
                mnc = null,
                locationId = info.cellIdentity.basestationId,
                areaCode = null,
                scramblingCode = null,
                cellUUID = UUID.nameUUIDFromBytes((providerName + info.hashCode()).toByteArray()).toString()
            )
        }

        private fun CellIdentityLte.mccCompat(): Int? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mccString?.toInt().fixMncMcc()
            } else {
                mcc.fixMncMcc()
            }

        private fun CellIdentityLte.mncCompat(): Int? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mncString?.toInt().fixMncMcc()
            } else {
                mnc.fixMncMcc()
            }

        private fun CellIdentityWcdma.mccCompat(): Int? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mccString?.toInt().fixMncMcc()
            } else {
                mcc.fixMncMcc()
            }

        private fun CellIdentityWcdma.mncCompat(): Int? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mncString?.toInt().fixMncMcc()
            } else {
                mnc.fixMncMcc()
            }

        private fun CellIdentityGsm.mccCompat(): Int? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mccString?.toInt().fixMncMcc()
            } else {
                mcc.fixMncMcc()
            }

        private fun CellIdentityGsm.mncCompat(): Int? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mncString?.toInt().fixMncMcc()
            } else {
                mnc.fixMncMcc()
            }

        private fun Int?.fixMncMcc(): Int? {
            return if (this == null || this == Int.MIN_VALUE || this < 0) {
                null
            } else {
                this
            }
        }
    }
}