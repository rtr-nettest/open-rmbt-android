package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityCoverageResultBinding
import at.rtr.rmbt.android.databinding.ItemCoverageMarkerDetailsBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.map.DefaultLocation
import at.rtr.rmbt.android.util.isGmsAvailable
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.CoverageResultViewModel
import at.rtr.rmbt.android.viewmodel.viewData.CoverageMarkerDetailsData
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.FencesResultItemRecord
import at.specure.info.network.MobileNetworkType
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Timer
import kotlin.concurrent.timerTask
import kotlin.math.max

class CoverageResultsActivity : BaseActivity(), OnMapReadyCallback {

    private val viewModel: CoverageResultViewModel by viewModelLazy()
    private lateinit var binding: ActivityCoverageResultBinding
    private var mapLoadRequested: Boolean = false
    private var map: GoogleMap? = null
    private var timer: Timer? = null
    private var loadAttempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_coverage_result)
        binding.state = viewModel.state

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
                val insetsSystemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                val insetsDisplayCutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
                val topSafe = max(insetsSystemBars.top, insetsDisplayCutout.top)
                val leftSafe = max(insetsSystemBars.left, insetsDisplayCutout.left)
                val rightSafe = max(insetsSystemBars.right, insetsDisplayCutout.right)
                val bottomSafe = max(insetsSystemBars.bottom, insetsDisplayCutout.bottom)

                v.updatePadding(
                    right = rightSafe,
                    left = leftSafe,
                    top = topSafe,
                    bottom = bottomSafe
                )
                windowInsets
            }
        }

        viewModel.state.playServicesAvailable.set(isGmsAvailable())

        val testUUID = intent.getStringExtra(KEY_TEST_UUID)
        check(!testUUID.isNullOrEmpty()) { "TestUUID was not passed to result activity" }

        viewModel.state.testUUID = testUUID

        binding.fabClose.setOnClickListener {
            onBackPressed()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshResults()
        }

        binding.testDetailsButton.setOnClickListener {
            TestResultDetailActivity.start(this, viewModel.state.testUUID)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                HomeActivity.startWithFragment(this@CoverageResultsActivity, HomeActivity.Companion.HomeNavigationTarget.HISTORY_FRAGMENT_TO_SHOW)
            }
        })
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
    }

    private fun cancelAnyPreviouslyRunningTimer() {
        try {
            this.timer?.cancel()
        } catch (e: IllegalStateException) {
            Timber.e(e.localizedMessage)
        }
    }

    // Get a handle to the GoogleMap object and display marker.
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        lifecycleScope.launch {
            onMapFullyReady()
//            refreshResults()
        }
    }

    fun onMapFullyReady() {
        this.map = map
        map?.uiSettings?.isMyLocationButtonEnabled = false
        map?.uiSettings?.isMapToolbarEnabled = false
        map?.isIndoorEnabled = false
        map?.isBuildingsEnabled = false
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(
            viewModel.state.cameraPositionLiveData.value ?: DefaultLocation.austriaLocation,
            viewModel.state.zoom
        ))
        map?.setOnCameraIdleListener {
            map?.cameraPosition?.zoom?.let { newZoom ->
                viewModel.state.zoom = newZoom
            }
            map?.cameraPosition?.let {
                viewModel.state.cameraPositionLiveData.postValue(LatLng(it.target.latitude, it.target.longitude))
            }
        }

        map?.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {

            override fun getInfoWindow(marker: Marker): View? = null

            override fun getInfoContents(marker: Marker): View {
                val binding = ItemCoverageMarkerDetailsBinding.inflate(
                    LayoutInflater.from(this@CoverageResultsActivity)
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
        viewModel.fencesLiveData.listen(this) { fences ->
            Timber.d("Loaded points from livedata: ${fences.count()} for ${viewModel.state.testUUID} attempt: $loadAttempts")
            if (fences.isNotEmpty()) {
                setUpMap(map, fences)
            } else {
                if (loadAttempts < 3) {
                    refreshResults()
                    loadAttempts++
                }
            }
        }

        viewModel.testServerResultLiveData.listen(this) { result ->

            // show local results if no results from server after 2000 ms
            if (result?.isLocalOnly == true) {
                cancelAnyPreviouslyRunningTimer()
                timer = Timer()
                timer?.schedule(timerTask {
                    viewModel.state.testResult.set(result)
                }, 2000)
            } else {
                cancelAnyPreviouslyRunningTimer()
                viewModel.state.testResult.set(result)
            }
        }
        viewModel.testResultDetailsLiveData.listen(this) {
            Timber.d("found ${it.size} rows of details")
            // todo: display result details
        }
        viewModel.loadingLiveData.listen(this) {
            binding.swipeRefreshLayout.isRefreshing = false
            if (viewModel.state.testResult.get() == null) {
                binding.textWaitLoading.visibility = if (it) View.GONE else View.VISIBLE
            } else {
                binding.textWaitLoading.visibility = View.GONE
            }
        }
    }

    private fun setUpMap(map: GoogleMap?, fences: List<FencesResultItemRecord>?) {
        Timber.d("Showing points: ${fences?.size} for ${viewModel.state.testUUID}")
        viewModel.clearPerformanceImprovementLists() // to show data after rotation without loading it again
        viewModel.updateMapPoints(map, fences, null, null)
    }

    private fun refreshResults() {
        viewModel.loadTestResults()
        binding.swipeRefreshLayout.isRefreshing = true
    }

    override fun onHandledException(exception: HandledException?) { }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    companion object {

        private const val KEY_TEST_UUID = "KEY_TEST_UUID"

        fun start(context: Context, testUUID: String) {
            val intent = Intent(context, CoverageResultsActivity::class.java)
            intent.putExtra(KEY_TEST_UUID, testUUID)
            context.startActivity(intent)
        }
    }
}

fun List<CoverageMeasurementFenceRecord>?.toCoverageResultItemRecords(): List<FencesResultItemRecord>? {
    return this?.mapIndexed { index, it ->
        it.toFencesResultItemRecord(index)
    }
}

fun CoverageMeasurementFenceRecord.toFencesResultItemRecord(index: Int): FencesResultItemRecord {
    return FencesResultItemRecord(
        id = this.sequenceNumber.toLong(),
        testUUID = this.sessionId,
        fenceRemoteId = null,
        networkTechnologyId = this.technologyId,
        networkTechnologyName = MobileNetworkType.fromValue(this.technologyId ?: 0).displayName,
        latitude = this.location?.lat,
        longitude = this.location?.long,
        fenceRadiusMeters = this.radiusMeters,
        durationMillis = this.leaveTimestampMillis - this.entryTimestampMillis,
        offsetMillis = this.entryTimestampMillis,
        averagePingMillis = this.avgPingMillis,
        fenceTimestampMillis = this.entryTimestampMillis,
        signalMainDbm = signalStrength,
    )
}