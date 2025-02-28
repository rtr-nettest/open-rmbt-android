package at.specure.test

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import at.rmbt.client.control.PermissionStatusBody
import at.specure.core.BuildConfig
import at.specure.location.LocationInfo
import at.specure.util.hasPermission
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.Locale
import java.util.TimeZone

// this is the default version overwritten by the version received from the server
const val RMBT_CLIENT_VERSION = "0.0.0"

@Keep
class DeviceInfo(context: Context, val location: Location? = null, val temperature: Float? = null) {

    @SerializedName("plattform")
    val platform = "Android"

    @SerializedName("os_version")
    val osVersion = "${Build.VERSION.RELEASE}(${Build.VERSION.INCREMENTAL})"

    @SerializedName("api_level")
    val apiLevel = Build.VERSION.SDK_INT.toString()
    val device: String? = Build.DEVICE
    val model: String? = Build.MODEL
    val product: String? = Build.PRODUCT
    val language: String? = Locale.getDefault().language
    val timezone: String? = TimeZone.getDefault().id

    val softwareRevision = buildString {
        append(BuildConfig.GIT_BRANCH_NAME)
        append("_")
        append(BuildConfig.GIT_FULL_HASH)
        @Suppress("ConstantConditionIf")
        if (BuildConfig.GIT_IS_DIRTY) {
            append("-dirty")
        }
    }

    val softwareVersionCode = BuildConfig.VERSION_CODE
    val softwareVersionName = BuildConfig.VERSION_NAME

    @SerializedName("type")
    val clientType = "MOBILE"

    @Expose
    val clientName = "RMBT"

    val clientVersionName = BuildConfig.VERSION_NAME

    val clientVersionCode = BuildConfig.VERSION_CODE

    @SerializedName("android_permission_status")
    val permissionsStatus: List<PermissionStatusBody> = mutableListOf<PermissionStatusBody>().apply {
        add(
            PermissionStatusBody(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                context.hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            )
        )
        add(
            PermissionStatusBody(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                context.hasPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(
                PermissionStatusBody(
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    context.hasPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                )
            )
        } else {
            add(PermissionStatusBody("android.permission.ACCESS_BACKGROUND_LOCATION", false))
        }
    }

    data class Location(
        val lat: Double,
        val long: Double,
        val provider: String,
        val speed: Float,
        val bearing: Float,
        /**
         * System.currentTimeMillis() when information was obtained
         */
        val time: Long,
        /**
         *  This should be taken from Location.time
         */
        val age: Long?,
        val accuracy: Float,
        val mock_location: Boolean,
        val satellites: Int?,
        @Expose
        val altitude: Double
    )
}

fun LocationInfo?.toDeviceInfoLocation(): DeviceInfo.Location? = if (this == null) null else {
    DeviceInfo.Location(
        lat = latitude,
        long = longitude,
        provider = provider,
        speed = speed,
        bearing = bearing,
        time = time,
        age = ageNanos / 1000000,
        accuracy = accuracy,
        mock_location = locationIsMocked,
        altitude = altitude,
        satellites = satellites
    )
}