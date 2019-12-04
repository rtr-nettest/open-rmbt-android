package at.specure.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.database.Columns
import at.specure.database.Tables

@Entity(tableName = Tables.TEST_WLAN_RECORD)
class TestWlanRecord(

    @PrimaryKey
    @ForeignKey(entity = TestRecord::class, parentColumns = [Columns.TEST_UUID_PARENT_COLUMN], childColumns = ["testUUID"], onDelete = ForeignKey.CASCADE)
    val testUUID: String,

    /**
     * Wifi suplicant state from [android.net.wifi.WifiInfo]
     */
    val supplicantState: String?,

    /**
     * Wifi detailed suplicant state from [android.net.wifi.WifiInfo]
     */
    val supplicantDetailedState: String?,

    /**
     * Wifi ssid state from [android.net.wifi.WifiInfo]
     */
    val ssid: String?,

    /**
     * Wifi bssid state from [android.net.wifi.WifiInfo]
     */
    val bssid: String?,

    /**
     * Wifi networkId from [android.net.wifi.WifiInfo]
     */
    val networkId: String?
)