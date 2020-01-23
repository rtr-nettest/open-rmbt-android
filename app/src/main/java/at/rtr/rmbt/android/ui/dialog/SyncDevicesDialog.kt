package at.rtr.rmbt.android.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogSyncDevicesBinding
import at.rtr.rmbt.android.di.Injector
import at.rtr.rmbt.android.viewmodel.SyncDevicesViewModel
import javax.inject.Inject

class SyncDevicesDialog : FullscreenDialog() {

    @Inject
    lateinit var viewModel: SyncDevicesViewModel

    private lateinit var binding: DialogSyncDevicesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Injector.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_sync_devices, container, false)
        binding.state = viewModel.state
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonOpenGpsSetting.setOnClickListener {
            viewModel.getSyncCode()
        }
    }

    companion object {

        fun show(fragmentManager: FragmentManager) {
            with(fragmentManager.beginTransaction()) {
                val tag = SyncDevicesDialog::class.java.name
                val prev = fragmentManager.findFragmentByTag(SyncDevicesDialog::class.java.name)
                if (prev != null) {
                    remove(prev)
                }
                addToBackStack(null)
                SyncDevicesDialog().show(fragmentManager, tag)
            }
        }
    }
}