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

import android.Manifest
import android.content.Context
import android.os.Build
import at.specure.util.isCoarseLocationPermitted
import java.util.Collections

/**
 * Implementation of [LocationAccess] that
 * checks [android.Manifest.permission.ACCESS_FINE_LOCATION] granted
 */
class LocationAccessImpl(private val context: Context) : LocationAccess {

    private val listeners = Collections.synchronizedSet(mutableSetOf<LocationAccess.LocationAccessChangeListener>())

    override val requiredPermission = Manifest.permission.ACCESS_FINE_LOCATION

    override val monitoredPermission: Array<String>
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }

    override val isAllowed: Boolean
        get() = context.isCoarseLocationPermitted()

    override fun notifyPermissionsUpdated() {
        val allowed = isAllowed
        listeners.forEach {
            it.onLocationAccessChanged(allowed)
        }
    }

    override fun addListener(listener: LocationAccess.LocationAccessChangeListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: LocationAccess.LocationAccessChangeListener) {
        listeners.remove(listener)
    }
}