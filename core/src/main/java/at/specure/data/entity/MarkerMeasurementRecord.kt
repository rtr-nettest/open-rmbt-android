package at.specure.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Classification
import at.specure.data.Columns
import at.specure.data.Tables

@Entity(tableName = Tables.MAP_MARKER_MEASUREMENTS)
data class MarkerMeasurementRecord(

    /**
     * longitude coordinate of geolocation
     */
    val longitude: Double,

    /**
     * latitude coordinate of geolocation
     */
    val latitude: Double,

    /**
     * Classification value for assigning traffic-light-color
     */
    val uploadClass: Classification,

    /**
     * Upload speed in kbit per second
     */
    val uploadSpeed: String?,

    /**
     * Classification value for assigning traffic-light-color
     */
    val downloadClass: Classification,

    /**
     * Download speed in kbit per second
     */
    val downloadSpeed: String?,

    /**
     * Classification value for assigning traffic-light-color
     */
    val signalClass: Classification,

    /**
     * Signal value in dBm for WIFI, 3G, 2G, 4G measurement connections
     */
    val signalStrength: String?,

    /**
     * Classification value for assigning traffic-light-color
     */
    val pingClass: Classification,

    /**
     * Median ping (round-trip time) in milliseconds, measured on the server side. In previous versions (before June 3rd 2015) this was the minimum ping measured on the client side.
     */
    val pingMillis: String?,

    /**
     * Server type of the network (human readable format)
     */
    val networkTypeLabel: String?,

    /**
     * Provider name
     */
    val providerName: String?,

    /**
     * Network name (ssid)
     */
    val wifiSSID: String?,

    /**
     * open uuid of the test used for identify test in opendata and request opendata result details (necessary for graph values)
     */
    @PrimaryKey
    @ColumnInfo(name = Columns.TEST_OPEN_UUID_PARENT_COLUMN)
    val openTestUUID: String,

    /**
     * time of test execution in ms
     */
    val time: Long,

    /**
     * time of test execution, example: "Dec 20, 2019 12:42:14 PM"
     */
    val timeString: String?

)