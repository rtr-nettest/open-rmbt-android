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

import androidx.lifecycle.LiveData
import javax.inject.Inject

/**
 * LiveData that observes changes of active network from [ActiveNetworkWatcher]
 * If no active connection is available null well be posted
 */
class ActiveNetworkLiveData @Inject constructor(private val activeNetworkWatcher: ActiveNetworkWatcher) : LiveData<DetailedNetworkInfo>(),
    ActiveNetworkWatcher.NetworkChangeListener {

    override fun onActiveNetworkChanged(detailedNetworkInfo: DetailedNetworkInfo) {
        postValue(detailedNetworkInfo)
    }

    override fun onActive() {
        super.onActive()
        activeNetworkWatcher.addListener(this)
    }

    override fun onInactive() {
        super.onInactive()
        activeNetworkWatcher.removeListener(this)
    }
}