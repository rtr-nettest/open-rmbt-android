package at.specure.database

import androidx.room.TypeConverter
import at.specure.info.TransportType
import at.specure.info.cell.CellTechnology
import at.specure.measurement.MeasurementState

class TypeConverter {

    @TypeConverter
    fun transportTypeToValue(transportType: TransportType): Int = transportType.value

    @TypeConverter
    fun valueToTransportType(value: Int): TransportType {
        TransportType.values().forEach { if (value == it.value) return it }
        throw IllegalArgumentException("Transport type $value not found")
    }

    @TypeConverter
    fun measurementStateToValue(measurementState: MeasurementState): Int = measurementState.ordinal

    @TypeConverter
    fun valueToMeasurementState(value: Int): MeasurementState {
        MeasurementState.values().forEach { if (value == it.ordinal) return it }
        throw IllegalArgumentException("Measurement state $value not found")
    }

    @TypeConverter
    fun cellTechnologyToValue(cellTechnology: CellTechnology): Int = cellTechnology.ordinal

    @TypeConverter
    fun valueToCellTechnology(value: Int): CellTechnology {
        CellTechnology.values().forEach { if (value == it.ordinal) return it }
        throw IllegalArgumentException("CellTechnology value $value not found")
    }
}