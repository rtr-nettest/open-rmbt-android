package at.specure.config

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

const val KEY_IPV4_PRIVATE_ADDRESS = "KEY_IPV4_PRIVATE_ADDRESS"
const val KEY_IPV4_PUBLIC_ADDRESS = "KEY_IPV4_PUBLIC_ADDRESS"
const val KEY_IPV6_PRIVATE_ADDRESS = "KEY_IPV6_PRIVATE_ADDRESS"
const val KEY_IPV6_PUBLIC_ADDRESS = "KEY_IPV6_PUBLIC_ADDRESS"

@Singleton
class IpAddressSettings @Inject constructor(context: Context) {

    private val preferences = context.getSharedPreferences("ip_settings.pref", Context.MODE_PRIVATE)

    var lastPrivateIpv4: String = ""
        set(value) {
            field = value
            preferences.edit().putString(KEY_IPV4_PRIVATE_ADDRESS, value).apply()
        }
        get() = preferences.getString(KEY_IPV4_PRIVATE_ADDRESS, "") ?: ""

    var lastPublicIpv4: String = ""
        set(value) {
            field = value
            preferences.edit().putString(KEY_IPV4_PUBLIC_ADDRESS, value).apply()
        }
        get() = preferences.getString(KEY_IPV4_PUBLIC_ADDRESS, "") ?: ""

    var lastPrivateIpv6: String = ""
        set(value) {
            field = value
            preferences.edit().putString(KEY_IPV6_PRIVATE_ADDRESS, value).apply()
        }
        get() = preferences.getString(KEY_IPV6_PRIVATE_ADDRESS, "") ?: ""

    var lastPublicIpv6: String = ""
        set(value) {
            field = value
            preferences.edit().putString(KEY_IPV6_PUBLIC_ADDRESS, value).apply()
        }
        get() = preferences.getString(KEY_IPV6_PUBLIC_ADDRESS, "") ?: ""
}