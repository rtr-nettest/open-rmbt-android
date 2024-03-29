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

package at.rmbt.client.control

/**
 * Describes IP protocol types
 */
enum class MapFilterTypeClass(val serverValue: String) {
    MAP_TYPE("MAP_TYPE"),
    MAP_FILTER_TECHNOLOGY("MAP_FILTER_TECHNOLOGY"),
    MAP_FILTER_CARRIER("MAP_FILTER_CARRIER"),
    MAP_FILTER_APPEARANCE("MAP_FILTER_APPEARANCE"),
    MAP_OVERLAY_TYPE("OVERLAY_TYPE"),
    MAP_FILTER_PERIOD("MAP_FILTER_PERIOD"),
    MAP_FILTER_STATISTIC("MAP_FILTER_STATISTIC"); // this value is not in API

    companion object {
        fun fromServerString(type: String): MapFilterTypeClass? {
            MapFilterTypeClass.values().forEach { x ->
                if (x.serverValue.contentEquals(type, true)) {
                    return x
                }
            }
            return MAP_FILTER_STATISTIC //  because that one has no icon filled in API
        }
    }
}