package at.specure.info.ip

import at.rmbt.client.control.IpEndpointProvider
import at.rmbt.util.Maybe
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaptivePortal @Inject constructor(private val ipEndpointProvider: IpEndpointProvider) {
    var captivePortalStatus: CaptivePortalStatus = CaptivePortalStatus.NOT_TESTED
    private var isCaptivePortalTestRunning = false

    fun resetCaptivePortalStatus() {
        captivePortalStatus = CaptivePortalStatus.NOT_TESTED
    }

    fun checkForCaptivePortal() {
        if (!isCaptivePortalTestRunning) {
            isCaptivePortalTestRunning = true
            captivePortalStatus = CaptivePortalStatus.TESTING
            val status = isWalledGardenConnection()
            captivePortalStatus = if (status.ok && status.success) CaptivePortalStatus.FOUND else CaptivePortalStatus.NOT_FOUND
            Timber.e("CPS detected: $status")
            isCaptivePortalTestRunning = false
        }
    }

    private fun isWalledGardenConnection(): Maybe<Boolean> {
        var urlConnection: HttpURLConnection? = null
        try {
            Timber.i("checking for walled garden...")
            val url = URL(ipEndpointProvider.captivePortalWalledGardenUrl)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.instanceFollowRedirects = false
            urlConnection.connectTimeout = WALLED_GARDEN_SOCKET_TIMEOUT_MS
            urlConnection.readTimeout = WALLED_GARDEN_SOCKET_TIMEOUT_MS
            urlConnection.useCaches = false
            urlConnection.inputStream
            Timber.d("check completed, response: ${urlConnection.responseCode}")
            // We got a valid response, but not from the real google
            return Maybe(urlConnection.responseCode != 204)
        } catch (e: IOException) {
            e.printStackTrace()
            return Maybe(false)
        } finally {
            urlConnection?.disconnect()
        }
    }

    enum class CaptivePortalStatus {
        NOT_TESTED,
        FOUND,
        NOT_FOUND,
        TESTING;
    }

    companion object {
        const val WALLED_GARDEN_SOCKET_TIMEOUT_MS = 10000
    }
}