package at.rtr.rmbt.android.ui.viewstate

import android.os.Build
import android.telephony.CellInfo
import android.text.Html
import android.text.Spanned
import androidx.databinding.ObservableField
import at.rmbt.client.control.IpProtocol
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.cell.CellTechnology
import at.specure.info.connectivity.ConnectivityInfo
import at.specure.info.ip.IpInfo
import at.specure.info.network.DetailedNetworkInfo
import at.specure.info.network.WifiNetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.location.LocationInfo

class NetworkDetailsViewState : ViewState {

    val connectivityInfo = ObservableField<Spanned?>()
    val activeNetworkInfo = ObservableField<Spanned?>()
    val signalStrengthInfo = ObservableField<Spanned?>()
    val ipV4Info = ObservableField<Spanned?>()
    val ipV6Info = ObservableField<Spanned?>()
    val locationInfo = ObservableField<Spanned?>()
    val locationState = ObservableField<String>()

    fun setIpInfo(ipInfo: IpInfo?) {
        val info = buildString {
            if (ipInfo == null) {
                append("No Data")
            } else {
                bold("Private: ").append(ipInfo.privateAddress).newLine()
                bold("Public: ").append(ipInfo.publicAddress).newLine()
                bold("Status: ").append(ipInfo.ipStatus.name).newLine()
            }
        }

        if (ipInfo?.protocol == IpProtocol.V6) {
            ipV6Info.set(info.html())
        } else {
            ipV4Info.set(info.html())
        }
    }

    fun setConnectivityInfo(connectivityInfo: ConnectivityInfo?) {
        val info = buildString {
            if (connectivityInfo == null) {
                append("Disconnected")
            } else {
                bold("Connected").newLine()
                bold("netId: ").append(connectivityInfo.netId).newLine()
                bold("transport: ").append(connectivityInfo.transportType.name).newLine()
                bold("capabilities: ")
                connectivityInfo.capabilities.forEach {
                    append(it.name).append(", ")
                }
                newLine()
                bold("linkDownstreamBandwidthKbps: ").append(connectivityInfo.linkDownstreamBandwidthKbps).newLine()
                bold("linkUpstreamBandwidthKbps: ").append(connectivityInfo.linkUpstreamBandwidthKbps).newLine()
            }
        }
        this.connectivityInfo.set(info.html())
    }

    fun setSignalStrengthInfo(signal: SignalStrengthInfo?) {
        val info = if (signal == null) {
            "No Info"
        } else {
            buildString {
                bold("transport: ").append(signal.transport).newLine()
                bold("value: ").append(signal.value).append(" dBm").newLine()
                bold("level: ").append(signal.signalLevel).newLine()
                bold("rsrq: ").append(signal.rsrq).append(" dB").newLine()
            }
        }
        this.signalStrengthInfo.set(info.html())
    }

    fun setActiveNetworkInfo(detailedNetworkInfo: DetailedNetworkInfo?) {
        val info = when (detailedNetworkInfo?.networkInfo) {
            null -> "Disconnected"
            is WifiNetworkInfo -> extractWifiNetworkInfo(detailedNetworkInfo.networkInfo as WifiNetworkInfo)
            is CellNetworkInfo -> {
                extractCellNetworkInfo(
                    detailedNetworkInfo.networkInfo as CellNetworkInfo,
                    detailedNetworkInfo.cellInfos
                )
            }
            else -> "Not Implemented"
        }

        this.activeNetworkInfo.set(info.html())
    }

    fun setLocationInfo(location: LocationInfo?) {
        val info = when (location) {
            null -> "Not available"
            else -> extractLocationInfo(location)
        }
        locationInfo.set(info.html())
    }

    private fun extractLocationInfo(info: LocationInfo) = buildString {
        bold("provider: ").append(info.provider).newLine()
        bold("lat: ").append(info.latitude).newLine()
        bold("lon: ").append(info.longitude).newLine()
        bold("acc: ").append(info.accuracy).newLine()
    }

    private fun extractWifiNetworkInfo(info: WifiNetworkInfo): String = buildString {
        bold("name: ").append(info.name).newLine()
        bold("bssid: ").append(info.bssid).newLine()
        bold("band: ").append(info.band).newLine()
        bold("isSSIDHidden: ").append(info.isSSIDHidden).newLine()
        bold("ipAddress: ").append(info.ipAddress).newLine()
        bold("linkSpeed: ").append(info.linkSpeed).newLine()
        bold("networkId: ").append(info.networkId).newLine()
        bold("rssi: ").append(info.rssi).newLine()
        bold("signalLevel: ").append(info.signalLevel).newLine()
        bold("ssid: ").append(info.ssid).newLine()
        bold("supplicantState: ").append(info.supplicantState).newLine()
    }

    private fun extractCellNetworkInfo(info: CellNetworkInfo, rawCellInfos: List<CellInfo>?): String =
        buildString {
            bold("name: ").append(info.name).newLine()
            bold("band: ").append(info.band).newLine()
            bold("technology: ").append(CellTechnology.fromMobileNetworkType(info.networkType)).newLine()
            bold("provider name: ").append(info.providerName).newLine()
            bold("network type: ").append(info.networkType.name).newLine()
            bold("cell infos: ").append(rawCellInfos).newLine()
        }

    @Suppress("DEPRECATION")
    private fun String.html() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(this, Html.FROM_HTML_MODE_COMPACT)
    } else {
        Html.fromHtml(this)
    }

    private fun StringBuilder.newLine() = append("<br>")

    private fun StringBuilder.bold(text: String) = append("<b>")
        .append(text)
        .append("</b>")
}