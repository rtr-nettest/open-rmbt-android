package at.specure.location.cell

import androidx.lifecycle.LiveData
import javax.inject.Inject

class CellLocationLiveData @Inject constructor(private val watcher: CellLocationWatcher) : LiveData<CellLocationInfo?>(),
    CellLocationWatcher.CellLocationChangeListener {

    override fun onActive() {
        super.onActive()
        watcher.addListener(this)
    }

    override fun onInactive() {
        super.onInactive()
        watcher.removeListener(this)
    }

    override fun onCellLocationChanged(info: CellLocationInfo?) {
        postValue(info)
    }
}