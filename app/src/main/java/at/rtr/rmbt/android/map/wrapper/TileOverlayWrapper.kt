package at.rtr.rmbt.android.map.wrapper

interface TileOverlayWrapper {
    fun remove()
}

class HMSOverlayWrapper(private val tileOverlay: com.huawei.hms.maps.model.TileOverlay?) : TileOverlayWrapper {
    override fun remove() {
        tileOverlay?.remove()
    }
}

class GMSOverlayWrapper(private val tileOverlay: com.google.android.gms.maps.model.TileOverlay?) : TileOverlayWrapper {
    override fun remove() {
        tileOverlay?.remove()
    }
}

class EmptyTileOverlay : TileOverlayWrapper {
    override fun remove() {}
}