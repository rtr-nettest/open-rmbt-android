package at.specure.location.cell

interface CellLocationWatcher {

    val latestLocation: CellLocationInfo?

    fun addListener(listener: CellLocationChangeListener)

    fun getCellLocationFromTelephony(): CellLocationInfo?

    fun removeListener(listener: CellLocationChangeListener)

    interface CellLocationChangeListener {

        fun onCellLocationChanged(info: CellLocationInfo?)
    }
}