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
import at.rtr.rmbt.android.ui.adapter.HistoryLoopAdapter
import at.rtr.rmbt.android.ui.adapter.NetworkInfoAdapter
import at.rtr.rmbt.android.util.listen
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.ActiveNetworkLiveData
import at.specure.info.network.WifiNetworkInfo
import at.specure.info.strength.SignalStrengthInfoLte
import at.specure.info.strength.SignalStrengthLiveData
import timber.log.Timber
import javax.inject.Inject

class NetworkInfoDialog : FullscreenDialog() {

    @Inject
    lateinit var activeNetworkLiveData: ActiveNetworkLiveData

    @Inject
    lateinit var signalStrengthLiveData: SignalStrengthLiveData

    private val adapter: NetworkInfoAdapter by lazy { NetworkInfoAdapter() }
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

        binding.recyclerViewCells.adapter = adapter

        signalStrengthLiveData.listen(this) { signal ->

            val networkInfo = signal?.networkInfo
            when (networkInfo) {
                is WifiNetworkInfo -> {
                    networkInfo.signal = signal.signalStrengthInfo?.value
                    adapter.items = listOf(networkInfo)
                }
                is CellNetworkInfo -> {
                    val cells = mutableListOf<CellNetworkInfo>()
                    if (signal.networkInfo != null && signal.networkInfo is CellNetworkInfo) {
                        cells.add(signal.networkInfo as CellNetworkInfo)
                    }
                    signal.secondaryActiveCellNetworks?.let {
                        Timber.d("Secondary Cell Count: ${it.size}")
                        it.forEach { cellNetworkInfo ->
                            if (cellNetworkInfo != null) {
                                cells.add(cellNetworkInfo)
                            }
                        }
                    }
                    signal.inactiveCellInfos?.let {
                        Timber.d("Inactive Cell Count: ${it.size}")
                        it.forEach { cellNetworkInfo ->
                            if (cellNetworkInfo != null) {
                                cells.add(cellNetworkInfo)
                            }
                        }
                    }
                    Timber.d("Total Cell Count: ${cells.size}")
                    adapter.items = cells
                }
            }
        }
    }

    companion object {

        fun show(manager: FragmentManager) {
            NetworkInfoDialog().show(manager, NetworkInfoDialog::class.java.name)
        }
    }
}