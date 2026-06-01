package at.rtr.rmbt.android.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.lifecycleScope
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivitySignalMeasurementBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.map.wrapper.LatLngW
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.HomeViewModel
import at.specure.location.LocationInfo
import at.specure.location.LocationState
import at.specure.test.DeviceInfo
import at.rmbt.client.control.data.SignalMeasurementType
import at.rtr.rmbt.android.databinding.ItemCoverageMarkerDetailsBinding
import at.rtr.rmbt.android.map.DefaultLocation
import at.rtr.rmbt.android.ui.dialog.CoverageSettingsDialog
import at.rtr.rmbt.android.ui.dialog.MessageDialog
import at.rtr.rmbt.android.util.formatAccuracy
import at.specure.info.network.NetworkInfo
import at.specure.measurement.coverage.domain.models.CoverageMeasurementData
import at.specure.measurement.coverage.domain.models.state.CoverageMeasurementState
import at.specure.measurement.coverage.presentation.validators.CoverageNetworkValidator
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineName
import timber.log.Timber
import kotlin.math.roundToInt
import at.rtr.rmbt.android.viewmodel.CoverageResultViewModel
import at.rtr.rmbt.android.viewmodel.viewData.CoverageMarkerDetailsData
import at.specure.measurement.coverage.data.getFrequencyBand
import at.specure.measurement.coverage.data.getSignalStrengthValue
import at.specure.test.toDeviceInfoLocation
import at.specure.util.hasPermission
import at.specure.util.openAppSettings
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

const val DEFAULT_POSITION_TRACKING_ZOOM_LEVEL = 16.2f
const val DEFAULT_TRACKING_ZOOM_LEVEL = 16f
const val GPS_CHECK_GRACE_PERIOD = 6000L

class SignalMeasurementActivity() : BaseActivity(), OnMapReadyCallback, CoverageSettingsDialog.Callback {

    val networkValidator = CoverageNetworkValidator()
    private val viewModel: HomeViewModel by viewModelLazy()
    private val coverageViewModel: CoverageResultViewModel by viewModelLazy()
    private lateinit var binding: ActivitySignalMeasurementBinding
    private var map: GoogleMap? = null
    private var infoWindowMarker: Marker? = null
    private var warningSnackbar: Snackbar? = null
    private var sendingResultsErrorSnackbar: Snackbar? = null
    private var noBackgroundLocationPermissionGrantedSnackbar: Snackbar? = null
    private var showMeasurementResultsJob: kotlinx.coroutines.Job? = null
    private var updateUnfinishedMeasurementJob: kotlinx.coroutines.Job? = null
    private val emptyBitmap by lazy { createBitmap(1, 1) }

    override fun onFenceOrAccuracyUpdated() {
        coverageViewModel.onCoverageConfigurationChanged()
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_signal_measurement)
        binding.state = viewModel.state
        coverageViewModel.onConfigurationChanged(map)
        viewModel.shouldStartDedicatedMeasurementStateChecker = {
            coverageViewModel.shouldRunCoverageMeasurement()
        }
        binding.isActive = false
        binding.isPaused = false

