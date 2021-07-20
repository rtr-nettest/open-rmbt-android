package at.rtr.rmbt.android.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.View
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentHomeBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.activity.MeasurementActivity
import at.rtr.rmbt.android.ui.activity.PermissionsExplanationActivity
import at.rtr.rmbt.android.ui.activity.PreferenceActivity
import at.rtr.rmbt.android.ui.dialog.MessageDialog
import at.rtr.rmbt.android.ui.dialog.SimpleDialog
import at.rtr.rmbt.android.util.InfoWindowStatus
import at.rtr.rmbt.android.util.InformationAccessProblem
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.addOnPropertyChanged
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.util.setTechnologyIcon
import at.rtr.rmbt.android.viewmodel.HomeViewModel
import at.specure.location.LocationState
import at.specure.measurement.MeasurementService
import at.specure.util.hasPermission
import at.specure.util.openAppSettings
import at.specure.util.toast

class HomeFragment : BaseFragment() {

    private val homeViewModel: HomeViewModel by viewModelLazy()
    private val binding: FragmentHomeBinding by bindingLazy()

    private var callback: NetworkInfoCallback? = null

    override val layoutResId = R.layout.fragment_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (activity is NetworkInfoCallback) {
            callback = activity as NetworkInfoCallback
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.state = homeViewModel.state

        homeViewModel.isConnected.listen(this) {
            activity?.window?.changeStatusBarColor(ToolbarTheme.WHITE)
        }

        homeViewModel.signalStrengthLiveData.listen(this) {
            homeViewModel.state.signalStrength.set(it?.signalStrengthInfo)
            homeViewModel.state.activeNetworkInfo.set(it?.networkInfo)

            it?.networkInfo?.let { info ->
                binding.panelNetworkDetails.textNetworkName.text = info.name
                binding.panelNetworkDetails.textNetworkType.setTechnologyIcon(info)
            }
        }

        homeViewModel.locationStateLiveData.listen(this) {
            homeViewModel.state.isLocationEnabled.set(it)
            checkInformationAvailability()
        }

        homeViewModel.ipV4ChangeLiveData.listen(this) {
            homeViewModel.state.ipV4Info.set(it)
        }

        homeViewModel.ipV6ChangeLiveData.listen(this) {
            homeViewModel.state.ipV6Info.set(it)
        }

        binding.ivSignalLevel.setOnClickListener {
            if (homeViewModel.isConnected.value == true) {
                if (!homeViewModel.clientUUID.value.isNullOrEmpty()) {
                    MeasurementService.startTests(requireContext())
                    MeasurementActivity.start(requireContext())
                } else {
                    MessageDialog.instance(R.string.client_not_registered).show(activity)
                }
            } else {
                MessageDialog.instance(R.string.home_no_internet_connection).show(activity)
            }
        }

        binding.panelNetworkDetails.root.setOnClickListener {
            callback?.showNetworkInfo()
        }

        binding.buttonLoop.setOnClickListener {
            if (this.isResumed) {
                if (homeViewModel.state.isLoopModeActive.get()) {
                    homeViewModel.state.isLoopModeActive.set(false)
                } else {
                    homeViewModel.state.isLoopModeActive.set(true)
                }
            }
            checkInformationAvailability()
        }
        binding.logo.setOnClickListener {
            PermissionsExplanationActivity.start(this)
        }

        binding.buttonSettings.setOnClickListener {
            startActivity(Intent(requireContext(), PreferenceActivity::class.java))
        }

        homeViewModel.activeSignalMeasurementLiveData.listen(this) {
            homeViewModel.state.isSignalMeasurementActive.set(it)
            checkInformationAvailability()
        }

        homeViewModel.newsLiveData.listen(this) {
            it?.forEach { newItem ->
                val latestNewsShown: Long = homeViewModel.getLatestNewsShown() ?: -1
                newItem.text?.let { text ->
                    newItem.title?.let { title ->
                        if (newItem.uid ?: -1 > latestNewsShown) {
                            SimpleDialog.Builder()
                                .messageText(text)
                                .titleText(title)
                                .positiveText(android.R.string.ok)
                                .cancelable(false)
                                .show(this.childFragmentManager, CODE_DIALOG_NEWS)
                        }
                    }
                }
                homeViewModel.setNewsShown(newItem)
            }
        }
        binding.panelPermissionsProblems.drawableHelp.setOnClickListener {
            PermissionsExplanationActivity.start(this)
        }

        homeViewModel.state.informationAccessProblem.addOnPropertyChanged { problem ->
            problem.get()?.let { updateProblemUI(it) }
        }
    }

