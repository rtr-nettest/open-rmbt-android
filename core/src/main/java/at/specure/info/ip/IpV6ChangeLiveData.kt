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

package at.specure.info.ip

import androidx.lifecycle.LiveData
import javax.inject.Inject

/**
 * LiveData that tracks changes of IPv6 info
 */
class IpV6ChangeLiveData @Inject constructor(private val watcher: IpChangeWatcher) : LiveData<IpInfo?>(),
    IpChangeWatcher.OnIpV6ChangedListener {

    init {
        value = watcher.lastIPv6Address
    }

    override fun onIpV6Changed(info: IpInfo) {
        postValue(info)
    }

    override fun onActive() {
        super.onActive()
        watcher.addListener(this)
    }

    override fun onInactive() {
        super.onInactive()
        watcher.removeListener(this)
    }
}