package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import at.rmbt.client.control.data.MapPresentationType
import at.rmbt.client.control.data.MapStyleType
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityDetailedFullscreenMapBinding
import at.rtr.rmbt.android.map.wrapper.LatLngW
import at.rtr.rmbt.android.map.wrapper.MapWrapper
import at.rtr.rmbt.android.ui.dialog.MapLayersDialog
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.specure.data.NetworkTypeCompat
import kotlin.math.max

class DetailedFullscreenMapActivity : BaseActivity(), MapLayersDialog.Callback {

    private lateinit var binding: ActivityDetailedFullscreenMapBinding

    private lateinit var latLng: LatLngW
    private lateinit var networkType: NetworkTypeCompat

    private var currentMapStyle = MapStyleType.STANDARD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_detailed_fullscreen_map)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insetsSystemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val insetsDisplayCutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val insetsTouch = windowInsets.getInsets(WindowInsetsCompat.Type.tappableElement())

            val topSafeMargin = maxOf(insetsSystemBars.top, insetsDisplayCutout.top, insetsTouch.top)
            val lefSafetMargin = 16 + maxOf(insetsSystemBars.left, insetsDisplayCutout.left, insetsTouch.left)
            val rightSafeMargin = 16 + maxOf(insetsSystemBars.right, insetsDisplayCutout.right, insetsTouch.right)

            binding.closeFab.updateLayoutParams<MarginLayoutParams> {
                rightMargin = rightSafeMargin
                leftMargin = lefSafetMargin
                topMargin = topSafeMargin
            }

            binding.layersFab.updateLayoutParams<MarginLayoutParams> {
                rightMargin = rightSafeMargin
                leftMargin = lefSafetMargin
            }
            windowInsets
        }

        setTransparentStatusBar()
        window.changeStatusBarColor(ToolbarTheme.WHITE)

        if (!intent.hasExtra(KEY_LATITUDE) || !intent.hasExtra(KEY_LONGITUDE) || !intent.hasExtra(KEY_NETWORK_TYPE)) {
            throw IllegalArgumentException("Should start with latitude and network type")
        }
        latLng = LatLngW(intent.getDoubleExtra(KEY_LATITUDE, 0.0), intent.getDoubleExtra(KEY_LONGITUDE, 0.0))
        networkType = NetworkTypeCompat.values()[intent.getIntExtra(KEY_NETWORK_TYPE, 0)]

        binding.map.onCreate(savedInstanceState)
        binding.map.loadMapAsync {
            onMapReady()
        }

        binding.closeFab.setOnClickListener { finish() }
        binding.layersFab.setOnClickListener {
            MapLayersDialog.instance(
                activeStyle = currentMapStyle.ordinal,
                noSatelliteOrHybrid = !mapW().supportSatelliteAndHybridView()
            ).show(this)
        }
    }

    private fun mapW(): MapWrapper = binding.map.mapWrapper

    private fun onMapReady() {
        mapW().addCircle(
            latLng,
            ContextCompat.getColor(this@DetailedFullscreenMapActivity, R.color.map_circle_fill),
            ContextCompat.getColor(this@DetailedFullscreenMapActivity, R.color.map_circle_stroke),
            STROKE_WIDTH,
            CIRCLE_RADIUS
        )

        val icon = when (networkType) {
            NetworkTypeCompat.TYPE_BLUETOOTH,
            NetworkTypeCompat.TYPE_VPN,
            NetworkTypeCompat.TYPE_UNKNOWN -> R.drawable.ic_marker_empty
            NetworkTypeCompat.TYPE_LAN -> R.drawable.ic_marker_ethernet
            NetworkTypeCompat.TYPE_BROWSER -> R.drawable.ic_marker_browser
            NetworkTypeCompat.TYPE_WLAN -> R.drawable.ic_marker_wifi
            NetworkTypeCompat.TYPE_5G_AVAILABLE,
            NetworkTypeCompat.TYPE_4G -> R.drawable.ic_marker_4g
            NetworkTypeCompat.TYPE_3G -> R.drawable.ic_marker_3g
            NetworkTypeCompat.TYPE_2G -> R.drawable.ic_marker_2g
            NetworkTypeCompat.TYPE_5G_NSA,
            NetworkTypeCompat.TYPE_5G -> R.drawable.ic_marker_5g
        }

        mapW().run {
            addMarker(this@DetailedFullscreenMapActivity, latLng, ANCHOR_U, ANCHOR_V, icon)
            moveCamera(latLng, ZOOM_LEVEL)
        }
    }

    override fun onStart() {
        super.onStart()
        binding.map.onStart()
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

    companion object {

        private const val KEY_LATITUDE = "KEY_LATITUDE"
        private const val KEY_LONGITUDE = "KEY_LONGITUDE"
        private const val KEY_NETWORK_TYPE = "KEY_NETWORK_TYPE"

        private const val ZOOM_LEVEL = 15f
        private const val CIRCLE_RADIUS = 13.0
        private const val STROKE_WIDTH = 7f
        private const val ANCHOR_U = 0.5f
        private const val ANCHOR_V = 0.865f

        fun start(context: Context, latitude: Double, longitude: Double, networkType: NetworkTypeCompat) {
            val intent = Intent(context, DetailedFullscreenMapActivity::class.java).apply {
                putExtra(KEY_NETWORK_TYPE, networkType.ordinal)
                putExtra(KEY_LATITUDE, latitude)
                putExtra(KEY_LONGITUDE, longitude)
            }
            context.startActivity(intent)
        }
    }

    override fun onStyleSelected(style: MapStyleType) {
        currentMapStyle = style
        mapW().setMapStyleType(style)
        when (style) {
            MapStyleType.HYBRID -> {
                window.changeStatusBarColor(ToolbarTheme.BLUE)
            }
            MapStyleType.SATELLITE -> {
                window.changeStatusBarColor(ToolbarTheme.BLUE)
            }
            else -> {
                window.changeStatusBarColor(ToolbarTheme.WHITE)
            }
        }
    }

    override fun onTypeSelected(type: MapPresentationType) {
        // not reachable
    }
}
