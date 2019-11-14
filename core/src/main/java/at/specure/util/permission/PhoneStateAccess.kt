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
 * Checks phone state permission is permitted
 */
interface PhoneStateAccess : PermissionAccess {

    /**
     * Add listener to handle phone state permission changes
     */
    fun addListener(listener: PhoneStateAccessChangeListener)

    /**
     * Remove listener from handling phone state permission changes
     */
    fun removeListener(listener: PhoneStateAccessChangeListener)

    /**
     * Listener to receive permission updates from [PhoneStateAccess]
     */
    interface PhoneStateAccessChangeListener {

        /**
         * Will be triggered when phone state permission was changed
         */
        fun onPhoneStateAccessChanged(isAllowed: Boolean)
    }
}