package at.rtr.rmbt.android.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import at.rmbt.client.control.IpProtocol
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.ui.view.ProgressBar
import at.rtr.rmbt.android.ui.view.ResultBar
import at.rtr.rmbt.android.ui.view.SpeedLineChart
import at.rtr.rmbt.android.ui.view.WaveView
import at.rtr.rmbt.android.ui.view.curve.MeasurementCurveLayout
import at.rtr.rmbt.android.util.InfoWindowStatus
import at.rtr.rmbt.android.util.format
import at.specure.data.Classification
import at.specure.data.NetworkTypeCompat
import at.specure.data.ServerNetworkType
import at.specure.data.entity.GraphItemRecord
import at.specure.info.TransportType
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.cell.CellTechnology
import at.specure.info.ip.IpInfo
import at.specure.info.ip.IpStatus
import at.specure.info.network.*
import at.specure.info.strength.SignalStrengthInfo
import at.specure.measurement.MeasurementState
import at.specure.result.QoECategory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.Calendar
import java.util.Locale
import java.util.Date
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@BindingAdapter("intText")
fun intText(textView: TextView, value: Int) {
    textView.text = value.toString()
}

/**
 * A Binding adapter that is used for change visibility of view
 */
@BindingAdapter("visibleOrGone")
fun View.setVisibleOrGone(show: Boolean) {
    visibility = if (show) View.VISIBLE else View.GONE
}

private fun AppCompatTextView.prepareSignalLabel(
    mainText: String,
    networkType: NetworkInfo?,
    networkTypeSecondary: NetworkInfo?,
    signal: Int?,
    signalSecondary: Int?
): StringBuilder {
    val mainType = when (networkType) {
        is CellNetworkInfo -> {
            if (networkType.cellType == CellTechnology.CONNECTION_UNKNOWN) {
                ""
            } else {
                networkType.cellType.displayName
            }
        }
        else -> {
            ""
        }
    }
    val secondaryType = when (networkTypeSecondary) {
        is CellNetworkInfo -> {
            if (networkTypeSecondary.cellType == CellTechnology.CONNECTION_UNKNOWN) {
                ""
            } else {
                networkTypeSecondary.cellType.displayName
            }
        }
        else -> {
            ""
        }
    }

    val signalValues = extractSignalValues(this.context, signal, signalSecondary, networkType)

    val builder = StringBuilder()
    if (mainText.isNotEmpty()) {
        builder.append(mainText)
    }
    if (networkType is CellNetworkInfo && networkType.networkType == MobileNetworkType.NR_NSA) {
        if (mainType.isNotEmpty()) {
            builder.append(" $mainType")
        }
        if (secondaryType.isNotEmpty() && signalValues.contains("/")) {
            builder.append("/$secondaryType")
        }
    }
    return builder
}

enum class HomeScreenLabelType(val value: String) {
    SIGNAL("signal"),
    FREQUENCY("frequency")
}

/**
 * A binding adapter that is used for show network technology
 */
@SuppressLint("SetTextI18n")
@BindingAdapter(
    value = ["mainText", "networkType", "networkTypeSecondary", "labelType", "signal", "signalSecondary"],
    requireAll = true
)
fun AppCompatTextView.appendType(
    mainText: String,
    networkType: NetworkInfo?,
    networkTypeSecondary: NetworkInfo?,
    labelType: String,
    signal: Int?,
    signalSecondary: Int?
) {
    val builder = when (labelType) {
        HomeScreenLabelType.FREQUENCY.value -> prepareFrequencyLabel(
            mainText,
            networkType,
            networkTypeSecondary
        )
        HomeScreenLabelType.SIGNAL.value -> prepareSignalLabel(
            mainText,
            networkType,
            networkTypeSecondary,
            signal,
            signalSecondary
        )
        else -> StringBuilder().append(mainText)
    }
    text = builder.toString()
}

private fun AppCompatTextView.prepareFrequencyLabel(
    mainText: String,
    networkType: NetworkInfo?,
    networkTypeSecondary: NetworkInfo?
): StringBuilder {
    val mainType = when (networkType) {
        is CellNetworkInfo -> {
            if (networkType.cellType == CellTechnology.CONNECTION_UNKNOWN) {
                ""
            } else {
                networkType.cellType.displayName
            }
        }
        else -> {
            ""
        }
    }
    val secondaryType = when (networkTypeSecondary) {
        is CellNetworkInfo -> {
            if (networkTypeSecondary.cellType == CellTechnology.CONNECTION_UNKNOWN) {
                ""
            } else {
                networkTypeSecondary.cellType.displayName
            }
        }
        else -> {
            ""
        }
    }
    val builder = StringBuilder()
    if (mainText.isNotEmpty()) {
        builder.append(mainText)
    }
    if (networkType is CellNetworkInfo && networkType.networkType == MobileNetworkType.NR_NSA) {
        if (mainType.isNotEmpty()) {
            builder.append(" $mainType")
        }
        if (extractFrequency(
                networkType,
                networkTypeSecondary,
                this.context
            )?.contains("/") == true
        ) {
            if (secondaryType.isNotEmpty()) {
                builder.append("/$secondaryType")
            }
        }
    }
    return builder
}

/**
 * A binding adapter that is used for show signal
 */
