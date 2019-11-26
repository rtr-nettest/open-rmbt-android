package at.specure.test

import android.content.Context
import android.os.Build
import at.specure.core.BuildConfig
import at.specure.util.hasPermission
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.Locale
import java.util.TimeZone

class DeviceInfo(context: Context, val ndt: Boolean, val testCounter: Int, val location: Location?) {

    val plattform = "Android"
    @SerializedName("os_version")
    val osVersion = "${Build.VERSION.RELEASE}(${Build.VERSION.INCREMENTAL})"
    @SerializedName("api_level")
    val apiLevel = Build.VERSION.SDK_INT.toString()
    val device = Build.DEVICE
    val model = Build.MODEL
    val product = Build.PRODUCT
    val language = Locale.getDefault().language
    val timezone = TimeZone.getDefault().id

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

    @SerializedName("android_permission_status")
    val permissionsStatus: Map<String, Boolean> = mutableMapOf<String, Boolean>().apply {
        put(android.Manifest.permission.ACCESS_FINE_LOCATION, context.hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION))
        put(android.Manifest.permission.ACCESS_COARSE_LOCATION, context.hasPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION, context.hasPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION))
        } else {
            put("android.permission.ACCESS_BACKGROUND_LOCATION", false)
        }
    }

    data class Location(
        val lat: Double,
        val long: Double,
        val provider: String,
        val speed: Float,
        val bearing: Float,
        val time: Long,
        val age: Long?,
        val accuracy: Float,
        val mock_location: Boolean,
        @Expose
        val altitude: Double
    )
}