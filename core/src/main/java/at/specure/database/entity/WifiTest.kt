package at.specure.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.database.Columns

@Entity
class WifiTest(
    @PrimaryKey
    @ForeignKey(entity = Test::class, parentColumns = [Columns.TEST_UUID_PARENT_COLUMN], childColumns = ["testUUID"], onDelete = ForeignKey.CASCADE)
    val testUUID: String,
    /**
     * Wifi suplicant state from [android.net.wifi.WifiInfo]
     */
    val wifiSupplicantState: String?,
    /**
     * Wifi detailed suplicant state from [android.net.wifi.WifiInfo]
     */
    val wifiSupplicantDetailedState: String?,
    /**
     * Wifi ssid state from [android.net.wifi.WifiInfo]
     */
    val wifiSsid: String?,
    /**
     * Wifi bssid state from [android.net.wifi.WifiInfo]
     */
    val wifiBssid: String?,
    /**
     * Wifi networkId from [android.net.wifi.WifiInfo]
     */
    val wifiNetworkId: String?
)