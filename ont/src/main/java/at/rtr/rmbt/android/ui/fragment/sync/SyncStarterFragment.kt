package at.rtr.rmbt.android.ui.fragment.sync

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentSyncStarterBinding
import at.rtr.rmbt.android.ui.fragment.BaseFragment
import at.rtr.rmbt.android.viewmodel.SyncDevicesViewModel

class SyncStarterFragment : BaseFragment() {

    override val layoutResId: Int
        get() = R.layout.fragment_sync_starter

    private val activityViewModel: SyncDevicesViewModel by activityViewModels()
    private val binding: FragmentSyncStarterBinding by bindingLazy()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonRequestCode.setOnClickListener {
            activityViewModel.showRequest()
        }

        binding.buttonEnterCode.setOnClickListener {
            activityViewModel.showEnter()
        }
    }

    companion object {
        fun newInstance() = SyncStarterFragment()
    }
}