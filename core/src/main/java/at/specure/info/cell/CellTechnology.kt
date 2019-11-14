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

import at.specure.info.network.MobileNetworkType

/**
 * Mobile network technology generations
 */
enum class CellTechnology(val displayName: String) {
    CONNECTION_2G("2G"),
    CONNECTION_3G("3G"),
    CONNECTION_4G("4G"),
    CONNECTION_5G("5G");

    companion object {
        fun fromMobileNetworkType(type: MobileNetworkType) = when (type) {
            MobileNetworkType.GPRS,
            MobileNetworkType.EDGE,
            MobileNetworkType.UMTS,
            MobileNetworkType.CDMA,
            MobileNetworkType.EVDO_0,
            MobileNetworkType.EVDO_A,
            MobileNetworkType.EVDO_B,
            MobileNetworkType.IDEN,
            MobileNetworkType.EHRPD,
            MobileNetworkType._1xRTT,
            MobileNetworkType.GSM -> CONNECTION_2G
            MobileNetworkType.TD_SCDMA,
            MobileNetworkType.HSDPA,
            MobileNetworkType.HSUPA,
            MobileNetworkType.HSPA,
            MobileNetworkType.HSPAP -> CONNECTION_3G
            MobileNetworkType.LTE_CA,
            MobileNetworkType.LTE -> CONNECTION_4G
            MobileNetworkType.NR -> CONNECTION_5G
            MobileNetworkType.IWLAN,
            MobileNetworkType.UNKNOWN -> null
        }
    }
}