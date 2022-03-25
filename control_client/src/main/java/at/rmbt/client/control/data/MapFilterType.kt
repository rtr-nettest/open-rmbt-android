package at.rmbt.client.control.data

enum class MapFilterType(
    val value: String,

    /**
     * value used by map filter API
     */
    val serverValue: String) {

    ALL("all", "All"),
    BROWSER("browser", "Browser"),
    MOBILE("mobile", "Mobile"),
    WLAN("wifi", "WLAN (App)"),
    BLUETOOTH("bluetooth", "Bluetooth"),
    VPN("vpn", "VPN");

    companion object {
        fun fromServerString(type: String): MapFilterType? {
            MapFilterType.values().forEach { x ->
                if (x.serverValue.contentEquals(type, true)) {
                    return x
                }
            }
            return null
        }

        fun fromFilterString(type: String): MapFilterType? {
            MapFilterType.values().forEach { x ->
                if (x.value.contentEquals(type, true)) {
                    return x
                }
            }
            return null
        }
    }
}