package at.rtr.rmbt.android.ui.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import at.rtr.rmbt.android.BuildConfig
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentSettingsBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.activity.LoopInstructionsActivity
import at.rtr.rmbt.android.ui.activity.DataPrivacyAndTermsOfUseActivity
import at.rtr.rmbt.android.ui.dialog.Dialogs
import at.rtr.rmbt.android.ui.dialog.InputSettingDialog
import at.rtr.rmbt.android.ui.dialog.OpenGpsSettingDialog
import at.rtr.rmbt.android.ui.dialog.OpenLocationPermissionDialog
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.SettingsViewModel
import at.specure.location.LocationProviderState
import at.specure.util.copyToClipboard
import at.specure.util.openAppSettings
import timber.log.Timber

class SettingsFragment : BaseFragment(), InputSettingDialog.Callback {

    private val settingsViewModel: SettingsViewModel by viewModelLazy()
    private val binding: FragmentSettingsBinding by bindingLazy()

    override val layoutResId = R.layout.fragment_settings

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.state = settingsViewModel.state

        binding.loopModeWaitingTime.frameLayoutRoot.setOnClickListener {

            InputSettingDialog.instance(
                getString(R.string.preferences_loop_mode_min_delay),
                binding.loopModeWaitingTime.value.toString(), this,
                KEY_REQUEST_CODE_LOOP_MODE_WAITING_TIME
            )
                .show(activity)
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE)
        }

        binding.loopModeDistanceMeters.frameLayoutRoot.setOnClickListener {

            InputSettingDialog.instance(
                getString(R.string.preferences_loop_mode_max_movement),
                binding.loopModeDistanceMeters.value.toString(), this,
                KEY_REQUEST_CODE_LOOP_MODE_DISTANCE
            )
                .show(activity)
        }

        binding.switchLoopModeEnabled.switchButton.isClickable = false
        binding.switchLoopModeEnabled.rootView.setOnClickListener {

            if (!binding.switchLoopModeEnabled.switchButton.isChecked) {
                LoopInstructionsActivity.start(this, CODE_LOOP_INSTRUCTIONS)
            } else {
                settingsViewModel.state.loopModeEnabled.set(false)
            }
        }

        binding.clientUUID.frameLayoutRootKeyValue.setOnClickListener {
            val clientUUIDExists = !binding.clientUUID.value.isNullOrEmpty()
            if (clientUUIDExists) {
                context?.copyToClipboard(binding.clientUUID.value)
                Toast.makeText(context, R.string.about_client_uuid_copied, Toast.LENGTH_SHORT).show()
            }
        }

        settingsViewModel.state.clientUUID.liveData.listen(this) {
            binding.clientUUID.value = it
        }

        settingsViewModel.locationStateLiveData.listen(this) {
            settingsViewModel.state.isLocationEnabled.set(it)
            settingsViewModel.state.canManageLocationSettings.set(it == LocationProviderState.ENABLED)
            Timber.d("LocationStateLiveData Fragment  : $it")
        }

        binding.switchLocations.switchButton.isClickable = false
        binding.switchLocations.rootView.setOnClickListener {
            settingsViewModel.state.isLocationEnabled.get()?.let {
                when (it) {
                    LocationProviderState.ENABLED -> requireContext().openAppSettings()
                    LocationProviderState.DISABLED_APP -> OpenLocationPermissionDialog.instance().show(activity)
                    LocationProviderState.DISABLED_DEVICE -> OpenGpsSettingDialog.instance().show(activity)
                }
            }
        }

        binding.version.frameLayoutRootKeyValue.setOnClickListener {
            settingsViewModel.onVersionClicked()
        }

        settingsViewModel.openCodeWindow.listen(this) {
            if (it) {

                InputSettingDialog.instance(
                    getString(R.string.preferences_enter_code), "", this,
                    KEY_REQUEST_CODE_ENTER_CODE, isCancelable = false
                ).show(activity)
            }
        }

        binding.developerControlServerHost.frameLayoutRoot.setOnClickListener {

            InputSettingDialog.instance(
                getString(R.string.preferences_override_control_server_host),
                binding.developerControlServerHost.value.toString(), this,
                KEY_DEVELOPER_CONTROL_SERVER_HOST_CODE,
                inputType = InputType.TYPE_CLASS_TEXT
            )
                .show(activity)
        }

        binding.developerControlServerPort.frameLayoutRoot.setOnClickListener {

            InputSettingDialog.instance(
                getString(R.string.preferences_override_control_server_port),
                binding.developerControlServerPort.value.toString(), this,
                KEY_DEVELOPER_CONTROL_SERVER_PORT_CODE
            )
                .show(activity)
        }

        binding.developerMapServerHost.frameLayoutRoot.setOnClickListener {

            InputSettingDialog.instance(
                getString(R.string.preferences_developer_map_host),
                binding.developerMapServerHost.value.toString(), this,
                KEY_DEVELOPER_MAP_SERVER_HOST_CODE,
                inputType = InputType.TYPE_CLASS_TEXT
            )
                .show(activity)
        }

        binding.developerMapServerPort.frameLayoutRoot.setOnClickListener {

            InputSettingDialog.instance(
                getString(R.string.preferences_developer_map_port),
                binding.developerMapServerPort.value.toString(), this,
                KEY_DEVELOPER_MAP_SERVER_PORT_CODE
            )
                .show(activity)
        }

        binding.version.value = BuildConfig.VERSION_NAME
        binding.commitHash.value = BuildConfig.COMMIT_HASH
        binding.sourceCode.root.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(settingsViewModel.state.githubRepositoryUrl.get())))
        }
        binding.goToWebsite.root.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(settingsViewModel.state.webPageUrl.get())))
        }
        binding.contactUs.root.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SEND)
            emailIntent.type = "plain/text"
            emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(settingsViewModel.state.emailAddress.get()))
            emailIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                "${getString(R.string.about_email_subject)}  ${getString(R.string.app_name)}  ${BuildConfig.VERSION_NAME}"
            )
            emailIntent.putExtra(Intent.EXTRA_TEXT, "") // to navigate cursor directly to the message body
            startActivity(Intent.createChooser(emailIntent, getString(R.string.about_email_sending)))
        }

        binding.dataPrivacyAndTerms.root.setOnClickListener {
            settingsViewModel.state.dataPrivacyAndTermsUrl.get()?.let { url ->
                DataPrivacyAndTermsOfUseActivity.start(requireContext(),
                    url
                )
            }
        }
    }

    override fun onSelected(value: String, requestCode: Int) {
        when (requestCode) {
            KEY_REQUEST_CODE_LOOP_MODE_WAITING_TIME -> {
                if (!settingsViewModel.isLoopModeWaitingTimeValid(value.toInt(), MIN_LOOP_MODE_WAITING_TIME, MAX_LOOP_MODE_WAITING_TIME)) {
                    activity?.let { it1 ->
                        Dialogs.show(
                            it1,
                            getString(R.string.value_invalid),
                            String.format(getString(R.string.loop_mode_max_delay_invalid), MIN_LOOP_MODE_WAITING_TIME, MAX_LOOP_MODE_WAITING_TIME)
                        )
                    }
                }
            }
            KEY_REQUEST_CODE_LOOP_MODE_DISTANCE -> {
                if (!settingsViewModel.isLoopModeDistanceMetersValid(value.toInt(), MIN_LOOP_MODE_DISTANCE, MAX_LOOP_MODE_DISTANCE)) {
                    activity?.let { it1 ->
                        Dialogs.show(
                            it1, getString(R.string.value_invalid),
                            String.format(getString(R.string.loop_mode_max_movement_invalid), MIN_LOOP_MODE_DISTANCE, MAX_LOOP_MODE_DISTANCE)
                        )
                    }
                }
            }
            KEY_REQUEST_CODE_ENTER_CODE -> {
                Toast.makeText(activity, getString(settingsViewModel.isCodeValid(value)), Toast.LENGTH_LONG).show()
            }
            KEY_DEVELOPER_CONTROL_SERVER_HOST_CODE -> {
                settingsViewModel.state.controlServerHost.set(value)
            }

            KEY_DEVELOPER_CONTROL_SERVER_PORT_CODE -> {
                settingsViewModel.state.controlServerPort.set(value.toInt())
            }
            KEY_DEVELOPER_MAP_SERVER_HOST_CODE -> {
                settingsViewModel.state.mapServerHost.set(value)
            }

            KEY_DEVELOPER_MAP_SERVER_PORT_CODE -> {
                settingsViewModel.state.mapServerPort.set(value.toInt())
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CODE_LOOP_INSTRUCTIONS) {
            if (resultCode == Activity.RESULT_OK) {
                settingsViewModel.state.loopModeEnabled.set(true)
            } else {
                settingsViewModel.state.loopModeEnabled.set(false)
            }
        }
    }

    companion object {
        private const val KEY_REQUEST_CODE_LOOP_MODE_WAITING_TIME: Int = 1
        private const val KEY_REQUEST_CODE_LOOP_MODE_DISTANCE: Int = 2
        private const val KEY_REQUEST_CODE_ENTER_CODE: Int = 3
        private const val KEY_DEVELOPER_CONTROL_SERVER_HOST_CODE: Int = 4
        private const val KEY_DEVELOPER_CONTROL_SERVER_PORT_CODE: Int = 5
        private const val KEY_DEVELOPER_MAP_SERVER_HOST_CODE: Int = 6
        private const val KEY_DEVELOPER_MAP_SERVER_PORT_CODE: Int = 7

        private const val MIN_LOOP_MODE_WAITING_TIME: Int = 15
        private const val MAX_LOOP_MODE_WAITING_TIME: Int = 1440
        private const val MIN_LOOP_MODE_DISTANCE: Int = 50
        private const val MAX_LOOP_MODE_DISTANCE: Int = 10000

        private const val CODE_LOOP_INSTRUCTIONS = 13

        fun newInstance() = SettingsFragment()
    }
}