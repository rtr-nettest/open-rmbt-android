package at.rtr.rmbt.android.ui.dialog

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogLocationInfoBinding
import at.rtr.rmbt.android.di.Injector
import at.rtr.rmbt.android.util.listen
import at.specure.location.LocationState
import at.specure.location.LocationWatcher
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.max

class LocationInfoDialog : FullscreenDialog() {

    private lateinit var binding: DialogLocationInfoBinding

    @Inject
    lateinit var locationWatcher: LocationWatcher

    private var locationAge = 0L

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
        }
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

        fun instance(): FullscreenDialog = LocationInfoDialog()
    }
}