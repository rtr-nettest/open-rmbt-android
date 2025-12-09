package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityCoverageResultBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.map.wrapper.LatLngW
import at.rtr.rmbt.android.ui.activity.toCoverageResultItemRecords
import at.rtr.rmbt.android.util.isGmsAvailable
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.CoverageResultViewModel
import at.specure.data.NetworkTypeCompat
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.FencesResultItemRecord
import at.specure.data.entity.TestResultRecord
import at.specure.data.entity.isCoverageResult
import at.specure.info.network.MobileNetworkType
import at.specure.location.LocationInfo
import at.specure.measurement.coverage.domain.models.state.CoverageMeasurementState
import at.specure.test.DeviceInfo
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import timber.log.Timber
import java.util.Timer
import kotlin.concurrent.timerTask
import kotlin.math.max

class CoverageResultsActivity : BaseActivity() {

    private val viewModel: CoverageResultViewModel by viewModelLazy()
    private lateinit var binding: ActivityCoverageResultBinding

    private var mapLoadRequested: Boolean = false
    private var map: GoogleMap? = null
    private var timer: Timer? = null

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

        if (!mapLoadRequested) {
            mapLoadRequested = true
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment?
            mapFragment!!.getMapAsync({ map ->
                this.map = map
            })
        }

        viewModel.state.playServicesAvailable.set(isGmsAvailable())

        val testUUID = intent.getStringExtra(KEY_TEST_UUID)
        check(!testUUID.isNullOrEmpty()) { "TestUUID was not passed to result activity" }

        viewModel.state.testUUID = testUUID

        viewModel.fencesLiveData.listen(this) { fences ->
            setUpMap(map, fences)
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

            result?.testOpenUUID?.let {

            }

            result?.let { testResultRecord ->
                setUpDetails(testResultRecord)
            }
        }
        viewModel.testResultDetailsLiveData.listen(this) {
            Timber.d("found ${it.size} rows of details")
            // todo: display result details
        }

        binding.fabClose.setOnClickListener {
            onBackPressed()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshResults()
        }

        viewModel.loadingLiveData.listen(this) {
            binding.swipeRefreshLayout.isRefreshing = false
            if (viewModel.state.testResult.get() == null) {
                binding.textWaitLoading.visibility = if (it) View.GONE else View.VISIBLE
            } else {
                binding.textWaitLoading.visibility = View.GONE
            }
        }

        binding.testDetailsButton.setOnClickListener {
            TestResultDetailActivity.start(this, viewModel.state.testUUID)
        }

        refreshResults()
    }

    private fun cancelAnyPreviouslyRunningTimer() {
        try {
            this.timer?.cancel()
        } catch (e: IllegalStateException) {
            Timber.e(e.localizedMessage)
        }
    }

    private fun setUpMap(map: GoogleMap?, fences: List<FencesResultItemRecord>?) {
        viewModel.updateMapPoints(map, fences, null)
    }


    private fun setUpDetails(testResultRecord: TestResultRecord) {
        // todo: implement
    }


    private fun setUpSpeedMeasurementMap(result: TestResultRecord) {

    }

    private fun setUpCoverageMeasurementMap(result: TestResultRecord) {
        // todo: prepare map with fences
    }

    private fun refreshResults() {
        viewModel.loadTestResults()
        binding.swipeRefreshLayout.isRefreshing = true
    }

    override fun onHandledException(exception: HandledException?) { }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        HomeActivity.startWithFragment(this, HomeActivity.Companion.HomeNavigationTarget.HISTORY_FRAGMENT_TO_SHOW)
    }

    companion object {

        private const val ZOOM_LEVEL = 15f
        private const val CIRCLE_RADIUS = 13.0
        private const val STROKE_WIDTH = 7f
        private const val ANCHOR_U = 0.5f
        private const val ANCHOR_V = 0.865f

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
        averagePingMillis = this.avgPingMillis
    )
}