@BindingAdapter("simpleSignal")
fun AppCompatTextView.setSignal(
    signal: Int?
) {
    text = if (signal != null) {
        String.format(context.getString(R.string.home_signal_value), signal)
    } else {
        ""
    }
}

/**
 * A binding adapter that is used for show signal
 */
@BindingAdapter("signal", "signalSecondary", "signalNetworkInfo")
fun AppCompatTextView.setSignal(
    signal: Int?,
    signalSecondary: Int?,
    signalNetworkInfo: NetworkInfo?
) {
    text = extractSignalValues(this.context, signal, signalSecondary, signalNetworkInfo)
}

private fun extractSignalValues(
    context: Context,
    signal: Int?,
    signalSecondary: Int?,
    signalNetworkInfo: NetworkInfo?
) =
    if (signal != null && signalSecondary != null && signalNetworkInfo is CellNetworkInfo && signalNetworkInfo.networkType == MobileNetworkType.NR_NSA) {
        String.format(
            context.getString(R.string.home_signal_secondary_value),
            signal,
            signalSecondary
        )
    } else if (signal != null) {
        String.format(context.getString(R.string.home_signal_value), signal)
    } else {
        ""
    }

/**
 * A binding adapter that is used for show frequency
 */
@BindingAdapter("frequency", "frequencySecondary")
fun AppCompatTextView.setFrequency(networkInfo: NetworkInfo?, secondaryNetworkInfo: NetworkInfo?) {

    text = when (networkInfo) {
        is WifiNetworkInfo -> networkInfo.band.name
        is CellNetworkInfo -> {
            // we display secondary signal only for NR_NSA type of the network
            extractFrequency(networkInfo, secondaryNetworkInfo, this.context)
        }
        else -> ""
    }
}

private fun extractFrequency(
    networkInfo: CellNetworkInfo,
    secondaryNetworkInfo: NetworkInfo?,
    context: Context
): String? {
    return if (secondaryNetworkInfo == null || networkInfo.networkType != MobileNetworkType.NR_NSA) {
        if (networkInfo.band?.name?.contains("MHz") == true) {
            networkInfo.band?.name?.removeSuffix("MHz")
        } else {
            String.format(
                context.getString(R.string.home_frequency_value),
                networkInfo.band?.name
            )
        }
    } else {
        val builder = StringBuilder()
        if (networkInfo.band?.name?.contains("MHz") == true) {
            builder.append(networkInfo.band?.name?.removeSuffix("MHz"))
        } else {
            builder.append(if (networkInfo.band?.name.isNullOrEmpty()) "" else networkInfo.band?.name)
        }
        if ((secondaryNetworkInfo is CellNetworkInfo) && secondaryNetworkInfo.band?.name?.isNullOrEmpty() == false) {
            builder.append("/")
            if (secondaryNetworkInfo.band?.name?.contains("MHz") == true) {
                builder.append(secondaryNetworkInfo.band?.name?.removeSuffix("MHz"))
            } else {
                builder.append(if (secondaryNetworkInfo.band?.name.isNullOrEmpty()) "" else secondaryNetworkInfo.band?.name)
            }
        }
        String.format(context.getString(R.string.home_frequency_value), builder.toString())
    }
}

/**
 * A binding adapter that is used for show frequency
 */
@BindingAdapter("frequencyVisibility")
fun View.frequencyVisibility(networkInfo: NetworkInfo?) {
    visibility =
        if (networkInfo != null && networkInfo is CellNetworkInfo && networkInfo.band == null) {
            View.GONE
        } else {
            View.VISIBLE
        }
}

/**
 * A binding adapter that is used for show information window
 * Information window display when user not doing action for 2 second
 */
@BindingAdapter(value = ["isConnected", "infoWindowStatus"], requireAll = true)
fun AppCompatTextView.showPopup(isConnected: Boolean, infoWindowStatus: InfoWindowStatus) {
    visibility = if (isConnected) {
        when (infoWindowStatus) {
            InfoWindowStatus.NONE -> {
                View.GONE
            }
            InfoWindowStatus.VISIBLE -> {
                View.VISIBLE
            }
            InfoWindowStatus.GONE -> {
                View.GONE
            }
        }
    } else {
        View.GONE
    }
}

/**
 * A binding adapter that is used for show network icon based on network type (WIFI/MOBILE),
 * and signalLevel(0..4)
 */
@BindingAdapter("signalLevel", "connected", "networkTransportType")
fun AppCompatImageButton.setIcon(signalStrengthInfo: SignalStrengthInfo?, connected: Boolean, networkTransportType: TransportType?) {

    if (signalStrengthInfo != null) {

        val transportType: TransportType? = signalStrengthInfo.transport

        when (signalStrengthInfo.signalLevel) {

            2 -> {
                if (transportType == TransportType.WIFI)
                    setImageResource(R.drawable.ic_wifi_2)
                else if (transportType == TransportType.CELLULAR)
                    setImageResource(R.drawable.ic_mobile_2)
            }
            3 -> {
                if (transportType == TransportType.WIFI)
                    setImageResource(R.drawable.ic_wifi_3)
                else if (transportType == TransportType.CELLULAR)
                    setImageResource(R.drawable.ic_mobile_3)
            }
            4 -> {
                if (transportType == TransportType.WIFI)
                    setImageResource(R.drawable.ic_wifi_4)
                else if (transportType == TransportType.CELLULAR)
                    setImageResource(R.drawable.ic_mobile_4)
            }
            else -> {
                if (transportType == TransportType.WIFI)
                    setImageResource(R.drawable.ic_wifi_1)
                else if (transportType == TransportType.CELLULAR)
                    setImageResource(R.drawable.ic_mobile_1)
            }
        }
    } else {
        if (connected) {
            when (networkTransportType) {
                TransportType.ETHERNET -> setImageResource(R.drawable.ic_ethernet_home)
                else -> setImageResource(R.drawable.ic_signal_unknown)
            }
        } else {
            setImageResource(R.drawable.ic_no_internet)
        }
    }
}

