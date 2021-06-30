package at.rtr.rmbt.android.ui.dialog

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import at.rmbt.client.control.IpProtocol
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogNetworkInfoBinding
import at.rtr.rmbt.android.di.Injector
import at.rtr.rmbt.android.ui.activity.MeasurementServerSelectionActivity
import at.rtr.rmbt.android.util.formatAccuracy
import at.rtr.rmbt.android.util.formatAgeString
import at.rtr.rmbt.android.util.formatAltitude
import at.rtr.rmbt.android.util.formatCoordinate
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.util.listenNonNull
import at.rtr.rmbt.android.util.setTechnologyIcon
import at.specure.data.MeasurementServers
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.ip.IpInfo
import at.specure.info.ip.IpStatus
import at.specure.info.ip.IpV4ChangeLiveData
import at.specure.info.ip.IpV6ChangeLiveData
import at.specure.info.network.ActiveNetworkLiveData
import at.specure.info.network.NetworkInfo
import at.specure.info.network.WifiNetworkInfo
import at.specure.info.strength.SignalStrengthInfoLte
import at.specure.info.strength.SignalStrengthLiveData
import at.specure.location.LocationInfo
import at.specure.location.LocationWatcher
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt
import timber.log.Timber

class NetworkInfoDialog : BottomSheetDialogFragment() {

    @Inject
    lateinit var activeNetworkLiveData: ActiveNetworkLiveData

    @Inject
    lateinit var signalStrengthLiveData: SignalStrengthLiveData

    @Inject
    lateinit var locationWatcher: LocationWatcher

    @Inject
    lateinit var ipV4ChangeLiveData: IpV4ChangeLiveData

    @Inject
    lateinit var ipV6ChangeLiveData: IpV6ChangeLiveData

    @Inject
    lateinit var measurementServers: MeasurementServers

    private lateinit var binding: DialogNetworkInfoBinding

