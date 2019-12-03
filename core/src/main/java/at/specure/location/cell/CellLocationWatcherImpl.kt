package at.specure.location.cell

import android.telephony.CellLocation
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.telephony.gsm.GsmCellLocation
import java.util.Collections
import javax.inject.Inject

class CellLocationWatcherImpl @Inject constructor(private val telephonyManager: TelephonyManager) : CellLocationWatcher {

    private val listeners = Collections.synchronizedSet(mutableSetOf<CellLocationWatcher.CellLocationChangeListener>())
    private var _latestLocation: CellLocationInfo? = null

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
        val wasEmpty = listeners.isEmpty()
        listeners.add(listener)
        listener.onCellLocationChanged(_latestLocation)
        if (wasEmpty) {
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