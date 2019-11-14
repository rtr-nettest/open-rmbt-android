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

package at.specure.util.permission

/**
 * Checks location permission is permitted
 */
interface LocationAccess : PermissionAccess {

    /**
     * Add listener to handle location permission changes
     */
    fun addListener(listener: LocationAccessChangeListener)

    /**
     * Remove listener from handling location permission changes
     */
    fun removeListener(listener: LocationAccessChangeListener)

    /**
     * Listener to receive permission updates from [LocationAccess]
     */
    interface LocationAccessChangeListener {

        /**
         * Will be triggered when location permission was changed
         */
        fun onLocationAccessChanged(isAllowed: Boolean)
    }
}