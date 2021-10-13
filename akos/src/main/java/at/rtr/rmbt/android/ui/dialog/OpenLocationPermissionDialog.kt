package at.rtr.rmbt.android.ui.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import at.rtr.rmbt.android.databinding.DialogOpenLocationPermissionBinding
import at.specure.util.openAppSettings

class OpenLocationPermissionDialog : FullscreenDialog() {

    private lateinit var binding: DialogOpenLocationPermissionBinding

    override val gravity = Gravity.CENTER

    override val dimBackground = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, at.rtr.rmbt.android.R.layout.dialog_open_location_permission, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonOpenLocationPermissionSetting.setOnClickListener {
            requireContext().openAppSettings()
            dismissAllowingStateLoss()
        }
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
    }

    companion object {

        fun instance(): FullscreenDialog = OpenLocationPermissionDialog()
    }
}