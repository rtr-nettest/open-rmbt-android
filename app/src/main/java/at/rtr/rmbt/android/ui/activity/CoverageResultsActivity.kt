package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
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
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MapStyleOptions
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimeZone
import kotlin.concurrent.timerTask
import kotlin.math.max
import androidx.core.graphics.createBitmap

class CoverageResultsActivity : BaseActivity(), OnMapReadyCallback {

    private val viewModel: CoverageResultViewModel by viewModelLazy()
    private lateinit var binding: ActivityCoverageResultBinding
    private var mapLoadRequested: Boolean = false
    private var map: GoogleMap? = null
    private var infoWindowMarker: Marker? = null
    private var timer: Timer? = null
    private var loadAttempts = 0
    private val emptyBitmap by lazy { createBitmap(1, 1) }
    private var loopUUID: String? = null
    private val isWholeLoop: Boolean
        get() = !loopUUID.isNullOrEmpty()
    // When opened as part of the loop navigation (whole-loop map, or a segment from the segment
    // list), back/close should just finish() and return to the caller instead of jumping to History.
    private var returnToCaller: Boolean = false
    // Ensures the test result details are fetched once even when the fences are already cached
    // (otherwise the "Test details" screen would be empty for such a segment).
    private var detailsLoadRequested = false

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

        loopUUID = intent.getStringExtra(KEY_LOOP_UUID)
        returnToCaller = intent.getBooleanExtra(KEY_RETURN_TO_CALLER, false)

        if (isWholeLoop) {
            // Whole-loop mode: the whole measurement (all segments) is shown as one map. There is no
            // single test/result to open, so the "Test details" button becomes a "Details" button
            // that opens the list of the individual segments instead.
            binding.testDetailsButton.setText(R.string.coverage_details_button)
            binding.testDetailsButton.setOnClickListener {
                loopUUID?.let { CoverageLoopSegmentsActivity.start(this, it) }
            }
        } else {
            val testUUID = intent.getStringExtra(KEY_TEST_UUID)
            check(!testUUID.isNullOrEmpty()) { "TestUUID was not passed to result activity" }
            viewModel.state.testUUID = testUUID

            // Opened from the loop's segment list -> this is one segment of a loop, make that clear.
            if (returnToCaller) {
                binding.headerTitle.setText(R.string.segment_result_title)
                binding.headerTitle.visibility = View.VISIBLE
            }

            binding.testDetailsButton.setOnClickListener {
                TestResultDetailActivity.start(this, viewModel.state.testUUID)
            }
        }

