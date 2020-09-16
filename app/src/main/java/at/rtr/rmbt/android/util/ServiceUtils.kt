package at.rtr.rmbt.android.util

import android.content.Context
import com.google.android.gms.common.GoogleApiAvailability
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability

fun Context.isHmsAvailable() : Boolean {
    return HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(this) == ConnectionResult.SUCCESS
}

fun Context.isGmsAvailable() : Boolean {
    return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == com.google.android.gms.common.ConnectionResult.SUCCESS
}