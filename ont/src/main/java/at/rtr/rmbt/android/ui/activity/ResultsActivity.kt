package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityResultsBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.adapter.QosResultAdapter
import at.rtr.rmbt.android.ui.adapter.ResultChartFragmentPagerAdapter
import at.rtr.rmbt.android.ui.adapter.ResultQoEAdapter
import at.rtr.rmbt.android.util.iconFromVector
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.ResultViewModel
import at.specure.data.NetworkTypeCompat
import at.specure.data.entity.TestResultGraphItemRecord
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import timber.log.Timber

class ResultsActivity : BaseActivity(), OnMapReadyCallback {

    private val viewModel: ResultViewModel by viewModelLazy()
    private lateinit var binding: ActivityResultsBinding
    private val adapter: ResultQoEAdapter by lazy { ResultQoEAdapter() }
    private val qosAdapter: QosResultAdapter by lazy { QosResultAdapter() }
    private lateinit var resultChartFragmentPagerAdapterDownload: ResultChartFragmentPagerAdapter
    private lateinit var resultChartFragmentPagerAdapterUpload: ResultChartFragmentPagerAdapter

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


            result?.testOpenUUID?.let {
                resultChartFragmentPagerAdapterDownload =
                    ResultChartFragmentPagerAdapter(supportFragmentManager, testUUID, result.networkType, TestResultGraphItemRecord.Type.DOWNLOAD)
                binding.viewPagerDownloadChart?.adapter = resultChartFragmentPagerAdapterDownload
                resultChartFragmentPagerAdapterUpload =
                    ResultChartFragmentPagerAdapter(supportFragmentManager, testUUID, result.networkType, TestResultGraphItemRecord.Type.UPLOAD)
                binding.viewPagerUploadChart?.adapter = resultChartFragmentPagerAdapterUpload
            }

            if (result?.latitude != null && result.longitude != null) {
                with(LatLng(result.latitude!!, result.longitude!!)) {
                    googleMap?.addCircle(
                        CircleOptions()
                            .center(this)
                            .fillColor(ContextCompat.getColor(this@ResultsActivity, R.color.map_circle_fill))
                            .strokeColor(ContextCompat.getColor(this@ResultsActivity, R.color.map_circle_stroke))
                            .strokeWidth(STROKE_WIDTH)
                            .radius(CIRCLE_RADIUS)
                    )

                    val icon = when (result.networkType) {
                        NetworkTypeCompat.TYPE_UNKNOWN -> R.drawable.ic_marker_empty
                        NetworkTypeCompat.TYPE_LAN,
                        NetworkTypeCompat.TYPE_BROWSER -> R.drawable.ic_marker_browser
                        NetworkTypeCompat.TYPE_WLAN -> R.drawable.ic_marker_wifi
                        NetworkTypeCompat.TYPE_5G_AVAILABLE,
                        NetworkTypeCompat.TYPE_4G -> R.drawable.ic_marker_4g
                        NetworkTypeCompat.TYPE_3G -> R.drawable.ic_marker_3g
                        NetworkTypeCompat.TYPE_2G -> R.drawable.ic_marker_2g
                        NetworkTypeCompat.TYPE_5G_NSA,
                        NetworkTypeCompat.TYPE_5G -> R.drawable.ic_marker_5g
                    }

                    googleMap?.addMarker(MarkerOptions().position(this).anchor(ANCHOR_U, ANCHOR_V).iconFromVector(this@ResultsActivity, icon))
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(this, ZOOM_LEVEL))
                    googleMap?.setOnMapClickListener {
                        DetailedFullscreenMapActivity.start(
                            this@ResultsActivity,
                            latitude,
                            longitude,
                            result.networkType
                        )
                    }
                    googleMap?.setOnMarkerClickListener { true }
                }
            }
        }
        viewModel.testResultDetailsLiveData.listen(this) {
            Timber.d("found ${it.size} rows of details")
            // todo: display result details
        }

        viewModel.qoeResultLiveData.listen(this) {
            viewModel.state.qoeRecords.set(it)
            adapter.submitList(it)
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

        viewModel.qosCategoryResultLiveData.listen(this) {
            viewModel.state.qosCategoryRecords.set(it)
            qosAdapter.submitList(it)
        }

        qosAdapter.actionCallback = {

            QosTestsSummaryActivity.start(this, it)
        }
        refreshResults()
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