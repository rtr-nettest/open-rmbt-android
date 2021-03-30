package at.rtr.rmbt.android.ui.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import at.rmbt.client.control.Server
import at.rtr.rmbt.android.BuildConfig
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentSettingsBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.activity.DataPrivacyAndTermsOfUseActivity
import at.rtr.rmbt.android.ui.activity.LoopInstructionsActivity
import at.rtr.rmbt.android.ui.dialog.InputSettingDialog
import at.rtr.rmbt.android.ui.dialog.OpenGpsSettingDialog
import at.rtr.rmbt.android.ui.dialog.OpenLocationPermissionDialog
import at.rtr.rmbt.android.ui.dialog.ServerSelectionDialog
import at.rtr.rmbt.android.ui.dialog.SimpleDialog
import at.rtr.rmbt.android.util.addOnPropertyChanged
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.SettingsViewModel
import at.specure.location.LocationState
import at.specure.util.copyToClipboard
import at.specure.util.openAppSettings
import timber.log.Timber
import java.util.Locale

class SettingsFragment : BaseFragment(), InputSettingDialog.Callback, ServerSelectionDialog.Callback, SimpleDialog.Callback {

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
                val intent = LoopInstructionsActivity.start(requireContext())
                startActivityForResult(intent, CODE_LOOP_INSTRUCTIONS)
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
            binding.clientUUID.value = if (it.isNullOrEmpty()) "" else "U$it"
        }

        settingsViewModel.locationStateLiveData.listen(this) {
            settingsViewModel.state.isLocationEnabled.set(it)
            settingsViewModel.state.canManageLocationSettings.set(it == LocationState.ENABLED)
            Timber.d("LocationStateLiveData Fragment  : $it")
        }

        binding.switchLocations.switchButton.isClickable = false
        binding.switchLocations.rootView.setOnClickListener {
            settingsViewModel.state.isLocationEnabled.get()?.let {
                when (it) {
                    LocationState.ENABLED -> requireContext().openAppSettings()
                    LocationState.DISABLED_APP -> OpenLocationPermissionDialog.instance().show(activity)
                    LocationState.DISABLED_DEVICE -> OpenGpsSettingDialog.instance().show(activity)
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

        binding.developerTag.frameLayoutRoot.setOnClickListener {

            InputSettingDialog.instance(
                getString(R.string.preferences_tag),
                binding.developerTag.value?.toString() ?: "", this,
                KEY_DEVELOPER_TAG_CODE,
                inputType = InputType.TYPE_CLASS_TEXT,
                isEmptyInputAllowed = true
            )
                .show(activity)
        }

        binding.version.value = "${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TIME})"
        binding.commitHash.value = BuildConfig.COMMIT_HASH
        binding.sourceCode.root.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(settingsViewModel.state.githubRepositoryUrl.get())))
        }
        binding.developedBy.root.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.preferences_developer_page))))
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
                DataPrivacyAndTermsOfUseActivity.start(
                    requireContext(),
                    when (Locale.getDefault().language) {
                        "de" -> String.format(url, "de")
                        else -> String.format(url, "en")
                    }
                )
            }
        }

        binding.radioInfo.root.setOnClickListener {
            SimpleDialog.Builder()
                .titleText(R.string.radio_info_warning_title)
                .messageText(R.string.radio_info_warning_message)
                .positiveText(R.string.accept_risk)
                .negativeText(android.R.string.cancel)
                .cancelable(false)
                .show(childFragmentManager, KEY_RADIO_INFO_CODE)
        }

        settingsViewModel.state.developerModeIsEnabled.addOnPropertyChanged {
            if (it.get() == false) {
                if (!settingsViewModel.isLoopModeWaitingTimeValid(
                        settingsViewModel.state.loopModeWaitingTimeMin.get() ?: settingsViewModel.state.appConfig.loopModeMinWaitingTimeMin,
                        settingsViewModel.state.appConfig.loopModeMaxWaitingTimeMin,
                        settingsViewModel.state.appConfig.loopModeMinWaitingTimeMin
                    )
                ) {
                    settingsViewModel.state.loopModeWaitingTimeMin.set(settingsViewModel.state.appConfig.loopModeMinWaitingTimeMin)
                }
                if (!settingsViewModel.isLoopModeDistanceMetersValid(
                        settingsViewModel.state.loopModeDistanceMeters.get() ?: settingsViewModel.state.appConfig.loopModeMinDistanceMeters,
                        settingsViewModel.state.appConfig.loopModeMinDistanceMeters,
                        settingsViewModel.state.appConfig.loopModeMaxDistanceMeters
                    )
                ) {
                    settingsViewModel.state.loopModeDistanceMeters.set(settingsViewModel.state.appConfig.loopModeMinDistanceMeters)
                }
                if (!settingsViewModel.isLoopModeNumberOfTestValid(
                        settingsViewModel.state.loopModeNumberOfTests.get(),
                        settingsViewModel.state.appConfig.loopModeMinTestsNumber,
                        settingsViewModel.state.appConfig.loopModeMaxTestsNumber
                    )
                ) {
                    settingsViewModel.state.loopModeNumberOfTests.set(settingsViewModel.state.appConfig.loopModeMinTestsNumber)
                }
            }
        }

        binding.userServerSelection.root.setOnClickListener {

            ServerSelectionDialog.instance(
                settingsViewModel.measurementServers.selectedMeasurementServer?.uuid,
                settingsViewModel.measurementServers.measurementServers ?: emptyList(),
                this
            )
                .show(activity)
        }
    }

    override fun onSelected(value: String, requestCode: Int) {
        when (requestCode) {
            KEY_REQUEST_CODE_LOOP_MODE_WAITING_TIME -> {
                if (!settingsViewModel.isLoopModeWaitingTimeValid(
                        value.toInt(),
                        settingsViewModel.state.appConfig.loopModeMinWaitingTimeMin,
                        settingsViewModel.state.appConfig.loopModeMaxWaitingTimeMin
                    ) && settingsViewModel.state.developerModeIsEnabled.get() != true
                ) {
                    SimpleDialog.Builder()
                        .messageText(
                            String.format(
                                getString(R.string.loop_mode_max_delay_invalid),
                                settingsViewModel.state.appConfig.loopModeMinWaitingTimeMin,
                                settingsViewModel.state.appConfig.loopModeMaxWaitingTimeMin
                            )
                        )
                        .positiveText(android.R.string.ok)
                        .cancelable(false)
                        .show(childFragmentManager, CODE_DIALOG_INVALID)
                }
            }
            KEY_REQUEST_CODE_LOOP_MODE_DISTANCE -> {
                if (!settingsViewModel.isLoopModeDistanceMetersValid(
                        value.toInt(),
                        settingsViewModel.state.appConfig.loopModeMinDistanceMeters,
                        settingsViewModel.state.appConfig.loopModeMaxDistanceMeters
                    ) && settingsViewModel.state.developerModeIsEnabled.get() != true
                ) {
                    SimpleDialog.Builder()
                        .messageText(
                            String.format(
                                getString(R.string.loop_mode_max_delay_invalid),
                                settingsViewModel.state.appConfig.loopModeMinDistanceMeters,
                                settingsViewModel.state.appConfig.loopModeMaxDistanceMeters
                            )
                        )
                        .positiveText(android.R.string.ok)
                        .cancelable(false)
                        .show(childFragmentManager, CODE_DIALOG_INVALID)
                }
            }
            KEY_REQUEST_CODE_ENTER_CODE -> {
                Toast.makeText(activity, getString(settingsViewModel.isCodeValid(value)), Toast.LENGTH_LONG).show()
            }
            KEY_DEVELOPER_CONTROL_SERVER_HOST_CODE -> {
                settingsViewModel.state.controlServerHost.set(value)
            }

            KEY_DEVELOPER_CONTROL_SERVER_PORT_CODE -> {
                settingsViewModel.state.controlServerPort.set(value)
            }
            KEY_DEVELOPER_MAP_SERVER_HOST_CODE -> {
                settingsViewModel.state.mapServerHost.set(value)
            }

            KEY_DEVELOPER_MAP_SERVER_PORT_CODE -> {
                settingsViewModel.state.mapServerPort.set(value.toInt())
            }

            KEY_DEVELOPER_TAG_CODE -> {
                if (value.isEmpty() || value.isBlank()) {
                    settingsViewModel.state.developerModeTag.set(null)
                } else {
                    settingsViewModel.state.developerModeTag.set(value)
                }
            }
        }
    }

    override fun onSelectServer(server: Server) {

        if (server.uuid.equals("default")) {
            settingsViewModel.state.selectedMeasurementServer.set(null)
        } else {
            settingsViewModel.state.selectedMeasurementServer.set(server)
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
        private const val KEY_RADIO_INFO_CODE: Int = 8
        private const val KEY_DEVELOPER_TAG_CODE: Int = 9

        private const val CODE_LOOP_INSTRUCTIONS = 13
        private const val CODE_DIALOG_INVALID = 14

        fun newInstance() = SettingsFragment()
    }

    override fun onDialogPositiveClicked(code: Int) {
        when (code) {
            KEY_RADIO_INFO_CODE -> {
                val i = Intent(Intent.ACTION_VIEW)
                i.setClassName("com.android.settings", "com.android.settings.RadioInfo")
                try {
                    startActivity(i)
                } catch (e: ActivityNotFoundException) {
                    showNotSupportedToast()
                } catch (e: SecurityException) {
                    showNotSupportedToast()
                }
            }
        }
    }

    override fun onDialogNegativeClicked(code: Int) {
        // not needed
    }

    private fun showNotSupportedToast() {
        Toast.makeText(activity, R.string.preferences_connection_details_not_supported, Toast.LENGTH_SHORT).show()
    }
}