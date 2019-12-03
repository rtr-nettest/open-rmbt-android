package at.specure.location.cell

import android.content.Context
import android.telephony.CellLocation
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.telephony.gsm.GsmCellLocation
import at.specure.util.hasPermission
import java.util.Collections
import javax.inject.Inject

class CellLocationWatcherImpl @Inject constructor(private val context: Context, private val telephonyManager: TelephonyManager) :
    CellLocationWatcher {

    private val listeners = Collections.synchronizedSet(mutableSetOf<CellLocationWatcher.CellLocationChangeListener>())
    private var _latestLocation: CellLocationInfo? = null
    private var isRegistered = false

    private val locationListener = object : PhoneStateListener() {
        override fun onCellLocationChanged(location: CellLocation?) {
            _latestLocation = when (location) {
                is GsmCellLocation -> {
                    CellLocationInfo(
                        timestampMillis = System.currentTimeMillis(),
                        timestampNanos = System.nanoTime(),
                        locationId = location.cid,
                        areaCode = location.lac,
                        scramblingCode = location.psc
                    )
                }
                else -> {
                    null
                }
            }

            listeners.forEach {
                it.onCellLocationChanged(_latestLocation)
            }
        }
    }

    override val latestLocation: CellLocationInfo?
        get() = _latestLocation

    override fun addListener(listener: CellLocationWatcher.CellLocationChangeListener) {
        val needToRegister = listeners.isEmpty() || !isRegistered
        listeners.add(listener)
        listener.onCellLocationChanged(_latestLocation)
        if (needToRegister && context.hasPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
            telephonyManager.listen(locationListener, PhoneStateListener.LISTEN_CELL_LOCATION)
        }
    }

    override fun removeListener(listener: CellLocationWatcher.CellLocationChangeListener) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            telephonyManager.listen(locationListener, PhoneStateListener.LISTEN_NONE)
        }
    }
}