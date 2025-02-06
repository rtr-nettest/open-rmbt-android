package at.rtr.rmbt.android.ui.dialog

import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogSyncDevicesBinding
import at.rtr.rmbt.android.di.Injector
import at.rtr.rmbt.android.ui.viewstate.SyncDeviceViewState
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.util.onDone
import at.rtr.rmbt.android.util.onTextChanged
import at.rtr.rmbt.android.util.showKeyboard
import at.rtr.rmbt.android.viewmodel.SyncDevicesViewModel
import at.specure.util.copyToClipboard
import at.specure.util.toast
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CODE_LENGTH = 12

class SyncDevicesDialog : FullscreenDialog() {

    @Inject
    lateinit var viewModel: SyncDevicesViewModel

    private lateinit var binding: DialogSyncDevicesBinding

    override val gravity: Int
        get() = Gravity.CENTER

    override val dimBackground: Boolean
        get() = false

    private var progressDialog: ProgressDialogFragment? = null

    private val callback: Callback?
        get() = when {
            parentFragment is Callback -> parentFragment as Callback
            activity is Callback -> activity as Callback
            else -> null
        }

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

        binding.iconClose.setOnClickListener {
            this@SyncDevicesDialog.dismiss()
        }

        binding.buttonRequestCode.setOnClickListener {
            viewModel.getSyncCode()
        }

        binding.buttonEnterCode.setOnClickListener {
            if (viewModel.state.visibilityState.get() == SyncDeviceViewState.VisibilityState.ENTER_CODE) {
                onCodeEntered()
            } else {
                viewModel.state.visibilityState.set(SyncDeviceViewState.VisibilityState.ENTER_CODE)
                Handler().postDelayed({ binding.editCode.showKeyboard() }, 200)
            }
        }

        binding.buttonClose.setOnClickListener {
            dismissAllowingStateLoss()
        }

        binding.textSyncCode.setOnClickListener {
            viewModel.state.currentDeviceSyncCode.get()?.let {
                requireContext().copyToClipboard(it)
                requireContext().toast(R.string.toast_sync_code_copied_to_clipboard)
            }
        }

        binding.editCode.onDone {
            onCodeEntered()
        }

        launch(CoroutineName("syncDevicesDialog")) {
            binding.editCode.onTextChanged()
                .collect {
                    binding.inputCode.error = null
                }
        }

        viewModel.loadingLiveData.listen(this) { isLoading ->
            if (isLoading) {
                showProgressDialog()
            } else {
                hideProgressDialog()
            }
        }

        viewModel.getSyncCodeLiveData.listen(this) { syncCode ->
            viewModel.state.currentDeviceSyncCode.set(syncCode)
            viewModel.state.visibilityState.set(SyncDeviceViewState.VisibilityState.SHOW_CODE)
        }

        viewModel.syncDevicesLiveData.listen(this) {
            if (it.success) {
                viewModel.state.run {
                    visibilityState.set(SyncDeviceViewState.VisibilityState.SYNC_SUCCESS)
                    syncedTitle.set(it.dialogTitle)
                    syncedText.set(it.dialogText)
                }
                callback?.onDevicesSynced()
            } else {
                binding.inputCode.error = it.dialogText
            }
        }

        viewModel.errorLiveData.listen(this) {
            it?.let {
                SimpleDialog.Builder()
                    .messageText(it.getText(requireContext()))
                    .positiveText(R.string.button_close)
                    .show(parentFragmentManager, 0)
            }
        }
    }

    private fun onCodeEntered() {
        if (binding.editCode.text?.length == CODE_LENGTH) {
            binding.inputCode.error = null
            viewModel.syncDevices()
        } else {
            binding.inputCode.error = requireContext().getString(R.string.error_sync_code_short)
        }
    }

    private fun showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = ProgressDialogFragment()
            progressDialog?.show(childFragmentManager)
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismissAllowingStateLoss()
        progressDialog = null
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

    interface Callback {

        fun onDevicesSynced()
    }
}