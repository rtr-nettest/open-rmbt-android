package at.specure.data

import androidx.room.TypeConverter
import at.rtr.rmbt.client.helper.TestStatus
import at.specure.info.TransportType
import at.specure.info.cell.CellTechnology
import at.specure.info.network.MobileNetworkType
import at.specure.measurement.MeasurementState

class TypeConverter {

    @TypeConverter
    fun transportTypeToValue(transportType: TransportType?): Int? = transportType?.value

    @TypeConverter
    fun valueToTransportType(value: Int?): TransportType? {
        if (value == null) return null
        TransportType.values().forEach { if (value == it.value) return it }
        throw IllegalArgumentException("Transport type $value not found")
    }

    @TypeConverter
    fun measurementStateToValue(measurementState: MeasurementState?): Int? = measurementState?.ordinal

    @TypeConverter
    fun valueToMeasurementState(value: Int?): MeasurementState? {
        if (value == null) return null
        MeasurementState.values().forEach { if (value == it.ordinal) return it }
        throw IllegalArgumentException("Measurement state $value not found")
    }

    @TypeConverter
    fun cellTechnologyToValue(cellTechnology: CellTechnology?): Int? = cellTechnology?.ordinal

    @TypeConverter
    fun valueToCellTechnology(value: Int?): CellTechnology? {
        if (value == null) return null
        CellTechnology.values().forEach { if (value == it.ordinal) return it }
        throw IllegalArgumentException("CellTechnology value $value not found")
    }

    @TypeConverter
    fun mobileNetworkTypeToValue(type: MobileNetworkType?): Int? = type?.intValue

    @TypeConverter
    fun valueToMobileNetworkType(value: Int?): MobileNetworkType? {
        if (value == null) return null
        MobileNetworkType.values().forEach { if (value == it.intValue) return it }
        throw IllegalArgumentException("Mobile network type $value not found")
    }

    @TypeConverter
    fun testStatusToValue(status: TestStatus?): Int? = status?.ordinal

    @TypeConverter
    fun valueToTestStatus(value: Int?): TestStatus? {
        if (value == null) return null
        TestStatus.values().forEach { if (value == it.ordinal) return it }
        throw IllegalArgumentException("Test status $value not found")
    }

    @TypeConverter
    fun networkTypeCompatToValue(type: NetworkTypeCompat): String = type.stringValue

    @TypeConverter
    fun valueToNetworkTypeCompat(value: String): NetworkTypeCompat = NetworkTypeCompat.fromString(value)

    @TypeConverter
    fun classificationToValue(classification: Classification): Int = classification.intValue

    @TypeConverter
    fun valueToClassification(value: Int): Classification = Classification.fromValue(value)
}