/**
 * A binding adapter that is used for show network type
 */
@BindingAdapter("networkType")
fun AppCompatTextView.setNetworkType(detailedNetworkInfo: DetailedNetworkInfo?) {

    /**
     *  display "LTE" (true) or "4G/LTE" (false)
     */
    val shortDisplayOfTechnology = true

    text = extractTechnologyString(detailedNetworkInfo, shortDisplayOfTechnology)
}

private fun AppCompatTextView.extractTechnologyString(
    detailedNetworkInfo: DetailedNetworkInfo?,
    shortDisplayOfTechnology: Boolean
) = when (detailedNetworkInfo?.networkInfo) {
    is EthernetNetworkInfo -> context.getString(R.string.home_ethernet)
    is WifiNetworkInfo -> context.getString(R.string.home_wifi)
    is VpnNetworkInfo -> context.getString(R.string.home_vpn)
    is BluetoothNetworkInfo -> context.getString(R.string.home_bluetooth)
    is CellNetworkInfo -> {
        val technology =
            CellTechnology.fromMobileNetworkType((detailedNetworkInfo.networkInfo as CellNetworkInfo).networkType)?.displayName
        Timber.d("NM network type to display: ${(detailedNetworkInfo.networkInfo as CellNetworkInfo).networkType.displayName}")
        if (shortDisplayOfTechnology || technology == null) {
            (detailedNetworkInfo.networkInfo as CellNetworkInfo).networkType.displayName
        } else {
            "$technology/${(detailedNetworkInfo.networkInfo as CellNetworkInfo).networkType.displayName}"
        }
    }
    else -> ""
}

/**
 * A binding adapter that is used for show technology icon for mobile network
 */
@BindingAdapter("technology")
fun AppCompatImageView.setTechnologyIcon(networkInfo: NetworkInfo?) {

    when (networkInfo) {
        is CellNetworkInfo -> {
            visibility = View.VISIBLE
            when (CellTechnology.fromMobileNetworkType(networkInfo.networkType)) {
                null -> {
                    setImageDrawable(null)
                }
                CellTechnology.CONNECTION_2G -> {
                    setImageResource(R.drawable.ic_2g)
                }
                CellTechnology.CONNECTION_3G -> {
                    setImageResource(R.drawable.ic_3g)
                }
                CellTechnology.CONNECTION_4G -> {
                    setImageResource(R.drawable.ic_4g)
                }
                CellTechnology.CONNECTION_4G_5G -> {
                    setImageResource(R.drawable.ic_5g_available)
                }
                CellTechnology.CONNECTION_5G -> {
                    setImageResource(R.drawable.ic_5g)
                }
                else -> {setImageDrawable(null)}
            }
        }
        is EthernetNetworkInfo -> {
            visibility = View.VISIBLE
            setImageResource(R.drawable.ic_label_ethernet)
        }
        // todo: add resources for VPN and Bluetooth
        else -> visibility = View.GONE
    }
}

/**
 * A binding adapter that is used for show ip address icon
 */
@BindingAdapter(value = ["IpIcon"], requireAll = true)
fun ImageView.setIPAddressIcon(ipInfo: IpInfo?) {
    ipInfo?.let {
        val isIPV4 = it.protocol == IpProtocol.V4

        val res = when (it.ipStatus) {
            IpStatus.NO_INFO -> {
                isClickable = false
                alpha = 0.25f
                if (isIPV4) R.drawable.ic_ipv4_gray else R.drawable.ic_ipv6_gray
            }
            IpStatus.NO_ADDRESS -> {
                isClickable = true
                alpha = 1f
                if (isIPV4) R.drawable.ic_ipv4_red else R.drawable.ic_ipv6_red
            }
            IpStatus.NO_NAT -> {
                isClickable = true
                alpha = 1f
                if (isIPV4) R.drawable.ic_ipv4_green else R.drawable.ic_ipv6_green
            }
            else -> {
                isClickable = true
                alpha = 1f
                if (isIPV4) R.drawable.ic_ipv4_yellow else R.drawable.ic_ipv6_yellow
            }
        }
        setImageResource(res)
    }
}

@BindingAdapter("waveEnabled")
fun waveEnabled(view: WaveView, enabled: Boolean) {
    view.waveEnabled = enabled
}

val THRESHOLD_PING = listOf(0.0, 10.0, 25.0, 75.0) // 0ms, 10ms, 25ms, 75ms

/**
 * A binding adapter that is used for show ping value
 */