        binding.buttonBack.setOnClickListener {
            onBackPressed()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshResults()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (returnToCaller) {
                    // Opened from within the loop navigation: return to the caller (segment list or
                    // whole-loop map) via the normal back stack.
                    finish()
                } else {
                    HomeActivity.startWithFragment(this@CoverageResultsActivity, HomeActivity.Companion.HomeNavigationTarget.HISTORY_FRAGMENT_TO_SHOW)
                }
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
        map?.uiSettings?.isMyLocationButtonEnabled = false
        map?.uiSettings?.isMapToolbarEnabled = false
        map?.isIndoorEnabled = false
        map?.isBuildingsEnabled = false
        try {
            val success = map?.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))
            if (success == false) {
                Timber.e("Style parsing failed.")
            }
        } catch (e: Exception) {
            Timber.e(e, "Can't find style. Error: ")
        }
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(
            viewModel.state.cameraPositionLiveData.value ?: DefaultLocation.austriaLocation,
            viewModel.state.zoom
        ))
        map?.setOnCameraMoveListener {
            map?.cameraPosition?.zoom?.let { newZoom ->
                if (viewModel.state.zoom != newZoom) {
                    viewModel.state.zoom = newZoom
                    viewModel.updateMarkersRadius(newZoom)
                }
            }
        }
        map?.setOnCameraIdleListener {
            map?.cameraPosition?.zoom?.let { newZoom ->
                if (viewModel.state.zoom != newZoom) {
                    viewModel.state.zoom = newZoom
                    viewModel.updateMarkersRadius(newZoom)
                }
            }
            map?.cameraPosition?.let {
                viewModel.state.cameraPositionLiveData.postValue(LatLng(it.target.latitude, it.target.longitude))
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
        }

        map?.setOnMapClickListener {
            infoWindowMarker?.remove()
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
        // Legend (colours actually used) is shown in both modes.
        viewModel.legendLiveData.listen(this) { entries -> renderLegend(entries) }

        if (isWholeLoop) {
            // Whole-loop mode: render the aggregated fences of all segments; there is no server
            // result / details to load. A progress indicator is shown while the (possibly
            // not-yet-downloaded) segments are fetched and aggregated.
            binding.textWaitLoading.visibility = View.GONE
            binding.progressWholeLoop.visibility = View.VISIBLE
            // The header date/time (like the history overview) comes from the loop's newest segment.
            viewModel.loopSegmentsLiveData.listen(this) { segments ->
                segments.maxByOrNull { it.time }?.let { newest ->
                    val sdf = SimpleDateFormat("dd.MM.yy, HH:mm:ss", Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone(newest.timezone)
                    binding.testTime.text = sdf.format(Date(newest.time))
                    binding.testTime.visibility = View.VISIBLE
                }
            }
            viewModel.wholeLoopFencesLiveData.listen(this) { fences ->
                Timber.d("Loaded whole-loop points from livedata: ${fences.count()} for loop $loopUUID")
                binding.progressWholeLoop.visibility = View.GONE
                if (fences.isNotEmpty()) {
                    setUpMap(map, fences)
                }
            }
            loopUUID?.let {
                viewModel.loadWholeLoopMeasurement(it)
                viewModel.loadLoopSegments(it)
            }
            return
        }

        viewModel.fencesLiveData.listen(this) { fences ->
            Timber.d("Loaded points from livedata: ${fences.count()} for ${viewModel.state.testUUID} attempt: $loadAttempts")
            if (fences.isNotEmpty()) {
                setUpMap(map, fences)
                // The details are loaded together with the results by loadTestResults(); trigger it
                // once even though the fences are already cached, so "Test details" isn't empty.
                if (!detailsLoadRequested) {
                    detailsLoadRequested = true
                    viewModel.loadTestResults()
                }
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
            // A single coverage result (segment or standalone) has exactly one open-test-uuid, so a
            // share link can be built for it. The whole-loop overview has many, so it never shares.
            val openTestUuid = result?.testOpenUUID
            if (!openTestUuid.isNullOrEmpty()) {
                binding.buttonShare.visibility = View.VISIBLE
                binding.buttonShare.setOnClickListener { shareOpenResult(openTestUuid) }
            } else {
                binding.buttonShare.visibility = View.GONE
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

    /**
     * Shares a coverage result via a netztest.at open-data link built on the fly from the
     * open-test-uuid (the backend does not provide a share text for signal measurements). The "O"
     * prefix flags an open-test-uuid; it is not doubled if the uuid already starts with "O".
     */
    private fun shareOpenResult(openTestUuid: String) {
        val flagged = if (openTestUuid.startsWith("O")) openTestUuid else "O$openTestUuid"
        val url = "https://netztest.at/share/$flagged"
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, null))
    }

    private fun setUpMap(map: GoogleMap?, fences: List<FencesResultItemRecord>?) {
        Timber.d("Showing points: ${fences?.size} for ${viewModel.state.testUUID}")
        viewModel.clearPerformanceImprovementLists(map) // to show data after rotation without loading it again
        viewModel.updateMapPoints(map, fences, null)
    }

    /** Renders one legend row per technology present: a coloured dot + short generation label. */
    private fun renderLegend(entries: List<CoverageResultViewModel.CoverageLegendEntry>) {
        val container = binding.legendContainer
        container.removeAllViews()
        if (entries.isEmpty()) {
            binding.legendCard.visibility = View.GONE
            return
        }
        val density = resources.displayMetrics.density
        val dotSize = (12 * density).toInt()
        val gap = (6 * density).toInt()
        val rowGap = (2 * density).toInt()
        entries.forEach { entry ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, rowGap, 0, rowGap)
            }
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply { marginEnd = gap }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(entry.colorInt)
                }
            }
            val label = TextView(this).apply {
                text = entry.label
                setTextColor(ContextCompat.getColor(this@CoverageResultsActivity, R.color.text_dark_gray))
            }
            row.addView(dot)
            row.addView(label)
            container.addView(row)
        }
        binding.legendCard.visibility = View.VISIBLE
    }

    private fun refreshResults() {
        viewModel.loadTestResults()
        binding.swipeRefreshLayout.isRefreshing = true
    }

    override fun onDestroy() {
        cancelAnyPreviouslyRunningTimer()
        viewModel.clearPerformanceImprovementLists(map)
        infoWindowMarker?.remove()
        map?.setInfoWindowAdapter(null)
        map?.setOnCircleClickListener(null)
        map?.setOnMapClickListener(null)
        map?.setOnCameraMoveListener(null)
        map?.setOnCameraIdleListener(null)
        map = null
        super.onDestroy()
    }

    override fun onHandledException(exception: HandledException?) { }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    companion object {

        private const val KEY_TEST_UUID = "KEY_TEST_UUID"
        private const val KEY_LOOP_UUID = "KEY_LOOP_UUID"
        private const val KEY_RETURN_TO_CALLER = "KEY_RETURN_TO_CALLER"

        /**
         * @param returnToCaller when true, back/close returns to the caller via the normal back
         * stack (used when opened from the segment list); otherwise it navigates to History.
         */
        fun start(context: Context, testUUID: String, returnToCaller: Boolean = false) {
            val intent = Intent(context, CoverageResultsActivity::class.java)
            intent.putExtra(KEY_TEST_UUID, testUUID)
            intent.putExtra(KEY_RETURN_TO_CALLER, returnToCaller)
            context.startActivity(intent)
        }

        /** Opens the map of the WHOLE loop measurement (all segments combined). */
        fun startWholeLoop(context: Context, loopUUID: String) {
            val intent = Intent(context, CoverageResultsActivity::class.java)
            intent.putExtra(KEY_LOOP_UUID, loopUUID)
            intent.putExtra(KEY_RETURN_TO_CALLER, true)
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