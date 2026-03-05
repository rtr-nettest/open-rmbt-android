package at.specure.temperature

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import timber.log.Timber

class BatteryInfoReceiver : BroadcastReceiver() {
    // temperature in Celsius units in XXY format as XX.Y
    private var temp: Int? = null

    /**
     * temperature in Celsius or null if not acquired yet
     */
    fun getTemp(): Float? {
        temp?.let { temperature ->
            if (temp == Int.MIN_VALUE) return null

            return (temperature.toFloat() / 10f)
        }
        return null
    }

    override fun onReceive(arg0: Context?, intent: Intent) {
        Timber.d("Battery info updated")
        temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
    }
}