package at.rtr.rmbt.android.ui

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
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
import at.specure.info.network.NetworkInfo
import at.specure.info.network.WifiNetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.measurement.MeasurementState
import at.specure.result.QoECategory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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

/**
 * A binding adapter that is used for show signal
 */
@BindingAdapter("signal")
fun AppCompatTextView.setSignal(signal: Int?) {

    text = if (signal != null) {
        String.format(context.getString(R.string.home_signal_value), signal)
    } else {
        "-"
    }
}

/**
 * A binding adapter that is used for show frequency
 */
@BindingAdapter("frequency")
fun AppCompatTextView.setFrequency(networkInfo: NetworkInfo?) {

    text = when (networkInfo) {
        is WifiNetworkInfo -> networkInfo.band.name
        is CellNetworkInfo -> networkInfo.band?.name
        else -> "-"
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
@BindingAdapter("signalLevel")
fun AppCompatImageView.setIcon(signalStrengthInfo: SignalStrengthInfo?) {

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
        setImageResource(R.drawable.ic_no_internet)
    }
}

/**
 * A binding adapter that is used for show network type
 */
@BindingAdapter("networkType")
fun AppCompatTextView.setNetworkType(networkInfo: NetworkInfo?) {

    text = when (networkInfo) {
        is WifiNetworkInfo -> context.getString(R.string.home_wifi)
        is CellNetworkInfo -> {
            val technology =
                CellTechnology.fromMobileNetworkType(networkInfo.networkType)?.displayName
            if (technology == null) {
                networkInfo.networkType.displayName
            } else {
                "$technology/${networkInfo.networkType.displayName}"
            }
        }
        else -> context.getString(R.string.home_attention)
    }
}

/**
 * A binding adapter that is used for show technology icon for mobile network
 */
@BindingAdapter("technology")
fun AppCompatImageView.setTechnologyIcon(networkInfo: NetworkInfo?) {

    if (networkInfo is CellNetworkInfo) {
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
            CellTechnology.CONNECTION_5G -> {
                setImageResource(R.drawable.ic_5g)
            }
        }
    } else {
        visibility = View.GONE
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

val THRESHOLD_DOWNLOAD = listOf(0L, 1000000L, 2000000L, 30000000L) // 0mb, 1mb, 2mb, 30mb

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

val THRESHOLD_UPLOAD = listOf(0L, 500000L, 1000000L, 10000000L) // 0mb, 0.5mb, 1mb, 10mb

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
@BindingAdapter("measurementSignalLevel")
fun AppCompatImageView.setSmallIcon(signalStrengthInfo: SignalStrengthInfo?) {

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
            setImageResource(R.drawable.ic_small_no_internet)
        }
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

private fun getSignalImageResource(networkType: NetworkTypeCompat, signalStrength: Classification): Int =
    when (networkType) {
        NetworkTypeCompat.TYPE_2G -> {
            when (signalStrength) {
                Classification.BAD -> R.drawable.ic_history_2g_1
                Classification.NORMAL -> R.drawable.ic_history_2g_2
                Classification.GOOD -> R.drawable.ic_history_2g_3
                Classification.EXCELLENT -> R.drawable.ic_history_2g_4
                Classification.NONE -> R.drawable.ic_history_no_internet
            }
        }
        NetworkTypeCompat.TYPE_3G -> {
            when (signalStrength) {
                Classification.BAD -> R.drawable.ic_history_3g_1
                Classification.NORMAL -> R.drawable.ic_history_3g_2
                Classification.GOOD -> R.drawable.ic_history_3g_3
                Classification.EXCELLENT -> R.drawable.ic_history_3g_4
                Classification.NONE -> R.drawable.ic_history_no_internet
            }
        }
        NetworkTypeCompat.TYPE_4G -> {
            when (signalStrength) {
                Classification.BAD -> R.drawable.ic_history_4g_1
                Classification.NORMAL -> R.drawable.ic_history_4g_2
                Classification.GOOD -> R.drawable.ic_history_4g_3
                Classification.EXCELLENT -> R.drawable.ic_history_4g_4
                Classification.NONE -> R.drawable.ic_history_no_internet
            }
        }
        NetworkTypeCompat.TYPE_WLAN -> {
            when (signalStrength) {
                Classification.BAD -> R.drawable.ic_history_wifi_1
                Classification.NORMAL -> R.drawable.ic_history_wifi_2
                Classification.GOOD -> R.drawable.ic_history_wifi_3
                Classification.EXCELLENT -> R.drawable.ic_history_wifi_4
                Classification.NONE -> R.drawable.ic_no_wifi
            }
        }
        NetworkTypeCompat.TYPE_UNKNOWN -> {
            R.drawable.ic_history_no_internet
        }
        NetworkTypeCompat.TYPE_LAN,
        NetworkTypeCompat.TYPE_BROWSER -> {
            R.drawable.ic_browser
        }
        NetworkTypeCompat.TYPE_5G -> {
            when (signalStrength) {
                Classification.BAD -> R.drawable.ic_history_5g_1
                Classification.NORMAL -> R.drawable.ic_history_5g_2
                Classification.GOOD -> R.drawable.ic_history_5g_3
                Classification.EXCELLENT -> R.drawable.ic_history_5g_4
                Classification.NONE -> R.drawable.ic_history_no_internet
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
        context.getDrawable(
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
                NetworkTypeCompat.TYPE_4G -> {
                    R.drawable.ic_history_4g
                }
                NetworkTypeCompat.TYPE_UNKNOWN -> {
                    R.drawable.ic_history_no_internet
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