package at.rtr.rmbt.android.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivitySignalMeasurementBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.location.mappers.toLocation
import at.rtr.rmbt.android.map.wrapper.LatLngW
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.HomeViewModel
import at.specure.location.LocationInfo
import at.specure.location.LocationState
import at.specure.test.SignalMeasurementType
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class SignalMeasurementActivity : BaseActivity(), OnMapReadyCallback {

    private val viewModel: HomeViewModel by viewModelLazy()
    private lateinit var binding: ActivitySignalMeasurementBinding
    private var map: GoogleMap? = null
    private val mapLocationListener = object : LocationSource {
        private var listener: LocationSource.OnLocationChangedListener? = null

        override fun activate(onLocationChangedListener: LocationSource.OnLocationChangedListener) {
            listener = onLocationChangedListener
        }

        override fun deactivate() {
            listener = null
        }

        fun updateLocation(location: LocationInfo) {
            val latestLocation = location.toLocation()
            listener?.onLocationChanged(latestLocation)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_signal_measurement)
        binding.isActive = false
        binding.isPaused = false

        setFullscreen()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        hideDialog()

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

        binding.buttonPause.setOnClickListener {
            viewModel.pauseSignalMeasurement()
        }

        binding.buttonResume.setOnClickListener {
            viewModel.resumeSignalMeasurement()
        }

        binding.fabClose.setOnClickListener {
            showStopDialog()
        }

        binding.fabWarning.setOnClickListener {
            showLocationProblemDialog()
        }

        viewModel.locationLiveData.listen(this) { location ->
            Timber.d("New location obtained: $location")
            binding.textSource.text = "${location?.provider} ${location?.accuracy}m"
            if (viewModel.isLocationInfoMeetingQualityCriteria()) {
                hideWarningButton()
                if (!viewModel.state.closeDialogDisplayed.get()) {
                    hideDialog()
                }
            } else {
                if (!viewModel.state.closeDialogDisplayed.get()) {
                    showWarningButton()
                    showLocationProblemDialogIfNotSilenced()
                }
            }
            map?.let {
                location?.let { latestLocation ->
                    mapLocationListener.updateLocation(
                        latestLocation
                    )
                }
            }
        }
    }

    private fun showWarningButton() {
        binding.fabWarning.hide()
        binding.fabWarning.show()
    }

    private fun hideWarningButton() {
        binding.fabWarning.show()
        binding.fabWarning.hide()
    }

    private fun setFullscreen() {
        window.statusBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
            window.insetsController?.hide(WindowInsets.Type.navigationBars())
            window.insetsController?.hide(WindowInsets.Type.displayCutout())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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

    // Get a handle to the GoogleMap object and display marker.
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        checkLocationAndSetCurrent()
        updateLocationPermissionRelatedUi()
    }

    override fun onStart() {
        super.onStart()
        viewModel.attach(this)
    }

    override fun onStop() {
        super.onStop()
        viewModel.detach(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        map = null
    }

    private fun checkLocationAndSetCurrent() {
        if (!viewModel.state.locationChanged.get()) {
            viewModel.locationLiveData.value?.let {
                with(LatLngW(it.latitude, it.longitude)) {
                    viewModel.state.cameraPositionLiveData.postValue(this)
                    viewModel.state.coordinatesLiveData.postValue(this)
                    map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), viewModel.state.zoom))
                }
            }
        } else {
            viewModel.state.cameraPositionLiveData.value?.let {
                map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), viewModel.state.zoom))
            }
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
        }
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
        val hideStopDialogJob = lifecycleScope.launch {
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
            finish()
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
            this.applicationContext?.let {
                if (ActivityCompat.checkSelfPermission(
                        it,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                        it,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    map?.isMyLocationEnabled = state == LocationState.ENABLED
                }
            }

            binding.fabLocation.setOnClickListener {
                if (state == LocationState.ENABLED) {
                    viewModel.locationLiveData.value?.let { info ->
                        Timber.d("New location obtained in fabLocation: $info")
                        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(info.latitude, info.longitude), 16F))
                    }
                }
            }
        }
    }

}