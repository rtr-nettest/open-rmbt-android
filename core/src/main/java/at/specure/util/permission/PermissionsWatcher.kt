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

import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.core.content.ContextCompat.checkSelfPermission

/**
 * Class that aggregates all [PermissionAccess] and manages its states and changes
 */
class PermissionsWatcher(val context: Context, vararg handlers: PermissionAccess) {

    val allPermissions = mutableListOf<String>().apply {
        handlers.forEach { addAll(it.monitoredPermission) }
    }

    private val _handlers: List<PermissionAccess> = mutableListOf<PermissionAccess>().apply {
        handlers.forEach {
            add(it)
        }
    }

    val requiredPermissions: Array<String> = _handlers.map { it.requiredPermission }.toTypedArray()

    fun notifyPermissionsUpdated() {
        _handlers.forEach {
            it.notifyPermissionsUpdated()
        }
    }

    fun checkPermission(permission: String): Boolean {
        return checkSelfPermission(context, permission) == PERMISSION_GRANTED
    }
}