    private fun updateProblemUI(problem: InformationAccessProblem) {
        if (this.isAdded && problem != InformationAccessProblem.NO_PROBLEM) {
            binding.panelPermissionsProblems.labelPermissionDisabled.text = problem.let {
                this.getText(
                    it.titleID
                )
            }
            binding.panelPermissionsProblems.textPermissionExplanation.text =
                problem.let {
                    this.getText(
                        it.descriptionId
                    )
                }
        }
        when (problem) {
            InformationAccessProblem.MISSING_LOCATION_ENABLED -> {
                binding.panelPermissionsProblems.cardPP.setOnClickListener {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            }
            InformationAccessProblem.MISSING_LOCATION_PERMISSION,
            InformationAccessProblem.MISSING_READ_PHONE_STATE_PERMISSION,
            InformationAccessProblem.MISSING_BACKGROUND_LOCATION_PERMISSION -> {
                binding.panelPermissionsProblems.cardPP.setOnClickListener {
                    requireContext().openAppSettings()
                }
            }
            InformationAccessProblem.NO_PROBLEM -> {
                // do nothing
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkBasicNetworkInfoVisibility()
        homeViewModel.signalStrengthLiveData.listen(this) {
            homeViewModel.state.signalStrength.set(it?.signalStrengthInfo)
            homeViewModel.state.activeNetworkInfo.set(it?.networkInfo)

            it?.networkInfo?.let { info ->
                binding.panelNetworkDetails.textNetworkName.text = info.name
                binding.panelNetworkDetails.textNetworkType.setTechnologyIcon(info)
            }
        }
        checkInformationAvailability()
        homeViewModel.state.informationAccessProblem.get()?.let { updateProblemUI(it) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            CODE_LOOP_INSTRUCTIONS -> {
                if (resultCode == Activity.RESULT_OK) {
                    homeViewModel.state.isLoopModeActive.set(true)
                } else {
                    homeViewModel.state.isLoopModeActive.set(false)
                }
            }
            CODE_SIGNAL_MEASUREMENT_TERMS -> {
                if (resultCode == Activity.RESULT_OK) {
                    homeViewModel.toggleSignalMeasurementService()
                    requireContext().toast(R.string.toast_signal_measurement_enabled)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        homeViewModel.attach(requireContext())

        startTimerForInfoWindow()
        homeViewModel.state.checkConfig()
    }

    override fun onStop() {
        super.onStop()
        homeViewModel.detach(requireContext())
    }

    /**
     * If user not doing any action within 2 second, information window display
     */
    private fun startTimerForInfoWindow() {
        Handler().postDelayed({

            if (homeViewModel.state.infoWindowStatus.get() == InfoWindowStatus.NONE) {
                homeViewModel.state.infoWindowStatus.set(InfoWindowStatus.VISIBLE)
            }
        }, INFO_WINDOW_TIME_MS)
    }

    private fun checkBasicNetworkInfoVisibility() {
        homeViewModel.connectivityInfoLiveData.listen(this) { info ->
            binding.root.post {
                if (info == null) {
                    binding.panelNetworkDetails.root.visibility = View.GONE
                } else {
                    binding.panelNetworkDetails.root.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun checkInformationAvailability() {
        val phonePermissionsGranted =
            context?.hasPermission(Manifest.permission.READ_PHONE_STATE) == true
        val locationPermissionsGranted =
            context?.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) == true || context?.hasPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == true
        val backgroundLocationPermissionsGranted =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context?.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == true
            } else {
                true
            }
        val problem = when {
            homeViewModel.state.isLocationEnabled.get() == LocationState.DISABLED_DEVICE -> {
                homeViewModel.state.informationAccessProblem.set(InformationAccessProblem.MISSING_LOCATION_ENABLED)
            }
            !locationPermissionsGranted -> {
                homeViewModel.state.informationAccessProblem.set(InformationAccessProblem.MISSING_LOCATION_PERMISSION)
            }
            !phonePermissionsGranted -> {
                homeViewModel.state.informationAccessProblem.set(InformationAccessProblem.MISSING_READ_PHONE_STATE_PERMISSION)
            }
            (!backgroundLocationPermissionsGranted) && (homeViewModel.state.isLoopModeActive.get() || homeViewModel.activeSignalMeasurementLiveData.value == true) -> {
                homeViewModel.state.informationAccessProblem.set(
                    InformationAccessProblem.MISSING_BACKGROUND_LOCATION_PERMISSION
                )
            }
            else -> homeViewModel.state.informationAccessProblem.set(InformationAccessProblem.NO_PROBLEM)
        }
    }

    interface NetworkInfoCallback {
        fun showNetworkInfo()
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE: Int = 10
        private const val INFO_WINDOW_TIME_MS: Long = 2000
        private const val CODE_SIGNAL_MEASUREMENT_TERMS = 12
        private const val CODE_LOOP_INSTRUCTIONS = 13
        private const val CODE_DIALOG_NEWS = 14
    }
}