        setFullscreen()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (shouldAllowBackPress()) {
                    showStopDialog()
                }
            }
        })

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        hideDialog()

        coverageViewModel.coverageMeasurementDataLiveData.listen(this) {
            viewModel.state.coverageSessionStart.set(it?.coverageMeasurementSession?.startTimeLoopMillis ?: 0)
            updateMapState(it)
        }

        viewModel.activeSignalMeasurementLiveData.listen(this) {
            binding.isActive = it
        }

        viewModel.pausedSignalMeasurementLiveData.listen(this) {
            binding.isPaused = it
        }

        binding.buttonStart.setOnClickListener {
            viewModel.startSignalMeasurement(SignalMeasurementType.DEDICATED)
        }

        binding.buttonStop.setOnClickListener {
            viewModel.stopSignalMeasurement()
        }

        binding.fabClose.setOnClickListener {
            if (coverageViewModel.coverageMeasurementDataLiveData.value?.state != CoverageMeasurementState.FINISHED_LOOP_CORRECTLY) {
                showStopDialog()
            } else {
                coverageViewModel.clearMeasurementData()
                coverageViewModel.clearPerformanceImprovementLists(map)
                finish()
            }
        }

        binding.fabSettings.setOnClickListener {
            CoverageSettingsDialog.show(supportFragmentManager)
        }

        binding.fabWarning.setOnClickListener {
            showLocationProblemDialog()
        }

        viewModel.dedicatedSignalMeasurementSessionIdLiveData.listen(this) { sessionId ->
            Timber.d("SessionId loaded: $sessionId")
            coverageViewModel.onCoverageSessionLoaded(sessionId)

        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (shouldAllowBackPress()) {
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun enterInPictureMode() {
        val ratio = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val screenBounds = this.windowManager.maximumWindowMetrics.bounds
            val width = screenBounds.width()
            val height = screenBounds.height()
            Rational(width, height)
        } else {
            val display = windowManager.defaultDisplay
            val point = Point();
            display.getSize(point);
            val width = point.x;
            val height = point.y;
            Rational(width, height);
        }
        val pipBuilder = PictureInPictureParams.Builder()
        pipBuilder.setAspectRatio(ratio).build()
        enterPictureInPictureMode(pipBuilder.build())
    }

    private fun updateMapState(data: CoverageMeasurementData?) {
        if (data?.state == CoverageMeasurementState.FINISHED_LOOP_CORRECTLY) {
            showMeasurementResults(data)
        } else {
            updateUnfinishedMeasurement(data)
        }
        updateResultSendStatus(data)
    }

    private fun updateResultSendStatus(data: CoverageMeasurementData?) {
        if (data?.sendingResultsError == true) {
            showSendDataErrorSnackbar()
        }
    }

    private fun showSendDataErrorSnackbar() {
        if (sendingResultsErrorSnackbar?.isShownOrQueued == true) {
            // Already visible or scheduled to show
            return
        }

        sendingResultsErrorSnackbar = createErrorSnackbar(
            getString(R.string.error_sending_data),
            R.string.dismiss,
            {
                coverageViewModel.onSendingResultErrorClearPressed()
            }
        )
        sendingResultsErrorSnackbar?.show()
    }

    private fun showNoBackgroundLocationAllowed() {
        if (sendingResultsErrorSnackbar?.isShownOrQueued == true) return
        if (warningSnackbar?.isShownOrQueued == true) return
        if (noBackgroundLocationPermissionGrantedSnackbar?.isShownOrQueued == true) return

        noBackgroundLocationPermissionGrantedSnackbar = createErrorSnackbar(
            getString(R.string.location_usage_always_warning_message),
            R.string.allow,
            {
                this@SignalMeasurementActivity.openAppSettings()
                hideBackgroundLocationMissingSnackbar()
            }
        )
        noBackgroundLocationPermissionGrantedSnackbar?.show()
    }

    private fun createErrorSnackbar(
        message: String,
        actionResId: Int,
        action: (View) -> Unit
    ): Snackbar {
        return Snackbar.make(binding.root, message, Snackbar.LENGTH_INDEFINITE)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.snackbar_error_background))
            .setTextColor(ContextCompat.getColor(this, R.color.snackbar_error_text))
            .setAction(actionResId) {view ->
                action(view)
            }
            .setActionTextColor(ContextCompat.getColor(this, R.color.snackbar_error_text))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        coverageViewModel.onConfigurationChanged(map)
    }

    private fun showMeasurementResults(coverageMeasurementData: CoverageMeasurementData) {
        coverageViewModel.clearPerformanceImprovementLists(map)
        hideWarningButton()
        hideDialog()
        setMyPositionAndButtonVisible(false)
        hideNetworkWarningSnackbar()
        hideBackgroundLocationMissingSnackbar()
        setInfoVisible(false)
        setResultTitleVisible(true)
        setSettingsButtonVisible(false)
        updateSendingResultsInfo(coverageMeasurementData.sendingResults)
        // Launch a coroutine to safely update the map
        showMeasurementResultsJob?.cancel()
        showMeasurementResultsJob = lifecycleScope.launch {
            coverageViewModel.updateMapPoints(
                map,
                coverageMeasurementData?.fences.toCoverageResultItemRecords(),
                coverageMeasurementData?.state
            )
        }
    }

    private fun updateUnfinishedMeasurement(coverageMeasurementData: CoverageMeasurementData?) {

        setSettingsButtonVisible(true)
        checkNetwork(coverageMeasurementData?.currentNetworkInfo)
        checkLocationPermissions()
        setInfoVisible(true)
        setResultTitleVisible(false)
        updatePingValue(coverageMeasurementData)
        showCurrentNetworkType(coverageMeasurementData)
        showMeasurementError(coverageMeasurementData)
        updateSendingResultsInfo(coverageMeasurementData?.sendingResults ?: false)

        // Launch a coroutine to safely update the map
        updateUnfinishedMeasurementJob?.cancel()
        updateUnfinishedMeasurementJob = lifecycleScope.launch {
//            map?.awaitMapLoad()
            setMyPositionAndButtonVisible(true)
            val longEnoughTimePassedFromStart = (3000.plus(coverageMeasurementData?.coverageMeasurementSession?.startTimeMeasurementMillis ?: 0) <= System.currentTimeMillis())
            if (coverageMeasurementData?.currentLocation != null || (coverageMeasurementData?.coverageMeasurementSession != null
                        && longEnoughTimePassedFromStart)
            ) {
                updateCurrentLocation(coverageMeasurementData?.currentLocation)
            }
            coverageViewModel.updateMapPoints(
                map,
                coverageMeasurementData?.fences.toCoverageResultItemRecords(),
                coverageMeasurementData?.state
            )
        }
    }

    private fun checkLocationPermissions() {
        val backgroundLocationPermissionsGranted =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                this.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                true
            }
        if (!backgroundLocationPermissionsGranted) {
            showNoBackgroundLocationAllowed()
        } else {
            hideBackgroundLocationMissingSnackbar()
        }
    }

    private fun setSettingsButtonVisible(visible: Boolean) {
        binding.fabSettings.visibility = if (visible && !coverageViewModel.state.pipActive.get()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun setMyPositionAndButtonVisible(visible: Boolean) {

        val enabled = visible && coverageViewModel.coverageMeasurementDataLiveData.value?.state != CoverageMeasurementState.FINISHED_LOOP_CORRECTLY

        binding.fabLocation.isEnabled = enabled

        this.applicationContext?.let {
            try {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    if (enabled == false) {
                        map?.setLocationSource(null)
                    }
                    map?.isMyLocationEnabled = enabled
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    throw e
                }
                Timber.e("Unable to deactivate my location on the map  ${e.message}")
            }
        }

        binding.fabLocation.setOnClickListener {
            if (enabled) {
                val currentLocation = coverageViewModel.coverageMeasurementDataLiveData?.value?.currentLocation ?: return@setOnClickListener
                Timber.d("Setting latest location to 2: $currentLocation")
                map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(currentLocation.latitude, currentLocation.longitude), DEFAULT_TRACKING_ZOOM_LEVEL))
            }
        }

        binding.fabLocation.visibility = if (enabled && !coverageViewModel.state.pipActive.get()) {
            View.VISIBLE
        } else {
            View.GONE
        }

    }

    private fun setResultTitleVisible(visible: Boolean) {
        binding.measurementResultTitle.visibility = if (visible && !coverageViewModel.state.pipActive.get()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun setInfoVisible(visible: Boolean) {

        binding.measurementProgressInfoPipPing.visibility = if (visible && coverageViewModel.state.pipActive.get()) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.measurementProgressInfoPipNetwork.visibility = if (visible && coverageViewModel.state.pipActive.get()) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.measurementProgressInfo.visibility = if (visible && !coverageViewModel.state.pipActive.get()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun showMeasurementError(coverageMeasurementData: CoverageMeasurementData?) {
        coverageMeasurementData?.signalMeasurementException?.also {
            MessageDialog.show(this.supportFragmentManager, getString(R.string.coverage_measurement_error_unknown), "CoverageMeasurementErrorDialog")
        }
    }

    private fun showCurrentNetworkType(coverageMeasurementData: CoverageMeasurementData?) {
        val networkType = coverageViewModel.getCurrentNetworkTypeName(coverageMeasurementData?.currentNetworkInfo)
        val frequencyBand = coverageMeasurementData?.currentNetworkInfo?.getFrequencyBand()
        val signal = coverageMeasurementData?.currentNetworkInfo?.getSignalStrengthValue()
        val networkStringRaw = listOfNotNull(networkType, frequencyBand, signal).joinToString(" | ")
        val networkString = networkStringRaw.ifEmpty {
            ""
        }
        binding.technologyValue.text = networkString
        binding.technologyValuePip.text = networkString
    }

    private fun updatePingValue(coverageMeasurementData: CoverageMeasurementData?) {
        val pingResult = coverageMeasurementData?.currentPingMs
        val pingText = if (pingResult != null && pingResult > 0) {
            binding.pingValue.visibility = View.VISIBLE
            binding.pingValuePip.visibility = View.VISIBLE
            val mantissa = pingResult - (pingResult.toInt().toDouble())
            if (mantissa > 0 && pingResult < 10.0) {
                this.getString(R.string.measurement_ping_value_1f, pingResult)
            } else {
                this.getString(R.string.measurement_ping_value, pingResult.roundToInt().toString())
            }
        } else {
            if (coverageMeasurementData?.currentPingStatus != null) {
                binding.pingValue.visibility = View.VISIBLE
                binding.pingValuePip.visibility = View.VISIBLE
                coverageMeasurementData.currentPingStatus
            } else {
                binding.pingValue.visibility = View.INVISIBLE
                binding.pingValuePip.visibility = View.INVISIBLE
                this.getString(R.string.measurement_dash)
            }
        }
        binding.pingValue.text = pingText
        binding.pingValuePip.text = pingText
    }

    private fun updateSendingResultsInfo(sendingResults : Boolean) {
        if (sendingResults) {
            binding.sendingResults.visibility = View.VISIBLE
        } else {
            binding.sendingResults.visibility = View.GONE
        }
    }


    private fun updateCurrentLocation(location: LocationInfo?) {
        binding.textSource.text = "${location?.provider} ${location?.accuracy}m"
        val startTime = coverageViewModel.coverageMeasurementDataLiveData.value?.coverageMeasurementSession?.startTimeMeasurementMillis ?: 0L
        val gracePeriodEnded = System.currentTimeMillis() - startTime >= GPS_CHECK_GRACE_PERIOD
        val deviceInfoLocation = location.toDeviceInfoLocation()
        if (coverageViewModel.isLocationInfoMeetingQualityCriteria(deviceInfoLocation)) {
            hideWarningButton()
            if (!viewModel.state.closeDialogDisplayed.get()) {
                hideDialog()
            }
        } else if (!viewModel.state.closeDialogDisplayed.get() && gracePeriodEnded) {
            showWarningButton()
            showLocationProblemDialogIfNotSilenced()
        }

        binding.accuracyValue.text = location?.formatAccuracy()?.let { formattedAccuracy ->
            this.getString(R.string.location_dialog_accuracy, formattedAccuracy)
        } ?: getString(R.string.no_gps_value)

        map?.let { gMap ->
            location?.let { latestLocation ->
                Timber.d("Setting latest location to 1: $latestLocation")
                coverageViewModel.state.cameraPositionLiveData.postValue(LatLng(latestLocation.latitude, latestLocation.longitude))
                if (coverageViewModel.state.zoom <= DefaultLocation.austriaZoomLevel) {
                    coverageViewModel.state.zoom = DEFAULT_POSITION_TRACKING_ZOOM_LEVEL
                }
                if (!coverageViewModel.state.markerDetailsDisplayed.get()) {
                    gMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            latestLocation.toLatLng(),
                            coverageViewModel.state.zoom
                        )
                    )
                }
            }
        }
//            centerMapOnLocation()
    }

    private fun checkNetwork(networkInfo: NetworkInfo?) {
        if (!networkValidator.isNetworkToBeLogged(networkInfo = networkInfo)) {
            val message = getString(R.string.wrong_network_message, coverageViewModel.getCurrentNetworkTypeName(networkInfo))
            showNetworkWarningIfNotSilenced(binding.root, message)
        } else {
            hideNetworkWarningSnackbar()
        }
    }

    fun showNetworkWarningIfNotSilenced(view: View, message: String) {
        if (!viewModel.state.networkWarningDialogSilenced.get()) {
            showNetworkWarningSnackbar(view, message)
        }
    }

    fun showNetworkWarningSnackbar(view: View, message: String) {
        if (sendingResultsErrorSnackbar?.isShownOrQueued == true) return
        if (warningSnackbar?.isShownOrQueued == true) return

        warningSnackbar = createErrorSnackbar(
            message,
            R.string.dismiss,
            {
                viewModel.silenceNetworkWarning()
            }
        )

        warningSnackbar?.show()
    }

    fun hideNetworkWarningSnackbar() {
        warningSnackbar?.dismiss()
    }

    fun hideBackgroundLocationMissingSnackbar() {
        noBackgroundLocationPermissionGrantedSnackbar?.dismiss()
    }

    private fun showWarningButton() {
        binding.fabWarning.hide()
        binding.fabWarning.show()
    }

    private fun hideWarningButton() {
        binding.fabWarning.show()
        binding.fabWarning.hide()
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setFullscreen() {
        window.statusBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
            window.insetsController?.hide(WindowInsets.Type.navigationBars())
            window.insetsController?.hide(WindowInsets.Type.displayCutout())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
        }
    }

    private fun shouldAllowBackPress(): Boolean {
        return false
    }

    suspend fun GoogleMap.awaitLoadedOnce(): GoogleMap =
        suspendCancellableCoroutine { cont ->
            setOnMapLoadedCallback {
                if (cont.isActive) cont.resume(this, null)
            }
        }

    // Get a handle to the GoogleMap object and display marker.
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(
            coverageViewModel.state.cameraPositionLiveData.value ?: DefaultLocation.austriaLocation,
            coverageViewModel.state.zoom
        ))
        Timber.d("Setting latest location to 3: ${coverageViewModel.state.cameraPositionLiveData.value ?: DefaultLocation.austriaLocation}")
        lifecycleScope.launch {
            map?.awaitLoadedOnce()
            onMapFullyReady()
        }
    }

    fun onMapFullyReady() {
        checkLocationAndSetCurrent()
        updateLocationPermissionRelatedUi()
        map?.uiSettings?.isMyLocationButtonEnabled = false
        map?.uiSettings?.isMapToolbarEnabled = false
        map?.isIndoorEnabled = false
        map?.isBuildingsEnabled = false
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(
            coverageViewModel.state.cameraPositionLiveData.value ?: DefaultLocation.austriaLocation,
            coverageViewModel.state.zoom
        ))
        Timber.d("Setting latest location to 4: ${coverageViewModel.state.cameraPositionLiveData.value ?: DefaultLocation.austriaLocation}")
        map?.setOnCameraMoveListener {
            map?.cameraPosition?.zoom?.let { newZoom ->
                if (coverageViewModel.state.zoom != newZoom) {
                    coverageViewModel.state.zoom = newZoom
                    coverageViewModel.updateMarkersRadius(newZoom)
                }
            }
        }

        map?.setOnCameraIdleListener {
            map?.cameraPosition?.zoom?.let { newZoom ->
                if (coverageViewModel.state.zoom != newZoom) {
                    coverageViewModel.state.zoom = newZoom
                    coverageViewModel.updateMarkersRadius(newZoom)
                }
            }
        }

        map?.setOnCircleClickListener { circle ->
            infoWindowMarker = map?.addMarker(
                MarkerOptions()
                    .position(circle.center)
                    .icon(BitmapDescriptorFactory.fromBitmap(emptyBitmap))
                    .anchor(0.5f, 0.5f)
            )
            infoWindowMarker?.tag = circle.tag
            infoWindowMarker?.showInfoWindow()
            coverageViewModel.state.markerDetailsDisplayed.set(true)
        }

        map?.setOnMapClickListener {
            infoWindowMarker?.remove()
            coverageViewModel.state.markerDetailsDisplayed.set(false)
        }

        if (coverageViewModel.coverageMeasurementDataLiveData.value?.state == CoverageMeasurementState.FINISHED_LOOP_CORRECTLY) {
            updateMapState(coverageViewModel.coverageMeasurementDataLiveData.value)
        }
        map?.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {

            override fun getInfoWindow(marker: Marker): View? = null

            override fun getInfoContents(marker: Marker): View {
                val binding = ItemCoverageMarkerDetailsBinding.inflate(
                    LayoutInflater.from(this@SignalMeasurementActivity)
                )

                val data = marker.tag as? CoverageMarkerDetailsData
                if (data != null) {
                    binding.item = data
                    binding.executePendingBindings()
                }

                binding.root.setOnClickListener {

                }

                return binding.root
            }
        })
    }

    override fun onStart() {
        super.onStart()
        viewModel.attach(this)
        lifecycleScope.launch(CoroutineName("Starting signal measurement")) {
            // TODO: maybe a little improve and instead of delay add state variable which listens on onServiceConnected in homeViewModel and start it there, but drawback is that we need to know when we want to continue there
            delay(1000)
            Timber.d("Starting signal measurement")
            viewModel.startSignalMeasurement(SignalMeasurementType.DEDICATED)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterInPictureMode()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        coverageViewModel.state.pipActive.set(isInPictureInPictureMode)

        if (isInPictureInPictureMode) {
            binding.fabLocation.visibility = View.GONE
            binding.fabClose.visibility = View.GONE
            setResultTitleVisible(false)
        } else {
            binding.fabClose.visibility = View.VISIBLE
            binding.fabLocation.visibility = View.VISIBLE
            if (coverageViewModel.coverageMeasurementDataLiveData.value?.state == CoverageMeasurementState.FINISHED_LOOP_CORRECTLY) {
                updateMapState(coverageViewModel.coverageMeasurementDataLiveData.value)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.detach(this)
    }

    override fun onDestroy() {
        coverageViewModel.clearPerformanceImprovementLists(map)
        infoWindowMarker?.remove()
        map?.setInfoWindowAdapter(null)
        map?.setOnCircleClickListener(null)
        map?.setOnMapClickListener(null)
        map?.setOnCameraMoveListener(null)
        map?.setOnCameraIdleListener(null)
        map = null
        super.onDestroy()
    }

    private fun checkLocationAndSetCurrent() {
        if (!viewModel.state.locationChanged.get()) {
            viewModel.locationLiveData.value?.let {
                with(LatLngW(it.latitude, it.longitude)) {
                    viewModel.state.cameraPositionLiveData.postValue(this)
                    viewModel.state.coordinatesLiveData.postValue(this)
                    Timber.d("Setting latest location to 5: ${it}")
                    map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), DEFAULT_POSITION_TRACKING_ZOOM_LEVEL))
                }
                return
            }
        } else {
            var latitude = DefaultLocation.austriaLocation.latitude
            var longitude = DefaultLocation.austriaLocation.longitude
            var zoomLevel = DEFAULT_POSITION_TRACKING_ZOOM_LEVEL
            viewModel.state.cameraPositionLiveData.value?.let {
                latitude = it.latitude
                longitude = it.longitude
                if (latitude != 0.0 || longitude != 0.0) {
                    zoomLevel = DEFAULT_POSITION_TRACKING_ZOOM_LEVEL
                }
            }
            Timber.d("Setting latest location to 6: ${latitude} $longitude")
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), zoomLevel))