@BindingAdapter("pingMs")
fun AppCompatTextView.setPing(pingNanos: Long) {

    if (pingNanos > 0) {

        val pingResult = if (pingNanos > 1000000) {
            (pingNanos / 1000000.0).roundToLong().toDouble()
        } else {
            (pingNanos / 1000000.0)
        }

        setCompoundDrawablesWithIntrinsicBounds(

            when (pingResult) {
                in THRESHOLD_PING[3]..Double.MAX_VALUE -> {
                    R.drawable.ic_small_ping_red
                }
                in THRESHOLD_PING[2]..THRESHOLD_PING[3] -> {
                    R.drawable.ic_small_ping_yellow
                }
                in THRESHOLD_PING[1]..THRESHOLD_PING[2] -> {
                    R.drawable.ic_small_ping_light_green
                }
                in THRESHOLD_PING[0]..THRESHOLD_PING[1] -> {
                    R.drawable.ic_small_ping_dark_green
                }
                else -> {
                    R.drawable.ic_small_ping_gray
                }
            }, 0, 0, 0
        )

        setTextColor(context.getColor(android.R.color.white))
        val mantissa = pingResult - (pingResult.toInt().toDouble())
        if (mantissa > 0 && pingResult < 10.0) {
            text = context.getString(R.string.measurement_ping_value_1f, pingResult)
        } else {
            text = context.getString(R.string.measurement_ping_value, pingResult.roundToInt().toString())
        }
    } else {
        setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_small_ping_gray, 0, 0, 0)
        setTextColor(context.getColor(R.color.text_white_transparency_40))
        text = context.getString(R.string.measurement_dash)
    }
}

val THRESHOLD_DOWNLOAD = listOf(0L, 5000000L, 10000000L, 100000000L) // 0mb, 5mb, 10mb, 100mb

/**
 * A binding adapter that is used for show download speed
 */
@BindingAdapter("downloadSpeedBps")
fun AppCompatTextView.setDownload(downloadSpeedBps: Long) {

    if (downloadSpeedBps > 0) {
        val downloadSpeedInMbps: Float = downloadSpeedBps / 1000000.0f

        setCompoundDrawablesWithIntrinsicBounds(

            when (downloadSpeedBps) {
                in THRESHOLD_DOWNLOAD[0] until THRESHOLD_DOWNLOAD[1] -> {
                    R.drawable.ic_small_download_red
                }
                in THRESHOLD_DOWNLOAD[1] until THRESHOLD_DOWNLOAD[2] -> {
                    R.drawable.ic_small_download_yellow
                }
                in THRESHOLD_DOWNLOAD[2] until THRESHOLD_DOWNLOAD[3] -> {
                    R.drawable.ic_small_download_light_green
                }
                else -> {
                    R.drawable.ic_small_download_dark_green
                }
            }, 0, 0, 0
        )
        text = context.getString(
            R.string.measurement_download_upload_speed,
            downloadSpeedInMbps.format()
        )
        setTextColor(context.getColor(android.R.color.white))
    } else {
        setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_small_download_gray, 0, 0, 0)
        setTextColor(context.getColor(R.color.text_white_transparency_40))
        text = context.getString(R.string.measurement_dash)
    }
}

val THRESHOLD_UPLOAD = listOf(0L, 2500000L, 5000000L, 50000000L) // 0mb, 2.5mb, 5mb, 50mb

/**
 * A binding adapter that is used for show upload speed
 */
@BindingAdapter("uploadSpeedBps")
fun AppCompatTextView.setUpload(uploadSpeedBps: Long) {

    if (uploadSpeedBps > 0) {
        val uploadSpeedInMbps: Float = uploadSpeedBps / 1000000.0f

        setCompoundDrawablesWithIntrinsicBounds(

            when (uploadSpeedBps) {
                in THRESHOLD_UPLOAD[0] until THRESHOLD_UPLOAD[1] -> {
                    R.drawable.ic_small_upload_red
                }
                in THRESHOLD_UPLOAD[1] until THRESHOLD_UPLOAD[2] -> {
                    R.drawable.ic_small_upload_yellow
                }
                in THRESHOLD_UPLOAD[2] until THRESHOLD_UPLOAD[3] -> {
                    R.drawable.ic_small_upload_light_green
                }
                else -> {
                    R.drawable.ic_small_upload_dark_green
                }
            }, 0, 0, 0
        )
        text = context.getString(
            R.string.measurement_download_upload_speed,
            uploadSpeedInMbps.format()
        )
        setTextColor(context.getColor(android.R.color.white))
    } else {
        setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_small_upload_gray, 0, 0, 0)
        setTextColor(context.getColor(R.color.text_white_transparency_40))
        text = context.getString(R.string.measurement_dash)
    }
}

/**
 * A binding adapter that is used for show network icon based on network type (WIFI/MOBILE),
 * and signalLevel(0..4)
 */
@BindingAdapter("measurementSignalLevel", "connected")
fun AppCompatImageView.setSmallIcon(signalStrengthInfo: SignalStrengthInfo?, connectionAvailable: Boolean) {

    if (connectionAvailable) {
        when (signalStrengthInfo?.transport) {
            TransportType.WIFI -> {
                when (signalStrengthInfo.signalLevel) {
                    2 -> {
                        setImageResource(R.drawable.ic_small_wifi_2)
                    }
                    3 -> {
                        setImageResource(R.drawable.ic_small_wifi_3)
                    }
                    4 -> {
                        setImageResource(R.drawable.ic_small_wifi_4)
                    }
                    else -> {
                        setImageResource(R.drawable.ic_small_wifi_1)
                    }
                }
            }
            TransportType.CELLULAR -> {
                when (signalStrengthInfo.signalLevel) {
                    2 -> {
                        setImageResource(R.drawable.ic_small_mobile_2)
                    }
                    3 -> {
                        setImageResource(R.drawable.ic_small_mobile_3)
                    }
                    4 -> {
                        setImageResource(R.drawable.ic_small_mobile_4)
                    }
                    else -> {
                        setImageResource(R.drawable.ic_small_mobile_1)
                    }
                }
            }
            else -> {
                setImageResource(R.drawable.ic_signal_unknown_small)
            }
        }
    } else {
        setImageResource(R.drawable.ic_signal_unknown_small)
    }
}

