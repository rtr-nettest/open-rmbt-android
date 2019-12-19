package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import androidx.core.content.ContextCompat
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityResultsBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.ResultViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class ResultsActivity : BaseActivity(), OnMapReadyCallback {

    private val viewModel: ResultViewModel by viewModelLazy()
    private lateinit var binding: ActivityResultsBinding

    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_results)
        binding.state = viewModel.state

        binding.map.onCreate(savedInstanceState)
        binding.map.getMapAsync(this)

        val testUUID = intent.getStringExtra(KEY_TEST_UUID)
        check(!testUUID.isNullOrEmpty()) { "TestUUID was not passed to result activity" }

        binding.map.setOnClickListener { DetailedFullscreenMapActivity.start(this, testUUID) }

        viewModel.state.testUUID = testUUID
        viewModel.testServerResultLiveData.listen(this) {
            viewModel.state.testResult.set(it)

            if (it?.latitude != null && it.longitude != null) {
                with(LatLng(it.latitude!!, it.longitude!!)) {
                    googleMap?.addCircle(
                        CircleOptions()
                            .center(this)
                            .fillColor(ContextCompat.getColor(this@ResultsActivity, R.color.map_circle_fill))
                            .strokeColor(ContextCompat.getColor(this@ResultsActivity, R.color.map_circle_stroke))
                            .strokeWidth(STROKE_WIDTH)
                            .radius(CIRCLE_RADIUS)
                    )
                    googleMap?.addMarker(MarkerOptions().position(this).icon(bitmapDescriptorFromVector(R.drawable.ic_marker_wifi))) // todo add network type logic, not received from server yet
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(this, ZOOM_LEVEL))
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        googleMap = map
        map?.let {
            with(map.uiSettings) {
                isScrollGesturesEnabled = false
                isZoomGesturesEnabled = false
                isRotateGesturesEnabled = false
            }
        }
    }

    override fun onStart() {
        super.onStart()
        binding.map.onStart()
        viewModel.loadTestResults()
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onStop() {
        super.onStop()
        binding.map.onStop()
    }

    override fun onPause() {
        binding.map.onPause()
        super.onPause()
    }

    private fun bitmapDescriptorFromVector(vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(this, vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

    companion object {

        private const val ZOOM_LEVEL = 15f
        private const val CIRCLE_RADIUS = 13.0
        private const val STROKE_WIDTH = 7f

        private const val KEY_TEST_UUID = "KEY_TEST_UUID"

        fun start(context: Context, testUUID: String) {
            val intent = Intent(context, ResultsActivity::class.java)
            intent.putExtra(KEY_TEST_UUID, testUUID)
            context.startActivity(intent)
        }
    }
}