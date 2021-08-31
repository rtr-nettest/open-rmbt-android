package at.rtr.rmbt.android.ui.dialog

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogOpenGpsSettingBinding

class OpenGpsSettingDialog : FullscreenDialog() {

    private lateinit var binding: DialogOpenGpsSettingBinding

    override val gravity = Gravity.CENTER

    override val dimBackground = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_open_gps_setting, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonOpenGpsSetting.setOnClickListener {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            dismissAllowingStateLoss()
        }
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
    }

    companion object {

        fun instance(): FullscreenDialog = OpenGpsSettingDialog()
    }
}