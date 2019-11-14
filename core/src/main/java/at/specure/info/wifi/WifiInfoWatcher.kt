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

package at.specure.info.wifi

import at.specure.info.network.WifiNetworkInfo

/**
 * WiFi watcher that allows to get active wifi connection information
 */
interface WifiInfoWatcher {

    /**
     * Return an [WifiNetworkInfo] object if device has WiFi connection active and available
     */
    val activeWifiInfo: WifiNetworkInfo?
}