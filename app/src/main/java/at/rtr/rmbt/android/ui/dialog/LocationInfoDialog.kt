package at.rtr.rmbt.android.ui.dialog

import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogLocationInfoBinding
import at.rtr.rmbt.android.di.Injector
import at.rtr.rmbt.android.util.RasterIdSource
import at.rtr.rmbt.android.util.listen
import at.specure.location.LocationInfo
import at.specure.location.LocationState
import at.specure.location.LocationWatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.max

class LocationInfoDialog : FullscreenDialog() {

    private lateinit var binding: DialogLocationInfoBinding

    @Inject
    lateinit var locationWatcher: LocationWatcher

    private var locationAge = 0L

    // Last coordinate the coverage raster ids were fetched for, to avoid re-requesting on every fix.
    private var lastRasterLat: Double? = null
    private var lastRasterLng: Double? = null
    private var rasterFetchJob: Job? = null

    private val ageUpdateRunnable = Runnable {
        locationAge += TimeUnit.MILLISECONDS.toNanos(1000)
        val formatAge = TimeUnit.NANOSECONDS.toSeconds(locationAge).toString()
        binding.textAge.text = requireContext().getString(R.string.location_dialog_age, formatAge)
        scheduleUpdate()
    }

    private val updateHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Injector.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_location_info, container, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
                val insetsSystemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                val insetsDisplayCutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
                val topSafe = max(insetsSystemBars.top, insetsDisplayCutout.top)
                val leftSafe = max(insetsSystemBars.left, insetsDisplayCutout.left)
                val rightSafe = max(insetsSystemBars.right, insetsDisplayCutout.right)
                val bottomSafe = max(insetsSystemBars.bottom, insetsDisplayCutout.bottom)

                v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    rightMargin = rightSafe
                    leftMargin = leftSafe
                    topMargin = topSafe
                    bottomMargin = bottomSafe
                }
                WindowInsetsCompat.CONSUMED
            }
        }

        return binding.root
    }

    @Suppress("SENSELESS_COMPARISON")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.iconClose.setOnClickListener {
            this@LocationInfoDialog.dismiss()
        }

        locationWatcher.stateLiveData.listen(this) {
            if (it != LocationState.ENABLED) dismiss()
        }

        locationWatcher.liveData.listen(this) {
            binding.locationInfo = it
            if (locationAge == null) {
                updateHandler.removeCallbacks(ageUpdateRunnable)
                locationAge = 0
            } else {
                locationAge = it?.ageNanos ?: 0
                scheduleUpdate()
            }
            maybeFetchRasterIds(it)
        }
    }

    /**
     * Requests the coverage raster ids for the current coordinate and shows them in the overlay.
     * Only fetches on the first fix and after moving more than one raster cell (100 m), so a stream
     * of fixes doesn't spam the endpoint.
     */
    private fun maybeFetchRasterIds(info: LocationInfo?) {
        if (info == null) return
        val lat = info.latitude
        val lng = info.longitude

        val prevLat = lastRasterLat
        val prevLng = lastRasterLng
        val movedEnough = if (prevLat == null || prevLng == null) {
            true
        } else {
            val results = FloatArray(1)
            Location.distanceBetween(prevLat, prevLng, lat, lng, results)
            results[0] > RASTER_REFETCH_DISTANCE_METERS
        }
        if (!movedEnough) return

        lastRasterLat = lat
        lastRasterLng = lng
        rasterFetchJob?.cancel()
        rasterFetchJob = viewLifecycleOwner.lifecycleScope.launch {
            showRasterIds(RasterIdSource.fetch(lat, lng))
        }
    }

    private fun showRasterIds(entries: List<RasterIdSource.RasterEntry>) {
        binding.rasterContainer.removeAllViews()
        if (entries.isEmpty()) {
            binding.labelRasterTitle.visibility = View.GONE
            return
        }
        val inflater = LayoutInflater.from(requireContext())
        for (entry in entries) {
            val row = inflater.inflate(R.layout.item_location_raster, binding.rasterContainer, false)
            row.findViewById<TextView>(R.id.labelRaster).setText(labelForRasterKey(entry.key))
            row.findViewById<TextView>(R.id.valueRaster).text = entry.value
            binding.rasterContainer.addView(row)
        }
        binding.labelRasterTitle.visibility = View.VISIBLE
    }

    private fun labelForRasterKey(key: String): Int = when (key) {
        "r100" -> R.string.location_dialog_label_raster_100
        "short_id100" -> R.string.location_dialog_label_raster_short_100
        "long_id100" -> R.string.location_dialog_label_raster_long_100
        "r250" -> R.string.location_dialog_label_raster_250
        "short_id250" -> R.string.location_dialog_label_raster_short_250
        "long_id250" -> R.string.location_dialog_label_raster_long_250
        else -> R.string.location_dialog_label_raster_title
    }

    private fun scheduleUpdate() {
        updateHandler.removeCallbacks(ageUpdateRunnable)
        updateHandler.postDelayed(ageUpdateRunnable, 1000)
    }

    override fun onStop() {
        super.onStop()
        updateHandler.removeCallbacks(ageUpdateRunnable)
    }

    companion object {

        // Raster cells are 100 m; re-fetch the ids only after moving beyond one cell.
        private const val RASTER_REFETCH_DISTANCE_METERS = 100f

        fun instance(): FullscreenDialog = LocationInfoDialog()
    }
}