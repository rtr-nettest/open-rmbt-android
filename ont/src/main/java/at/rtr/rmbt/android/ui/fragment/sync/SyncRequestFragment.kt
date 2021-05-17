package at.rtr.rmbt.android.ui.fragment.sync

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentSyncRequestBinding
import at.rtr.rmbt.android.ui.fragment.BaseFragment
import at.rtr.rmbt.android.viewmodel.SyncDevicesViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

class SyncRequestFragment : BaseFragment() {

    override val layoutResId: Int
        get() = R.layout.fragment_sync_request

    private val activityViewModel: SyncDevicesViewModel by activityViewModels()
    private val binding: FragmentSyncRequestBinding by bindingLazy()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.state = activityViewModel.state

        activityViewModel.getSyncCode()

        activityViewModel.loadingChannel.receiveAsFlow().onEach {
            binding.loading = it
        }.launchIn(lifecycleScope)

    }

    companion object {
        fun newInstance() = SyncRequestFragment()
    }
}