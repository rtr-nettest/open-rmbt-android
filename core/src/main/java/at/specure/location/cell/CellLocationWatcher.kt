package at.specure.location.cell

interface CellLocationWatcher {

    val latestLocation: CellLocationInfo?

    fun addListener(listener: CellLocationChangeListener)

    fun removeListener(listener: CellLocationChangeListener)

    interface CellLocationChangeListener {

        fun onCellLocationChanged(info: CellLocationInfo?)
    }
}