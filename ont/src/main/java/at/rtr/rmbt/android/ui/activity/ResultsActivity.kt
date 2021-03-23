package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityResultsBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.ResultViewModel
import at.specure.data.NetworkTypeCompat
import at.specure.data.entity.TestResultGraphItemRecord
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback

class ResultsActivity : BaseActivity(), OnMapReadyCallback {

    private val viewModel: ResultViewModel by viewModelLazy()
    private lateinit var binding: ActivityResultsBinding

    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_results)
        binding.state = viewModel.state

        viewModel.state.playServicesAvailable.set(checkPlayServices())

        val testUUID = intent.getStringExtra(KEY_TEST_UUID)
        check(!testUUID.isNullOrEmpty()) { "TestUUID was not passed to result activity" }

        val returnPoint = intent.getStringExtra(KEY_RETURN_POINT)
        check(!testUUID.isNullOrEmpty()) { "ReturnPoint was not passed to result activity" }

        viewModel.state.testUUID = testUUID
        viewModel.state.returnPoint = returnPoint?.let { ReturnPoint.valueOf(returnPoint) } ?: ReturnPoint.HOME
        viewModel.testServerResultLiveData.listen(this) { result ->
            viewModel.state.testResult.set(result)

            loadGraphItems(viewModel.state.downloadGraphData, viewModel.downloadGraphLiveData, TestResultGraphItemRecord.Type.DOWNLOAD)
            loadGraphItems(viewModel.state.uploadGraphData, viewModel.uploadGraphLiveData, TestResultGraphItemRecord.Type.UPLOAD)
        }

        binding.buttonBack.setOnClickListener {
            onBackPressed()
        }
        binding.buttonShare.setOnClickListener {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_TEXT, viewModel.state.testResult.get()?.shareText)
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, viewModel.state.testResult.get()?.shareTitle)
            shareIntent.type = "text/plain"
            startActivity(Intent.createChooser(shareIntent, null))
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshResults()
        }

        viewModel.loadingLiveData.listen(this) {
            binding.swipeRefreshLayout.isRefreshing = false
            if (viewModel.state.testResult.get() == null) {
                binding.textFailedToLoad.visibility = if (it) View.GONE else View.VISIBLE
            } else {
                binding.textFailedToLoad.visibility = View.GONE
            }
        }
        refreshResults()
    }

    private fun loadGraphItems(
        graphLoadedData: List<TestResultGraphItemRecord>?,
        graphLiveData: LiveData<List<TestResultGraphItemRecord>>,
        type: TestResultGraphItemRecord.Type
    ) {
        if (graphLoadedData.isNullOrEmpty()) {
            graphLiveData.listen(this) {
                when (type) {
                    TestResultGraphItemRecord.Type.DOWNLOAD -> viewModel.state.downloadGraphData = it
                    TestResultGraphItemRecord.Type.UPLOAD -> viewModel.state.uploadGraphData = it
                }
                showGraphItems(type)
            }
        } else {
            showGraphItems(type)
        }
    }

    private fun showGraphItems(type: TestResultGraphItemRecord.Type) {
        val isActivityInForeground = this.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        if (isActivityInForeground) {
            when (type) {
                TestResultGraphItemRecord.Type.DOWNLOAD -> {
                    binding.downloadChart.addResultGraphItems(
                        viewModel.state.downloadGraphData,
                        viewModel.state.testResult.get()?.networkType ?: NetworkTypeCompat.TYPE_UNKNOWN
                    )
                }
                TestResultGraphItemRecord.Type.UPLOAD -> {
                    binding.uploadChart.addResultGraphItems(
                        viewModel.state.uploadGraphData,
                        viewModel.state.testResult.get()?.networkType ?: NetworkTypeCompat.TYPE_UNKNOWN
                    )
                }
            }
        }
    }

    private fun refreshResults() {
        viewModel.loadTestResults()
        binding.swipeRefreshLayout.isRefreshing = true
    }

    private fun checkPlayServices(): Boolean {
        if (Build.MANUFACTURER.compareTo("Amazon", true) == 0) {
            return false
        }
        val gApi: GoogleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode: Int = gApi.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            return false
        }
        return true
    }

    override fun onMapReady(map: GoogleMap?) {
        googleMap = map
        googleMap?.uiSettings?.isScrollGesturesEnabled = false
        googleMap?.uiSettings?.isZoomGesturesEnabled = false
        googleMap?.uiSettings?.isRotateGesturesEnabled = false
    }

    override fun onBackPressed() {
        super.onBackPressed()
        when (viewModel.state.returnPoint) {
            ReturnPoint.HOME -> HomeActivity.startWithFragment(this, HomeActivity.Companion.HomeNavigationTarget.HOME_FRAGMENT_TO_SHOW)
            ReturnPoint.HISTORY -> HomeActivity.startWithFragment(this, HomeActivity.Companion.HomeNavigationTarget.HISTORY_FRAGMENT_TO_SHOW)
        }
    }

    enum class ReturnPoint {
        HOME,
        HISTORY;
    }

    companion object {

        private const val ZOOM_LEVEL = 15f
        private const val CIRCLE_RADIUS = 13.0
        private const val STROKE_WIDTH = 7f
        private const val ANCHOR_U = 0.5f
        private const val ANCHOR_V = 0.865f

        private const val KEY_TEST_UUID = "KEY_TEST_UUID"
        private const val KEY_RETURN_POINT = "KEY_RETURN_POINT"

        fun start(context: Context, testUUID: String, returnPoint: ReturnPoint) {
            val intent = Intent(context, ResultsActivity::class.java)
            intent.putExtra(KEY_TEST_UUID, testUUID)
            intent.putExtra(KEY_RETURN_POINT, returnPoint.name)
            context.startActivity(intent)
        }
    }
}