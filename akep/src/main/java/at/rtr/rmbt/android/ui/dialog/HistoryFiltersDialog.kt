package at.rtr.rmbt.android.ui.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogFiltersHistoryBinding
import at.rtr.rmbt.android.di.Injector
import at.rtr.rmbt.android.viewmodel.ResultListFiltersViewModel
import javax.inject.Inject

class HistoryFiltersDialog : FullscreenDialog(), HistoryFiltersConfirmationDialog.Callback {

    override val gravity: Int = Gravity.BOTTOM

    override val dimBackground: Boolean = false

    @Inject
    lateinit var viewModel: ResultListFiltersViewModel

    private lateinit var binding: DialogFiltersHistoryBinding

    init {
        retainInstance = true
    }

    private val callback: Callback?
        get() = when {
            targetFragment is Callback -> targetFragment as Callback
            activity is Callback -> activity as Callback
            else -> null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Injector.inject(this)
        viewModel.onRestoreState(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_filters_history, container, false)
        binding.state = viewModel.state
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.devices.setOnClickListener {
            val state = viewModel.state
            HistoryFiltersConfirmationDialog.instance(
                this,
                CODE_DEVICES,
                getString(R.string.text_filter_devices),
                state.defaultDevices,
                state.activeDevices
            )
                .show(parentFragmentManager)
        }

        binding.networks.setOnClickListener {
            val state = viewModel.state
            HistoryFiltersConfirmationDialog.instance(
                this,
                CODE_NETWORK,
                getString(R.string.text_filter_networks),
                state.defaultNetwors,
                state.activeNetworks
            )
                .show(parentFragmentManager)
        }

//        viewModel.devicesLiveData.listen(this) {
//            viewModel.state.defaultDevices = it
//        }

//        viewModel.activeDevicesLiveData.listen(this) {
//            viewModel.state.run {
//                activeDevices = it
//                val displayString = viewModel.displayStringSet(it, defaultDevices)
//                devices.set(displayString)
//            }
//        }

//        viewModel.networksLiveData.listen(this) {
//            viewModel.state.defaultNetwors = it
//        }

//        viewModel.activeNetworksLiveData.listen(this) {
//            viewModel.state.run {
//                activeNetworks = it
//                val displayString = viewModel.displayStringSet(it, defaultNetwors)
//                networks.set(displayString)
//            }
//        }

        binding.iconClose.setOnClickListener { dismiss() }
    }

    override fun onOptionSelected(code: Int, selected: Set<String>) {
        when (code) {
            CODE_NETWORK -> viewModel.updateNetworkFilters(selected)
            CODE_DEVICES -> viewModel.updateDeviceFilters(selected)
        }
        callback?.onFiltersUpdated()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.onSaveState(outState)
    }

    companion object {

        const val CODE_DEVICES = 0
        const val CODE_NETWORK = 1

        fun instance(fragment: Fragment, requestCode: Int): FullscreenDialog =
            HistoryFiltersDialog().apply { setTargetFragment(fragment, requestCode) }
    }

    interface Callback {
        fun onFiltersUpdated()
    }
}