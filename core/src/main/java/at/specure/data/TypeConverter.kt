package at.specure.data

import androidx.room.TypeConverter
import at.rmbt.client.control.data.TestFinishReason
import at.rtr.rmbt.client.helper.TestStatus
import at.specure.data.entity.LoopModeState
import at.specure.data.entity.TestResultGraphItemRecord
import at.specure.info.TransportType
import at.specure.info.cell.CellTechnology
import at.specure.info.connectivity.ConnectivityState
import at.specure.info.network.MobileNetworkType
import at.specure.measurement.MeasurementState
import at.specure.measurement.signal.SignalMeasurementState
import at.specure.result.QoECategory
import at.specure.result.QoSCategory
import at.specure.test.DeviceInfo
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.json.JSONArray

class TypeConverter {

    private val gson = Gson()

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

    @TypeConverter
    fun jsonObjectToValue(jsonObject: JsonObject): String = jsonObject.toString()

    @TypeConverter
    fun valueToJsonObject(value: String): JsonObject = JsonParser().parse(value).asJsonObject

    @TypeConverter
    fun jsonArrayToValue(jsonArray: JSONArray): String = jsonArray.toString()

    @TypeConverter
    fun valueToJsonArray(value: String): JSONArray = JSONArray(value)

    @TypeConverter
    fun qoeCategoryToValue(type: QoECategory): String = type.categoryName

    @TypeConverter
    fun valueToQoeCategory(value: String): QoECategory = QoECategory.fromString(value)

    @TypeConverter
    fun finishReasonToInt(reason: TestFinishReason?): Int? = reason?.ordinal

    @TypeConverter
    fun intToFinishReason(value: Int?): TestFinishReason? {
        if (value == null) return null
        return TestFinishReason.values()[value]
    }

    @TypeConverter
    fun qosCategoryToValue(type: QoSCategory): String = type.categoryName

    @TypeConverter
    fun valueToQosCategory(value: String): QoSCategory = QoSCategory.fromString(value)

    @TypeConverter
    fun graphTypeToValue(type: TestResultGraphItemRecord.Type): Int = type.typeValue

    @TypeConverter
    fun valueToGraphType(value: Int): TestResultGraphItemRecord.Type = TestResultGraphItemRecord.Type.fromValue(value)

    @TypeConverter
    fun valueToLoopModeState(value: Int): LoopModeState = LoopModeState.fromValue(value)

    @TypeConverter
    fun loopModeStateToValue(state: LoopModeState): Int = state.valueInt

    @TypeConverter
    fun signalMeasurementStateToValue(state: SignalMeasurementState): Int = state.intValue

    @TypeConverter
    fun valueToSignalMeasurementState(value: Int): SignalMeasurementState = SignalMeasurementState.fromValue(value)

    @TypeConverter
    fun jsonToDeviceInfoLocation(json: String?): DeviceInfo.Location? {
        if (json == null) {
            return null
        }

        return gson.fromJson<DeviceInfo.Location>(json, DeviceInfo.Location::class.java)
    }

    @TypeConverter
    fun deviceInfoLocationToJson(location: DeviceInfo.Location?): String? = if (location == null) {
        null
    } else {
        gson.toJson(location)
    }

    @TypeConverter
    fun connectivityStateToValue(state: ConnectivityState): Int = state.ordinal

    @TypeConverter
    fun valueToConnectivityState(value: Int): ConnectivityState = ConnectivityState.values()[value]
}