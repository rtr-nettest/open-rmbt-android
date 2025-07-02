package at.rtr.rmbt.android.ui.dialog

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import at.rmbt.client.control.getCurrentDataSubscriptionId
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogNetworkInfoBindingImpl
import at.rtr.rmbt.android.di.Injector
import at.rtr.rmbt.android.ui.adapter.ICellAdapter
import at.rtr.rmbt.android.ui.adapter.NetworkInfoAdapter
import at.rtr.rmbt.android.util.listen
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.ActiveNetworkLiveData
import at.specure.info.network.WifiNetworkInfo
import at.specure.info.strength.SignalStrengthLiveData
import cz.mroczis.netmonster.core.factory.NetMonsterFactory
import cz.mroczis.netmonster.core.model.cell.ICell
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.max

class NetworkInfoDialog : FullscreenDialog() {

    @Inject
    lateinit var activeNetworkLiveData: ActiveNetworkLiveData

    @Inject
    lateinit var signalStrengthLiveData: SignalStrengthLiveData

    private val adapterWifi: NetworkInfoAdapter by lazy { NetworkInfoAdapter() }
    private val adapterMobile: ICellAdapter by lazy { ICellAdapter() }
    private lateinit var binding: DialogNetworkInfoBindingImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Injector.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_network_info, container, false)
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
            this.dismiss()
        }

        signalStrengthLiveData.listen(this) { signal ->


            val networkInfo = signal?.networkInfo
            when (networkInfo) {
                is WifiNetworkInfo -> {
                    if (binding.recyclerViewCells.adapter is NetworkInfoAdapter == false) {
                        binding.recyclerViewCells.adapter = adapterWifi
                    }
                    networkInfo.signal = signal.signalStrengthInfo?.value
                    adapterWifi.items = listOf(networkInfo)
                }
                is CellNetworkInfo -> {

                    var subscriptionManager = context?.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                    var primaryDataSubscription = subscriptionManager.getCurrentDataSubscriptionId()

//                   // add debug logging to NetworkInfo (aka Cell info page)
//                    context?.let {
//                        val netmonster = NetMonsterFactory.get(it)
//                        val networkType = netmonster.getNetworkType(primaryDataSubscription)
//                        binding.networktype.setText("SubID: $primaryDataSubscription Type: $networkType")
//                    }

                    if (binding.recyclerViewCells.adapter is ICellAdapter == false) {
                        binding.recyclerViewCells.adapter = adapterMobile.also {
                            it.setHasStableIds(false)
                        }
                    }
                    val cells = mutableListOf<ICell>()
//                    if (signal.networkInfo != null && signal.networkInfo is CellNetworkInfo) {
//                        cells.add(signal.networkInfo as CellNetworkInfo)
//                    }
//                    signal.secondaryActiveCellNetworks?.let {
//                        Timber.d("Secondary Cell Count: ${it.size}")
//                        it.forEach { cellNetworkInfo ->
//                            if (cellNetworkInfo != null) {
//                                cells.add(cellNetworkInfo)
//                            }
//                        }
//                    }
                    signal.allCellInfos?.let {
                        Timber.d("Inactive Cell Count: ${it.size}")
//                        it.forEach { cellNetworkInfo ->
//                            if (cellNetworkInfo != null) {
//                                cells.add(cellNetworkInfo)
//                            }
//                        }
                        Timber.d("Total Cell Count: ${cells.size}")
                        adapterMobile.primaryDataSubscriptionId = primaryDataSubscription
                        adapterMobile.items = it
                    }
//                    Timber.d("Total Cell Count: ${cells.size}")
//                    adapterMobile.items = cells
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