/**
 * A binding adapter that is used for show download and upload data on graph
 */
@BindingAdapter("graphItems")
fun SpeedLineChart.setGraphItems(graphItems: List<GraphItemRecord>?) {
    addGraphItems(graphItems)
}

/**
 * A binding adapter that is used for clear download and upload data
 */
@BindingAdapter("reset")
fun SpeedLineChart.reset(measurementState: MeasurementState) {

    when (measurementState) {
        MeasurementState.IDLE, MeasurementState.INIT,
        MeasurementState.DOWNLOAD, MeasurementState.UPLOAD -> {
            reset()
        }
        else -> {
        }
    }
}

@BindingAdapter("speed")
fun MeasurementCurveLayout.setSpeed(speed: Long) {
    setBottomProgress(speed)
}

@BindingAdapter("percentage")
fun MeasurementCurveLayout.setPercents(percentage: Int) {
    setTopProgress(percentage)
}

@BindingAdapter("strength")
fun MeasurementCurveLayout.setSignal(signalLevel: SignalStrengthInfo?) {
    setSignalStrength(signalLevel)
}

@BindingAdapter("measurementPhase")
fun MeasurementCurveLayout.setMeasurementPhase(state: MeasurementState) {
    setMeasurementState(state)
}

@BindingAdapter("qosEnabled")
fun MeasurementCurveLayout.setQosEnabled(enabled: Boolean) {
    setQoSEnabled(enabled)
}

@BindingAdapter("progress_enabled")
fun ProgressBar.setProgressEnabled(enabled: Boolean) {
    setProgressEnabled(enabled)
}

/**
 * A binding adapter that is used for show label of measurement state
 */
@BindingAdapter("labelMeasurementState")
fun AppCompatTextView.setLabelOfMeasurementState(measurementState: MeasurementState) {

    when (measurementState) {
        MeasurementState.IDLE, MeasurementState.INIT, MeasurementState.PING, MeasurementState.DOWNLOAD -> {
            text = context.getString(R.string.measurement_download)
        }
        MeasurementState.UPLOAD -> {
            text = context.getString(R.string.measurement_upload)
        }
        MeasurementState.QOS -> {
            text = context.getString(R.string.measurement_qos)
        }
        else -> {
        }
    }
}

@BindingAdapter("bottomSheetState")
fun ConstraintLayout.setBottomSheetState(state: Int) {
    val behavior = BottomSheetBehavior.from(this)
    behavior.state = state
}

/**
 * A binding adapter that is used for show date and time in history list
 */
@BindingAdapter("networkType", "historyTime", "historyTimezone", "historySignalStrength", requireAll = true)
fun AppCompatTextView.setHistoryTime(networkType: NetworkTypeCompat, historyTime: Long, historyTimezone: String, signalStrength: Classification) {

    val calendar: Calendar = Calendar.getInstance()
    calendar.timeInMillis = historyTime
    calendar.timeZone = TimeZone.getTimeZone(historyTimezone)
    text = calendar.format("dd.MM.yy, HH:mm:ss")

    setCompoundDrawablesWithIntrinsicBounds(getSignalImageResource(networkType, signalStrength), 0, 0, 0)
}

@BindingAdapter("historyTime", "historyTimezone", requireAll = true)
fun AppCompatTextView.setHistoryTime(historyTime: Long, historyTimezone: String) {

    val calendar: Calendar = Calendar.getInstance()
    calendar.timeInMillis = historyTime
    calendar.timeZone = TimeZone.getTimeZone(historyTimezone)
    text = calendar.format("dd.MM.yy, HH:mm:ss")
}

@BindingAdapter("networkType", "historySignalStrength", requireAll = true)
fun ImageView.setSignalIcon(networkType: NetworkTypeCompat?, signalStrength: Classification) {
    networkType?.let { setImageResource(getSignalImageResource(it, signalStrength)) }
}

