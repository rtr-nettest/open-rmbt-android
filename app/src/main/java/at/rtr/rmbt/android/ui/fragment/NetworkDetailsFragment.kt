package at.rtr.rmbt.android.ui.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentNetworkDetailsBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.activity.SignalMeasurementActivity
import at.rtr.rmbt.android.ui.viewstate.NetworkDetailsViewState
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.NetworkDetailsViewModel

class NetworkDetailsFragment : BaseFragment() {

    private val viewModel: NetworkDetailsViewModel by viewModelLazy()
    private val binding: FragmentNetworkDetailsBinding by bindingLazy()

    override val layoutResId = R.layout.fragment_network_details

    private val state: NetworkDetailsViewState
        get() = viewModel.state

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.state = state

        viewModel.connectivityInfoLiveData.listen(this) {
            state.setConnectivityInfo(it)
        }

        viewModel.activeNetworkLiveData.listen(this) {
            state.setActiveNetworkInfo(it)
        }

        viewModel.signalStrengthLiveData.listen(this) {
            state.setSignalStrengthInfo(it)
        }

        viewModel.ipV4ChangeLiveData.listen(this) {
            state.setIpInfo(it)
        }

        viewModel.ipV6ChangeLiveData.listen(this) {
            state.setIpInfo(it)
        }

        viewModel.locationProducer.liveData.listen(this) {
            state.setLocationInfo(it)
        }

        binding.buttonSignalMeasurement.setOnClickListener {
            startActivity(Intent(requireContext(), SignalMeasurementActivity::class.java))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        viewModel.permissionsWatcher.notifyPermissionsUpdated()
    }

    override fun onStart() {
        super.onStart()
        if (viewModel.permissionsWatcher.requiredPermissions.isNotEmpty()) { // for development, dont follow this style
            requestPermissions(viewModel.permissionsWatcher.requiredPermissions, 10)
        }
    }
}