    private var locationAge = 0L
    private val updateHandler = Handler()
    private val ageUpdateRunnable = Runnable {
        locationAge += TimeUnit.MILLISECONDS.toNanos(1000)
        val formatAge = TimeUnit.NANOSECONDS.toSeconds(locationAge).toString()
        if (isAdded) {
            binding.textAge.text = requireContext().getString(R.string.location_dialog_age, formatAge)
            scheduleUpdate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Injector.inject(this)

        // bottom sheet round corners can be obtained but the while background appears to remove that we need to add this.
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.AppBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DialogNetworkInfoBinding.inflate(layoutInflater)
        isCancelable = false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.close.setOnClickListener {
            dismiss()
        }

        observeIp()
        observeActiveNetworkUpdates()
        observeSignalUpdates()
        observeLocationUpdates()
    }

    override fun onStart() {
        super.onStart()
        observeMeasurementServers()

        view?.let { view ->
            view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)

                    (dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? FrameLayout)?.let {
                        val behavior = BottomSheetBehavior.from(it)
                        behavior.isGestureInsetBottomIgnored = true
                        behavior.peekHeight = (view.measuredHeight * 0.75).roundToInt()
                    }
                }
            })
        }
    }

    private fun observeMeasurementServers() {
        binding.textServer.text = measurementServers.selectedMeasurementServer?.name
        binding.groupServer.visibility = if (!measurementServers.measurementServers.isNullOrEmpty()) View.VISIBLE else View.GONE
        binding.linkServer.setOnClickListener { startActivity(Intent(requireContext(), MeasurementServerSelectionActivity::class.java)) }
    }

    private fun observeIp() {
        ipV4ChangeLiveData.listen(viewLifecycleOwner) { ipInfo ->
            ipInfo?.let { setIpViewBackground(binding.infoIpv4.label, it, getString(R.string.text_label_ipv4)) }
            binding.infoIpv4.textNotAvailable.visibility =
                if (ipInfo != null && (ipInfo.ipStatus == IpStatus.NO_ADDRESS || ipInfo.ipStatus == IpStatus.NO_INFO)) View.VISIBLE else View.GONE

            if (ipInfo != null && ipInfo.protocol == IpProtocol.V4 && ipInfo.publicAddress != null && ipInfo.ipStatus != IpStatus.NO_ADDRESS) {
                binding.infoIpv4.textPublic.text = ipInfo.publicAddress
                binding.infoIpv4.textPublic.visibility = View.VISIBLE
                binding.infoIpv4.labelPublic.visibility = View.VISIBLE
            } else {
                binding.infoIpv4.textPublic.visibility = View.GONE
                binding.infoIpv4.labelPublic.visibility = View.GONE
            }

            if (ipInfo?.privateAddress != null && ipInfo.ipStatus != IpStatus.NO_ADDRESS) {
                binding.infoIpv4.textPrivate.text = ipInfo.privateAddress
                binding.infoIpv4.textPrivate.visibility = View.VISIBLE
                binding.infoIpv4.labelPrivate.visibility = View.VISIBLE
            } else {
                binding.infoIpv4.textPrivate.visibility = View.GONE
                binding.infoIpv4.labelPrivate.visibility = View.GONE
            }

            binding.infoIpv4.textPublicV6.visibility = View.GONE
            binding.infoIpv4.labelPublicV6.visibility = View.GONE
            binding.infoIpv4.executePendingBindings()
        }

        ipV6ChangeLiveData.listen(viewLifecycleOwner) { ipInfo ->
            ipInfo?.let { setIpViewBackground(binding.infoIpv6.label, it, getString(R.string.text_label_ipv6)) }
            binding.infoIpv6.textNotAvailable.visibility =
                if (ipInfo != null && (ipInfo.ipStatus == IpStatus.NO_ADDRESS || ipInfo.ipStatus == IpStatus.NO_INFO)) View.VISIBLE else View.GONE

            if (ipInfo != null && ipInfo.protocol == IpProtocol.V6 && ipInfo.publicAddress != null && ipInfo.ipStatus != IpStatus.NO_ADDRESS) {
                binding.infoIpv6.textPublicV6.text = ipInfo.publicAddress
                binding.infoIpv6.textPublicV6.visibility = View.VISIBLE
                binding.infoIpv6.labelPublicV6.visibility = View.VISIBLE
            } else {
                binding.infoIpv6.textPublicV6.visibility = View.GONE
                binding.infoIpv6.labelPublicV6.visibility = View.GONE
            }

            binding.infoIpv6.textPrivate.visibility = View.GONE
            binding.infoIpv6.labelPrivate.visibility = View.GONE
            binding.infoIpv6.textPublic.visibility = View.GONE
            binding.infoIpv6.labelPublic.visibility = View.GONE
        }
    }

    private fun setIpViewBackground(view: TextView, ipInfo: IpInfo, label: String) {
        view.text = label

        when (ipInfo.ipStatus) {
            IpStatus.NO_INFO, IpStatus.NO_ADDRESS -> {
                view.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_ip_red))
                view.setTextColor(Color.WHITE)
            }
            IpStatus.NO_NAT -> {
                view.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_ip_green))
                view.setTextColor(Color.BLACK)
            }
            else -> {
                view.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_ip_yellow))
                view.setTextColor(Color.BLACK)
            }
        }
    }

    private fun observeActiveNetworkUpdates() {
        activeNetworkLiveData.listenNonNull(viewLifecycleOwner, this::setNetworkInfo)
    }

    @SuppressLint("SetTextI18n")
    private fun observeSignalUpdates() {
        signalStrengthLiveData.listen(viewLifecycleOwner) { signal ->
            signal?.let {
                it.networkInfo?.let { it1 -> setNetworkInfo(it1) }
                binding.textSignal.text = getString(R.string.home_signal_value, it.signalStrengthInfo?.value)
            } ?: run {
                binding.textSignal.visibility = View.GONE
                binding.labelSignal.visibility = View.GONE
            }

            if (signal?.signalStrengthInfo is SignalStrengthInfoLte && (signal.signalStrengthInfo as SignalStrengthInfoLte).timingAdvance != null) {
                val ta = (signal.signalStrengthInfo as SignalStrengthInfoLte).timingAdvance!!
                binding.textTimingAdvance.text = "$ta ~(${ta * 78} m)"
            } else {
                binding.textTimingAdvance.visibility = View.GONE
                binding.labelTimingAdvance.visibility = View.GONE
            }

            signal?.signalStrengthInfo?.rsrq?.let {
                binding.textSignalStrengthExtra.text = "${signal.signalStrengthInfo?.rsrq} dB"
            } ?: run {
                binding.textSignalStrengthExtra.visibility = View.GONE
                binding.labelSignalStrengthExtra.visibility = View.GONE
            }
        }
    }

    private fun setNetworkInfo(info: NetworkInfo) {
        binding.textNetworkName.text = info.name
        binding.textNetworkType.setTechnologyIcon(info)

        var name: String? = null
        var number: String? = null
        if (info is CellNetworkInfo && info.band != null) {
            val band = info.band!!
            number = band.channel.toString()
            name = "${band.band} (${band.name})"
            binding.labelChannelNumber.text = band.channelAttribution.name
        } else if (info is WifiNetworkInfo) {
            val band = info.band
            number = "${band.channelNumber} (${band.frequency} MHz)"
            name = band.informalName
            binding.labelChannelNumber.text = getString(R.string.dialog_signal_info_channel)
        }

        name?.let {
            binding.textChannelName.text = it
        } ?: run {
            binding.labelChannelName.visibility = View.GONE
            binding.textChannelName.visibility = View.GONE
        }

        number?.let {
            binding.textChannelNumber.text = it
        } ?: run {
            binding.labelChannelNumber.visibility = View.GONE
            binding.textChannelNumber.visibility = View.GONE
        }
    }

    private fun observeLocationUpdates() {
        locationWatcher.liveData.listen(viewLifecycleOwner) {
            it?.let { locationInfo ->
                binding.groupLocation.visibility = View.VISIBLE
                val latlng = LatLng(it.latitude, it.longitude)
                val locationName = fetchLocationName(latlng)
                locationName?.let {
                    binding.textLocationName.text = it
                } ?: run {
                    binding.textLocationName.visibility = View.GONE
                    binding.labelLocationName.visibility = View.GONE
                }

                val position = fetchLocationPosition(latlng, it.latitudeDirection, it.longitudeDirection)
                binding.textCoordinates.text = position

                locationInfo.formatAccuracy()?.let {
                    binding.textAccuracy.text = it
                } ?: run {
                    binding.textAccuracy.visibility = View.GONE
                    binding.labelAccuracy.visibility = View.GONE
                }

                binding.textAge.text = getString(R.string.location_dialog_age, locationInfo.formatAgeString())
                locationAge = it.ageNanos
                scheduleUpdate()

                binding.textSource.text = locationInfo.provider
                locationInfo.satellites?.let { binding.textSatellites.text = it.toString() } ?: run {
                    binding.textSatellites.visibility = View.GONE
                    binding.labelSatellites.visibility = View.GONE
                }

                fetchAltitude(locationInfo)?.let { binding.textAltitude.text = it } ?: {
                    binding.textAltitude.visibility = View.INVISIBLE
                    binding.labelAltitude.visibility = View.INVISIBLE
                }
            } ?: run {
                binding.groupLocation.visibility = View.GONE
            }
        }
    }

    private fun fetchLocationName(latlng: LatLng): String? {
        return try {
            val addresses = Geocoder(requireContext(), Locale.getDefault()).getFromLocation(latlng.latitude, latlng.longitude, 1)
            if (addresses.isNotEmpty()) addresses[0].getAddressLine(0) else null
        } catch (ex: Exception) {
            Timber.e("Geocoder exception")
            null
        }
    }

    private fun fetchLocationPosition(
        latlng: LatLng,
        latitudeDirection: LocationInfo.LocationCardinalDirections,
        longitudeDirection: LocationInfo.LocationCardinalDirections
    ): String {
        val formattedLatitude = latlng.latitude.formatCoordinate()
        val formattedLongitude = latlng.longitude.formatCoordinate()
        val latitudeText = if (latitudeDirection == LocationInfo.LocationCardinalDirections.NORTH) {
            getString(R.string.location_location_direction_n, formattedLatitude)
        } else {
            getString(R.string.location_location_direction_s, formattedLatitude)
        }

        val longitudeText = if (longitudeDirection == LocationInfo.LocationCardinalDirections.EAST) {
            getString(R.string.location_location_direction_e, formattedLongitude)
        } else {
            getString(R.string.location_location_direction_w, formattedLongitude)
        }

        return getString(R.string.location_dialog_position, latitudeText, longitudeText)
    }

    private fun fetchAltitude(locationInfo: LocationInfo): String? {
        val formatAltitude = locationInfo.formatAltitude()
        return if (formatAltitude == null) {
            null
        } else {
            getString(R.string.location_dialog_accuracy, formatAltitude)
        }
    }

    private fun scheduleUpdate() {
        updateHandler.removeCallbacks(ageUpdateRunnable)
        updateHandler.postDelayed(ageUpdateRunnable, 1000)
    }

    companion object {
        fun show(fragmentManager: FragmentManager) {
            NetworkInfoDialog().show(fragmentManager, null)
        }
    }
}