private fun getSignalImageResource(networkType: NetworkTypeCompat, signalStrength: Classification): Int {
    return when (networkType) {
        NetworkTypeCompat.TYPE_2G -> {
            when (signalStrength) {
                Classification.BAD -> R.drawable.ic_history_2g_1
                Classification.NORMAL -> R.drawable.ic_history_2g_2
                Classification.GOOD -> R.drawable.ic_history_2g_3
                Classification.EXCELLENT -> R.drawable.ic_history_2g_4
                Classification.NONE -> R.drawable.ic_signal_unknown_small
            }
        }
        NetworkTypeCompat.TYPE_3G -> {
            when (signalStrength) {
                Classification.BAD -> R.drawable.ic_history_3g_1
                Classification.NORMAL -> R.drawable.ic_history_3g_2
                Classification.GOOD -> R.drawable.ic_history_3g_3
                Classification.EXCELLENT -> R.drawable.ic_history_3g_4
                Classification.NONE -> R.drawable.ic_signal_unknown_small
            }
        }
        NetworkTypeCompat.TYPE_5G_AVAILABLE,
        NetworkTypeCompat.TYPE_4G -> {
            when (signalStrength) {
                Classification.BAD -> R.drawable.ic_history_4g_1
                Classification.NORMAL -> R.drawable.ic_history_4g_2
                Classification.GOOD -> R.drawable.ic_history_4g_3
                Classification.EXCELLENT -> R.drawable.ic_history_4g_4
                Classification.NONE -> R.drawable.ic_signal_unknown_small
            }
        }
        NetworkTypeCompat.TYPE_WLAN -> {
            when (signalStrength) {
                Classification.BAD -> R.drawable.ic_history_wifi_1
                Classification.NORMAL -> R.drawable.ic_history_wifi_2
                Classification.GOOD -> R.drawable.ic_history_wifi_3
                Classification.EXCELLENT -> R.drawable.ic_history_wifi_4
                Classification.NONE -> R.drawable.ic_signal_unknown_small
            }
        }
        NetworkTypeCompat.TYPE_UNKNOWN -> {
            R.drawable.ic_signal_unknown_small
        }
        NetworkTypeCompat.TYPE_BLUETOOTH -> {
            R.drawable.ic_bluetooth
        }
        NetworkTypeCompat.TYPE_VPN -> {
            R.drawable.ic_vpn
        }
        NetworkTypeCompat.TYPE_LAN -> {
            R.drawable.ic_ethernet
        }
        NetworkTypeCompat.TYPE_BROWSER -> {
            R.drawable.ic_browser
        }
        NetworkTypeCompat.TYPE_5G_NSA,
        NetworkTypeCompat.TYPE_5G -> {
            when (signalStrength) {
                Classification.BAD -> R.drawable.ic_history_5g_1
                Classification.NORMAL -> R.drawable.ic_history_5g_2
                Classification.GOOD -> R.drawable.ic_history_5g_3
                Classification.EXCELLENT -> R.drawable.ic_history_5g_4
                Classification.NONE -> R.drawable.ic_signal_unknown_small
            }
        }
    }
}

/**
 * A binding adapter that is used for show date and time in result details
 */
@BindingAdapter("resultTime", "resultTimezone", requireAll = true)
fun AppCompatTextView.setResultTime(resultTime: Long?, resultTimezone: String?) {
    if (resultTime != null && resultTimezone != null) {
        val calendar: Calendar = Calendar.getInstance()
        calendar.timeInMillis = resultTime
        calendar.timeZone = TimeZone.getTimeZone(resultTimezone)
        text = calendar.format("dd.MM.yy, HH:mm:ss")
    }
}

/**
 * A binding adapter that is used for show download speed with classification icon in history list
 */
@BindingAdapter("speedDownloadClassification")
fun AppCompatTextView.setDownload(speedDownloadClassification: Classification) {
    setCompoundDrawablesWithIntrinsicBounds(getSpeedDownloadClassification(speedDownloadClassification), 0, 0, 0)
}

/**
 * A binding adapter that is used for show download speed with classification icon in results
 */
@BindingAdapter("speedDownloadResult", "speedDownloadClassificationResult", requireAll = true)
fun AppCompatTextView.speedDownloadResult(speedDownloadResult: Long, speedDownloadClassificationResult: Classification) {

    text = if (speedDownloadResult > 0) {
        context.getString(R.string.measurement_download_upload_speed, ((speedDownloadResult.toFloat() / 1000f).format()))
    } else {
        context.getString(R.string.measurement_dash)
    }
    setCompoundDrawablesWithIntrinsicBounds(getSpeedDownloadClassification(speedDownloadClassificationResult), 0, 0, 0)
}

fun getSpeedDownloadClassification(speedDownloadClassification: Classification): Int {
    return when (speedDownloadClassification) {
        Classification.NONE -> {
            R.drawable.ic_small_download_gray
        }
        Classification.BAD -> {
            R.drawable.ic_small_download_red
        }
        Classification.NORMAL -> {
            R.drawable.ic_small_download_yellow
        }
        Classification.GOOD -> {
            R.drawable.ic_small_download_light_green
        }
        Classification.EXCELLENT -> {
            R.drawable.ic_small_download_dark_green
        }
    }
}

/**
 * A binding adapter that is used for show upload speed with classification icon in results
 */
@BindingAdapter("speedUploadResult", "speedUploadClassificationResult", requireAll = true)
fun AppCompatTextView.speedUploadResult(speedUploadResult: Long, speedUploadClassificationResult: Classification) {
    text = if (speedUploadResult > 0) {
        context.getString(R.string.measurement_download_upload_speed, ((speedUploadResult.toFloat() / 1000f).format()))
    } else {
        context.getString(R.string.measurement_dash)
    }
    setCompoundDrawablesWithIntrinsicBounds(getSpeedUploadClassificationIcon(speedUploadClassificationResult), 0, 0, 0)
}

/**
 * A binding adapter that is used for show upload speed with classification icon in history list
 */
@BindingAdapter("speedUploadClassification")
fun AppCompatTextView.setUpload(speedUploadClassification: Classification) {
    setCompoundDrawablesWithIntrinsicBounds(getSpeedUploadClassificationIcon(speedUploadClassification), 0, 0, 0)
}

