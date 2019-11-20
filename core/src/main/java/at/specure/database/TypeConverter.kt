package at.specure.database

import androidx.room.TypeConverter
import at.specure.info.TransportType

class TypeConverter {

    @TypeConverter
    fun transportTypeToValue(transportType: TransportType): Int = transportType.value

    @TypeConverter
    fun valueToTransportType(value: Int): TransportType {
        TransportType.values().forEach { if (value == it.value) return it }
        throw IllegalArgumentException("Transport type not found $value")
    }
}