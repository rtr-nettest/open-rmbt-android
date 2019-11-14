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

package at.specure.info.network

import android.telephony.TelephonyManager
import timber.log.Timber

/**
 * Type of Mobile network technology according to [android.telephony.TelephonyManager].NETWORK_TYPE_*
 */
enum class MobileNetworkType(val intValue: Int, val displayName: String) {

    /**
     *  Network type is unknown
     */
    UNKNOWN(TelephonyManager.NETWORK_TYPE_UNKNOWN, "UNKNOWN"),

    /**
     * Current network is GPRS
     */
    GPRS(TelephonyManager.NETWORK_TYPE_GPRS, "GPRS"),

    /**
     * Current network is EDGE
     */
    EDGE(TelephonyManager.NETWORK_TYPE_EDGE, "EDGE"),

    /**
     * Current network is UMTS
     */
    UMTS(TelephonyManager.NETWORK_TYPE_UMTS, "UMTS"),

    /**
     * Current network is CDMA: Either IS95A or IS95B
     */
    CDMA(TelephonyManager.NETWORK_TYPE_CDMA, "CDMA"),

    /**
     * Current network is EVDO revision 0
     */
    EVDO_0(TelephonyManager.NETWORK_TYPE_EVDO_0, "EVDO_0"),

    /**
     *  Current network is EVDO revision A
     */
    EVDO_A(TelephonyManager.NETWORK_TYPE_EVDO_A, "EVDO_A"),

    /**
     * Current network is 1xRTT
     */
    _1xRTT(TelephonyManager.NETWORK_TYPE_1xRTT, "1xRTT"),

    /**
     * Current network is HSDPA
     */
    HSDPA(TelephonyManager.NETWORK_TYPE_HSDPA, "HSDPA"),

    /**
     *  Current network is HSUPA
     */
    HSUPA(TelephonyManager.NETWORK_TYPE_HSUPA, "HSUPA"),

    /**
     * Current network is HSPA
     */
    HSPA(TelephonyManager.NETWORK_TYPE_HSPA, "HSPA"),

    /**
     * Current network is iDen
     */
    IDEN(TelephonyManager.NETWORK_TYPE_IDEN, "IDEN"),

    /**
     * Current network is EVDO revision B
     */
    EVDO_B(TelephonyManager.NETWORK_TYPE_EVDO_B, "EVDO_B"),

    /**
     * Current network is LTE
     */
    LTE(TelephonyManager.NETWORK_TYPE_LTE, "LTE"),

    /**
     * Current network is eHRPD
     */
    EHRPD(TelephonyManager.NETWORK_TYPE_EHRPD, "EHRPD"),

    /**
     * Current network is HSPA+
     */
    HSPAP(TelephonyManager.NETWORK_TYPE_HSPAP, "HSPA+"),

    /**
     *  Current network is GSM
     */
    GSM(16, "GSM"), // TelephonyManager.NETWORK_TYPE_GSM

    /**
     *  Current network is TD_SCDMA
     */
    TD_SCDMA(17, "TD-SCDMA"), // TelephonyManager.NETWORK_TYPE_TD_SCDMA

    /**
     * Current network is IWLAN
     */
    IWLAN(18, "IWLAN"), // TelephonyManager.NETWORK_TYPE_IWLAN

    /**
     * Current network is LTE_CA {@hide}
     */
    LTE_CA(19, "LTE CA"), // TelephonyManager.NETWORK_TYPE_LTE_CA

    /**
     *  Current network is NR(New Radio) 5G.
     */
    NR(20, "NR"); // TelephonyManager.NETWORK_TYPE_NR

    companion object {

        fun fromValue(intValue: Int): MobileNetworkType {
            for (value in values()) {
                if (value.intValue == intValue) {
                    return value
                }
            }
            Timber.w("Mobile network type $intValue not found in known types list")
            return UNKNOWN
        }
    }
}