package at.rtr.rmbt.android.map.wrapper

import com.google.android.gms.maps.model.Tile

interface TileWrapperProvider {
    fun getTileW(x: Int, y: Int, zoom: Int): TileW
}

data class TileW(val width: Int, val height: Int, val byteArray: ByteArray?) {

    fun toGMSTile(): Tile {
        return Tile(width, height, byteArray)
    }
}