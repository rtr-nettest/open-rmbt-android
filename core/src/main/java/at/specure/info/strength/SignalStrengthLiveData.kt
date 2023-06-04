/*
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

package at.specure.info.strength

import androidx.lifecycle.LiveData
import at.specure.info.network.DetailedNetworkInfo
import javax.inject.Inject

/**
 * LiveData that observes cellular signal strength changes
 */
class SignalStrengthLiveData @Inject constructor(private val signalStrengthWatcher: SignalStrengthWatcher) : LiveData<DetailedNetworkInfo?>(),
    SignalStrengthWatcher.SignalStrengthListener {

    override fun onActive() {
        super.onActive()
        signalStrengthWatcher.addListener(this)
    }

    override fun onInactive() {
        super.onInactive()
        signalStrengthWatcher.removeListener(this)
    }

    override fun onSignalStrengthChanged(signalInfo: DetailedNetworkInfo?) {
//        var message = ""
//        message = if (signalInfo == null) {
//            "SSP NOTIFY - SignalStrength: null"
//        } else {
//            "SSP NOTIFY - SignalStrength: value: ${signalInfo.signalStrengthInfo?.value} \nmax: ${signalInfo.signalStrengthInfo?.max} \nmin: ${signalInfo.signalStrengthInfo?.min} \nrsrq: ${signalInfo.signalStrengthInfo?.rsrq} \ntransportType: ${signalInfo.signalStrengthInfo?.transport?.name} \nsignal level: ${signalInfo.signalStrengthInfo?.signalLevel} \n "
//        }
//        Timber.v(message)
        postValue(signalInfo)
    }
}