fun getSpeedUploadClassificationIcon(speedUploadClassification: Classification): Int {
    return when (speedUploadClassification) {
        Classification.NONE -> {
            R.drawable.ic_small_upload_gray
        }
        Classification.BAD -> {
            R.drawable.ic_small_upload_red
        }
        Classification.NORMAL -> {
            R.drawable.ic_small_upload_yellow
        }
        Classification.GOOD -> {
            R.drawable.ic_small_upload_light_green
        }
        Classification.EXCELLENT -> {
            R.drawable.ic_small_upload_dark_green
        }
    }
}

/**
 * A binding adapter that is used for show download ping with classification icon in history list
 */
@BindingAdapter("pingClassification")
fun AppCompatTextView.setPing(pingClassification: Classification) {
    setCompoundDrawablesWithIntrinsicBounds(getPingClassificationIcon(pingClassification), 0, 0, 0)
}

/**
 * A binding adapter that is used for show download ping with classification icon in results
 */
@BindingAdapter("pingResult", "pingClassificationResult", requireAll = true)
fun AppCompatTextView.setPingResult(pingResult: Double, pingClassificationResult: Classification) {
    text = if (pingResult > 0) {
        val mantissa = pingResult - (pingResult.toInt().toDouble())
        if (mantissa > 0 && pingResult < 10.0) {
            context.getString(R.string.measurement_ping_value_1f, pingResult)
        } else {
            context.getString(R.string.measurement_ping_value, pingResult.roundToInt().toString())
        }
    } else {
        context.getString(R.string.measurement_dash)
    }
    setCompoundDrawablesWithIntrinsicBounds(getPingClassificationIcon(pingClassificationResult), 0, 0, 0)
}

fun getPingClassificationIcon(pingClassification: Classification): Int {
    return when (pingClassification) {
        Classification.NONE -> {
            R.drawable.ic_small_ping_gray
        }
        Classification.BAD -> {
            R.drawable.ic_small_ping_red
        }
        Classification.NORMAL -> {
            R.drawable.ic_small_ping_yellow
        }
        Classification.GOOD -> {
            R.drawable.ic_small_ping_light_green
        }
        Classification.EXCELLENT -> {
            R.drawable.ic_small_ping_dark_green
        }
    }
}

/**
 * A binding adapter that is used for show signal strength with classification icon in result
 */
@BindingAdapter("signalStrengthResult", "signalStrengthClassificationResult", requireAll = true)
fun AppCompatTextView.setSignalStrength(signalStrengthResult: Int?, signalStrengthClassificationResult: Classification) {

    text = if (signalStrengthResult != null) {
        context.getString(R.string.strength_signal_value, signalStrengthResult)
    } else {
        context.getString(R.string.measurement_dash)
    }

    setCompoundDrawablesWithIntrinsicBounds(

        when (signalStrengthClassificationResult) {
            Classification.NONE -> {
                R.drawable.ic_small_wifi_gray
            }
            Classification.BAD -> {
                R.drawable.ic_small_wifi_red
            }
            Classification.NORMAL -> {
                R.drawable.ic_small_wifi_yellow
            }
            Classification.GOOD -> {
                R.drawable.ic_small_wifi_light_green
            }
            Classification.EXCELLENT -> {
                R.drawable.ic_small_wifi_dark_green
            }
        }, 0, 0, 0
    )
}

/**
 * A binding adapter that is used for show correct icon for qoe item in the results
 */
@BindingAdapter("qoeIcon")
fun AppCompatImageView.setQoEIcon(qoECategory: QoECategory) {
    setImageDrawable(
        ContextCompat.getDrawable(
            context,
            when (qoECategory) {
                QoECategory.QOE_UNKNOWN -> 0
                QoECategory.QOE_AUDIO_STREAMING -> R.drawable.ic_qoe_music
                QoECategory.QOE_VIDEO_SD -> R.drawable.ic_qoe_video
                QoECategory.QOE_VIDEO_HD -> R.drawable.ic_qoe_video
                QoECategory.QOE_VIDEO_UHD -> R.drawable.ic_qoe_video
                QoECategory.QOE_GAMING -> R.drawable.ic_qoe_game
                QoECategory.QOE_GAMING_CLOUD -> R.drawable.ic_qoe_game
                QoECategory.QOE_GAMING_STREAMING -> R.drawable.ic_qoe_game
                QoECategory.QOE_GAMING_DOWNLOAD -> R.drawable.ic_qoe_game
                QoECategory.QOE_VOIP -> R.drawable.ic_qoe_voip
                QoECategory.QOE_VIDEO_TELEPHONY -> R.drawable.ic_qoe_voip
                QoECategory.QOE_VIDEO_CONFERENCING -> R.drawable.ic_qoe_voip
                QoECategory.QOE_MESSAGING -> R.drawable.ic_qoe_image
                QoECategory.QOE_WEB -> R.drawable.ic_qoe_image
                QoECategory.QOE_CLOUD -> R.drawable.ic_qoe_image
                QoECategory.QOE_QOS -> R.drawable.ic_qoe_qos
            }
        )
    )
}

/**
 * A binding adapter that is used for show name for qoe item in the results
 */