//            visiblePosition = RecyclerView.NO_POSITION
//            onCloseMarkerDetails()
//            if (isMarkersAvailable()) {
//                viewModel.state.coordinatesLiveData.value?.let {
//                    viewModel.loadMarkers(
//                        it.latitude,
//                        it.longitude,
//                        mapW().currentCameraZoom().toInt()
//                    )
//                }
//            }
            return
        }
        var latitude = DefaultLocation.austriaLocation.latitude
        var longitude = DefaultLocation.austriaLocation.longitude
        var zoomLevel = DEFAULT_POSITION_TRACKING_ZOOM_LEVEL
        Timber.d("Setting latest location to 7: ${latitude} $longitude")
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), zoomLevel))
    }

    private fun showLocationProblemDialogIfNotSilenced() {
        if (!viewModel.state.locationWarningDialogSilenced.get()) {
            showLocationProblemDialog()
        }
    }


    private fun showLocationProblemDialog() {
        binding.warningMessageTitle.text =
            ContextCompat.getString(this, R.string.loop_mode_no_gps)
        binding.warningMessageContent.text =
            ContextCompat.getString(this, R.string.no_gps_or_insufficient_precision)
        binding.warningMessageAction.visibility = View.GONE
        binding.warningMessageCancel.text = ContextCompat.getString(this, R.string.confirm)

        binding.warningMessageCancel.setOnClickListener {
            viewModel.silenceLocationDialogWarning()
            hideDialog()
        }
        binding.warningMessage.visibility = View.VISIBLE
    }

    private fun showStopDialog() {
        val hideStopDialogJob = lifecycleScope.launch(CoroutineName("Hiding stop dialog")) {
            delay(5000L)
            hideDialog()
        }
        viewModel.setIsCloseDialogShown(true)
        binding.warningMessageTitle.text = ContextCompat.getString(this, R.string.stop_signal_measurement_title)
        binding.warningMessageContent.text = ContextCompat.getString(this, R.string.stop_signal_measurement_text)
        binding.warningMessageAction.text = ContextCompat.getString(this, R.string.text_stop_measurement)
        binding.warningMessageAction.visibility = View.VISIBLE
        binding.warningMessageCancel.text = ContextCompat.getString(this, R.string.text_continue_measurement)

        binding.warningMessageAction.setOnClickListener {
            hideStopDialogJob.cancel()
            viewModel.stopSignalMeasurement()
            hideDialog()
        }
        binding.warningMessageCancel.setOnClickListener {
            hideStopDialogJob.cancel()
            binding.warningMessage.visibility = View.GONE
        }
        binding.warningMessage.visibility = View.VISIBLE
    }

    private fun hideDialog() {
        viewModel.setIsCloseDialogShown(false)
        binding.warningMessage.visibility = View.GONE
    }

    private fun updateLocationPermissionRelatedUi() {
        viewModel.locationStateLiveData.listen(this) { state ->
            val enabled = state == LocationState.ENABLED
            setMyPositionAndButtonVisible(enabled)
        }
    }

    companion object {

        fun start(context: Context) = context.startActivity(Intent(context, SignalMeasurementActivity::class.java))
    }
}

private fun DeviceInfo.Location?.toLatLng(): LatLng? {
    this?.let {
        return LatLng(it.lat, it.long)
    }
    return null
}
