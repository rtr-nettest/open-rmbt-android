package at.rtr.rmbt.android.ui.dialog

import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogCoverageSettingsBinding
import at.rtr.rmbt.android.di.Injector
import at.rtr.rmbt.android.util.addOnPropertyChanged
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.CoverageSettingsViewModel
import at.specure.data.entity.CoverageMeasurementSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max

const val MIN_LOCATION_ACCURACY_METERS = 3
const val MAX_LOCATION_ACCURACY_METERS = 30

class CoverageSettingsDialog : FullscreenDialog() {

    override val gravity: Int = Gravity.BOTTOM

    override val dimBackground: Boolean = false

    @Inject
    lateinit var viewModel: CoverageSettingsViewModel

    private lateinit var binding: DialogCoverageSettingsBinding

    private var durationTickerJob: Job? = null

    private val timeFormat by lazy { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    init {
        retainInstance = true
    }

    private val callback: Callback?
        get() = when {
            targetFragment is Callback -> targetFragment as Callback
            activity is Callback -> activity as Callback
            else -> null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Injector.inject(this)
        viewModel.onRestoreState(savedInstanceState)

        viewModel.coverageMeasurementDataLiveData.listen(this) {
            val currentSequenceNumber = it?.coverageMeasurementSession?.sequenceNumber ?: -1
            showConnectionCount(currentSequenceNumber + 1)
            showFencesCount(it?.fences?.size ?: 0)
            showIpVersion(it?.coverageMeasurementSession?.ipVersion ?: 0)
            showSegmentRegistration(it?.coverageMeasurementSession)
        }

        viewModel.unsubmittedPreviousSegmentsCountLiveData.listen(this) { count ->
            showUnsubmittedPreviousSegments(count)
        }
    }

    /**
     * Registration status of the CURRENT segment: the registration time once the session has a server
     * measurement id, otherwise "Waiting for registration".
     */
    private fun showSegmentRegistration(session: CoverageMeasurementSession?) {
        binding.labelSegmentRegistration.text = if (session?.serverMeasurementId != null) {
            getString(
                R.string.coverage_segment_registered,
                timeFormat.format(Date(session.startMeasurementResponseReceivedMillis))
            )
        } else {
            getString(R.string.coverage_segment_waiting_registration)
        }
    }

    private fun showUnsubmittedPreviousSegments(count: Int) {
        binding.labelUnsubmittedSegments.text = if (count > 0) {
            getString(R.string.coverage_segments_unsubmitted, count)
        } else {
            getString(R.string.coverage_segments_unsubmitted_none)
        }
    }

    private fun showFencesCount(i: Int) {
        binding.labelPointsCount.text = getString(R.string.text_fence_count, i)
    }

    private fun showConnectionCount(i: Int) {
        binding.labelConnectionCount.text = getString(R.string.text_connection_count, i)
    }

    private fun startDurationTicker() {
        durationTickerJob?.cancel()
        durationTickerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                showDuration()
                delay(1000)
            }
        }
    }

    private fun showDuration() {
        val startMillis = viewModel.coverageMeasurementDataLiveData.value
            ?.coverageMeasurementSession?.startTimeLoopMillis
        val elapsedMillis =
            if (startMillis == null || startMillis <= 0L) 0L else System.currentTimeMillis() - startMillis
        binding.labelDuration.text = getString(R.string.coverage_duration_label, formatDuration(elapsedMillis))
    }

    private fun formatDuration(elapsedMillis: Long): String {
        val totalSeconds = (elapsedMillis / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return when {
            hours > 0L -> "${hours}h ${minutes}min ${seconds}s"
            minutes > 0L -> "${minutes}min ${seconds}s"
            else -> "${seconds}s"
        }
    }

    private fun showIpVersion(i: Int?) {
        val ipText = when (i) {
            4 -> getString(R.string.ipv4_short)
            6 -> getString(R.string.ipv6_short)
            else -> getString(R.string.text_unknown)
        }
        binding.labelIpVersion.text = ipText
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_coverage_settings, container, false)
        binding.state = viewModel.state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
                val insetsSystemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                val insetsDisplayCutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
                val topSafe = max(insetsSystemBars.top, insetsDisplayCutout.top)
                val leftSafe = max(insetsSystemBars.left, insetsDisplayCutout.left)
                val rightSafe = max(insetsSystemBars.right, insetsDisplayCutout.right)
                val bottomSafe = max(insetsSystemBars.bottom, insetsDisplayCutout.bottom)
                v.updateLayoutParams<MarginLayoutParams> {
                    topMargin = topSafe
                    leftMargin = leftSafe
                    rightMargin = rightSafe
                    bottomMargin = bottomSafe
                }
                WindowInsetsCompat.CONSUMED
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAccuracyRadiusItem()

        startDurationTicker()

        // The segment registration status and the outstanding-segments count are expert-only.
        val expertVisibility = if (viewModel.isExpertModeEnabled) View.VISIBLE else View.GONE
        binding.labelSegmentRegistration.visibility = expertVisibility
        binding.labelUnsubmittedSegments.visibility = expertVisibility

        binding.iconClose.setOnClickListener { dismiss() }
    }

    private fun setAccuracyRadiusItem() {
        val initialValue = (viewModel.state.locationAccuracyMeters.get() ?: MIN_LOCATION_ACCURACY_METERS).coerceIn(MIN_LOCATION_ACCURACY_METERS, MAX_LOCATION_ACCURACY_METERS)
        binding.accuracyRadiusMeters.text = getString(R.string.text_meters, initialValue)
        viewModel.state.locationAccuracyMeters.addOnPropertyChanged { property ->
            binding.accuracyRadiusMeters.text = getString(R.string.text_meters, property.get())
            binding.accuracyRadiusSlider.contentDescription = getString(R.string.accuracy_radius_slider_content_description, property.get())
        }

        binding.labelAccuracyRadiusMinMeters.text = getString(R.string.text_meters, MIN_LOCATION_ACCURACY_METERS)
        binding.labelAccuracyRadiusMaxMeters.text = getString(R.string.text_meters, MAX_LOCATION_ACCURACY_METERS)

        binding.accuracyRadiusSlider.value = initialValue.toFloat()
        binding.accuracyRadiusSlider.valueTo = MAX_LOCATION_ACCURACY_METERS.toFloat()
        binding.accuracyRadiusSlider.valueFrom = MIN_LOCATION_ACCURACY_METERS.toFloat()
        binding.accuracyRadiusSlider.addOnChangeListener { slider, value, fromUser ->
            viewModel.state.locationAccuracyMeters.set(value.toInt())
            callback?.onFenceOrAccuracyUpdated()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.onSaveState(outState)
    }

    companion object {

        fun show(fragmentManager: FragmentManager) {
            with(fragmentManager.beginTransaction()) {
                val tag = CoverageSettingsDialog::class.java.name
                val prev = fragmentManager.findFragmentByTag(CoverageSettingsDialog::class.java.name)
                if (prev != null) {
                    remove(prev)
                }
                addToBackStack(null)
                CoverageSettingsDialog().show(fragmentManager, tag)
            }
        }
    }

    interface Callback {
        fun onFenceOrAccuracyUpdated()
    }
}