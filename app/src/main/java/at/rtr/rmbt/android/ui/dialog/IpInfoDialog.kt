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

package at.rtr.rmbt.android.ui.dialog

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import at.rmbt.client.control.IpProtocol
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogIpInfoBinding
import at.rtr.rmbt.android.di.Injector
import at.rtr.rmbt.android.util.args
import at.rtr.rmbt.android.util.listen
import at.specure.info.ip.IpV4ChangeLiveData
import at.specure.info.ip.IpV6ChangeLiveData
import javax.inject.Inject
import kotlin.math.max

class IpInfoDialog : FullscreenDialog() {

    private lateinit var binding: DialogIpInfoBinding

    @Inject
    lateinit var ipV4InfoLiveData: IpV4ChangeLiveData

    @Inject
    lateinit var ipV6InfoLiveData: IpV6ChangeLiveData

    private lateinit var protocol: IpProtocol

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Injector.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_ip_info, container, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
                val insetsSystemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                val insetsDisplayCutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
                val topSafe = max(insetsSystemBars.top, insetsDisplayCutout.top)
                val leftSafe = max(insetsSystemBars.left, insetsDisplayCutout.left)
                val rightSafe = max(insetsSystemBars.right, insetsDisplayCutout.right)
                val bottomSafe = max(insetsSystemBars.bottom, insetsDisplayCutout.bottom)

                v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    rightMargin = rightSafe
                    leftMargin = leftSafe
                    topMargin = topSafe
                    bottomMargin = bottomSafe
                }
                WindowInsetsCompat.CONSUMED
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.iconClose.setOnClickListener {
            this@IpInfoDialog.dismiss()
        }

        protocol = arguments?.getSerializable(KEY_IP_PROTOCOL) as IpProtocol

        when (protocol) {

            IpProtocol.V4 -> {
                ipV4InfoLiveData.listen(this) {
                    binding.ipInfo = it
                }
            }
            IpProtocol.V6 -> {
                ipV6InfoLiveData.listen(this) {
                    binding.ipInfo = it
                }
            }
        }
    }

    companion object {

        private const val KEY_IP_PROTOCOL: String = "key_ip_protocol"

        fun instance(protocol: IpProtocol): FullscreenDialog = IpInfoDialog().args {
            putSerializable(KEY_IP_PROTOCOL, protocol)
        } as FullscreenDialog
    }
}