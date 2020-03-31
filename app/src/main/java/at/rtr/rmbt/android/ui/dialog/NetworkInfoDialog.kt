package at.rtr.rmbt.android.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogNetworkInfoBindingImpl
import at.rtr.rmbt.android.di.Injector
import at.rtr.rmbt.android.util.listen
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.ActiveNetworkLiveData
import at.specure.info.network.WifiNetworkInfo
import at.specure.info.strength.SignalStrengthInfoLte
import at.specure.info.strength.SignalStrengthLiveData
import javax.inject.Inject

class NetworkInfoDialog : FullscreenDialog() {

    @Inject
    lateinit var activeNetworkLiveData: ActiveNetworkLiveData

    @Inject
    lateinit var signalStrengthLiveData: SignalStrengthLiveData

    private lateinit var binding: DialogNetworkInfoBindingImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Injector.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_network_info, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activeNetworkLiveData.listen(this) { info ->
            binding.info = info

            if (info is CellNetworkInfo && info.band != null) {
                val band = info.band!!
                binding.channelNumber = band.channel.toString()
                binding.labelChannelNumber.text = band.channelAttribution.name

                binding.channelName = "${band.band} (${band.name})"
            } else if (info is WifiNetworkInfo) {
                val band = info.band
                binding.channelNumber = "${band.channelNumber} (${band.frequency} MHz)"
                binding.labelChannelNumber.text = getString(R.string.dialog_signal_info_channel)

                binding.channelName = band.informalName
            } else {
                binding.channelNumber = null
            }
        }

        signalStrengthLiveData.listen(this) { signal ->
            binding.signal = signal

            if (signal is SignalStrengthInfoLte && signal.timingAdvance != null) {
                val ta = signal.timingAdvance!!
                binding.timingAdvance = "$ta ~(${ta * 78} m)"
            } else {
                binding.timingAdvance = null
            }

            if (signal?.rsrq == null) {
                binding.quality = null
            } else {
                binding.quality = "${signal.rsrq} dB"
            }
        }
    }

    companion object {

        fun show(manager: FragmentManager) {
            NetworkInfoDialog().show(manager, NetworkInfoDialog::class.java.name)
        }
    }
}