@BindingAdapter("qoeName")
fun AppCompatTextView.setQoEName(qoECategory: QoECategory) {
    text = context.getString(
        when (qoECategory) {
            QoECategory.QOE_UNKNOWN -> 0
            QoECategory.QOE_AUDIO_STREAMING -> R.string.results_qoe_audio_streaming
            QoECategory.QOE_VIDEO_SD -> R.string.results_qoe_videos_sd
            QoECategory.QOE_VIDEO_HD -> R.string.results_qoe_videos_hd
            QoECategory.QOE_VIDEO_UHD -> R.string.results_qoe_videos_uhd
            QoECategory.QOE_GAMING -> R.string.results_qoe_gaming
            QoECategory.QOE_GAMING_CLOUD -> R.string.results_qoe_gaming_cloud
            QoECategory.QOE_GAMING_STREAMING -> R.string.results_qoe_gaming_streaming
            QoECategory.QOE_GAMING_DOWNLOAD -> R.string.results_qoe_gaming_download
            QoECategory.QOE_VOIP -> R.string.results_qoe_voip
            QoECategory.QOE_VIDEO_TELEPHONY -> R.string.results_qoe_video_telephony
            QoECategory.QOE_VIDEO_CONFERENCING -> R.string.results_qoe_video_conferencing
            QoECategory.QOE_MESSAGING -> R.string.results_qoe_messaging
            QoECategory.QOE_WEB -> R.string.results_qoe_web
            QoECategory.QOE_CLOUD -> R.string.results_qoe_cloud
            QoECategory.QOE_QOS -> R.string.results_qoe_qos
        }
    )
}

/**
 * A binding adapter that is used for show correct value for qoe item in the results
 */
@BindingAdapter("qoePercent", "classification")
fun ResultBar.setQoEValue(value: Double, classification: Classification) {
    val qoeMinThreshold = 0.16f
    if (value < qoeMinThreshold) {
        updateClassification((qoeMinThreshold * 100).toInt(), Classification.BAD)
    } else {
        updateClassification((value * 100).toInt(), classification)
    }
}

@BindingAdapter("networkType", "signalStrength", requireAll = true)
fun ImageView.setNetworkType(networkType: String, signalStrength: Classification) {
    if (networkType != ServerNetworkType.TYPE_UNKNOWN.stringValue) {
        setImageResource(
            when (NetworkTypeCompat.fromString(networkType)) {
                NetworkTypeCompat.TYPE_2G -> {
                    R.drawable.ic_history_2g
                }
                NetworkTypeCompat.TYPE_3G -> {
                    R.drawable.ic_history_3g
                }
                NetworkTypeCompat.TYPE_5G_AVAILABLE,
                NetworkTypeCompat.TYPE_4G -> {
                    R.drawable.ic_history_4g
                }
                NetworkTypeCompat.TYPE_UNKNOWN -> {
                    R.drawable.ic_history_no_internet
                }
                NetworkTypeCompat.TYPE_BLUETOOTH -> {
                    R.drawable.ic_bluetooth
                }
                NetworkTypeCompat.TYPE_VPN -> {
                    R.drawable.ic_vpn
                }
                NetworkTypeCompat.TYPE_LAN,
                NetworkTypeCompat.TYPE_BROWSER -> {
                    R.drawable.ic_browser
                }
                NetworkTypeCompat.TYPE_WLAN -> {
                    when (signalStrength) {
                        Classification.BAD -> R.drawable.ic_history_wifi_1
                        Classification.NORMAL -> R.drawable.ic_history_wifi_2
                        Classification.GOOD -> R.drawable.ic_history_wifi_3
                        Classification.EXCELLENT -> R.drawable.ic_history_wifi_4
                        Classification.NONE -> R.drawable.ic_history_no_internet
                    }
                }
                NetworkTypeCompat.TYPE_5G_NSA,
                NetworkTypeCompat.TYPE_5G -> {
                    R.drawable.ic_history_5g_3
                }
            }
        )
    } else {
        setImageResource(R.drawable.ic_history_no_internet)
    }
}

@BindingAdapter("timeString")
fun AppCompatTextView.setTimeAs24h(time: Long) {
    text = SimpleDateFormat("dd.MM.yy, HH:mm:ss", Locale.US).format(Date(time))
}

@BindingAdapter("signalStrengthMap", "signalStrengthClassificationMap", requireAll = true)
fun AppCompatTextView.setSignalStrengthMap(signalStrengthResult: String?, signalStrengthClassificationResult: Classification) {

    text = signalStrengthResult ?: context.getString(R.string.measurement_dash)

    setCompoundDrawablesWithIntrinsicBounds(

        when (signalStrengthClassificationResult) {
            Classification.NONE -> {
                R.drawable.ic_small_wifi_gray
            }
            Classification.BAD -> {
                R.drawable.ic_small_wifi_red
            }
            Classification.NORMAL -> {
                R.drawable.ic_small_wifi_yellow
            }
            Classification.GOOD -> {
                R.drawable.ic_small_wifi_light_green
            }
            Classification.EXCELLENT -> {
                R.drawable.ic_small_wifi_dark_green
            }
        }, 0, 0, 0
    )
}

/**
 * A binding adapter that is used for show network type
 */
@BindingAdapter("networkTypeDetailed")
fun AppCompatTextView.setDetailedNetworkType(networkTypeDetailed: DetailedNetworkInfo?) {

    /**
     *  display "LTE" (true) or "4G/LTE" (false)
     */
    val shortDisplayOfTechnology = false

    text = extractTechnologyString(networkTypeDetailed, shortDisplayOfTechnology)
}