package at.specure.util

import at.rmbt.client.control.FilterBaseOptionResponse
import at.rmbt.client.control.FilterOperatorOptionResponse
import at.rmbt.client.control.FilterPeriodOptionResponse
import at.rmbt.client.control.FilterProviderOptionResponse
import at.rmbt.client.control.FilterStatisticOptionResponse
import at.rmbt.client.control.FilterTechnologyOptionResponse
import at.rmbt.client.control.MapTypeOptionsResponse
import at.rmbt.client.control.data.MapFilterType
import java.util.Calendar
import java.util.EnumMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterValuesStorage @Inject constructor() {

    private var types: LinkedHashMap<String, MapFilterType> = linkedMapOf()
    private var subTypes: Map<MapFilterType, List<MapTypeOptionsResponse>> = EnumMap(MapFilterType::class.java)
    private var statistics: Map<MapFilterType, List<FilterStatisticOptionResponse>> = EnumMap(MapFilterType::class.java)
    private var period: Map<MapFilterType, List<FilterPeriodOptionResponse>> = EnumMap(MapFilterType::class.java)
    private var technology: Map<MapFilterType, List<FilterTechnologyOptionResponse>> = EnumMap(MapFilterType::class.java)
    private var operator: Map<MapFilterType, List<FilterOperatorOptionResponse>> = EnumMap(MapFilterType::class.java)
    private var provider: Map<MapFilterType, List<FilterProviderOptionResponse>> = EnumMap(MapFilterType::class.java)
    private var time: Pair<Int, Int> = Calendar.getInstance().getCurrentLatestFinishedMonth()

    var titleSubtype: String = ""
    var titleStatistics: String = ""
    var titlePeriod: String = ""
    var titleTechnology: String = ""
    var titleOperator: String = ""
    var titleProvider: String = ""

    fun init(
        types: LinkedHashMap<String, MapFilterType>,
        subTypes: Map<MapFilterType, List<MapTypeOptionsResponse>>,
        statistics: Map<MapFilterType, List<FilterStatisticOptionResponse>>,
        period: Map<MapFilterType, List<FilterPeriodOptionResponse>>,
        technology: Map<MapFilterType, List<FilterTechnologyOptionResponse>>,
        operator: Map<MapFilterType, List<FilterOperatorOptionResponse>>,
        provider: Map<MapFilterType, List<FilterProviderOptionResponse>>,
        time: Pair<Int, Int>
    ) {
        this.types = types
        this.subTypes = subTypes
        this.statistics = statistics
        this.period = period
        this.technology = technology
        this.operator = operator
        this.provider = provider
        this.time = time
    }

    fun findStatisticalList(type: MapFilterType) = statistics[type]

    fun findPeriodsList(type: MapFilterType) = period[type]

    fun findOperatorList(type: MapFilterType) = operator[type]

    fun findProviderList(type: MapFilterType) = provider[type]

    fun findSubtypesList(type: MapFilterType) = subTypes[type]

    fun findTechnologyList(type: MapFilterType) = technology[type]

    fun findType(value: String) = types[value]

    fun findType(value: MapFilterType) = types.filterValues { it == value }.keys.first()

    fun findSubtype(value: String?, type: MapFilterType) =
        subTypes.getValue(type).firstOrNull { it.title == value } ?: subTypes.getValue(type).first()

    fun findStatistical(value: String?, type: MapFilterType) = findOption(value, type, statistics) as FilterStatisticOptionResponse

    fun findPeriod(value: String?, type: MapFilterType) = findOption(value, type, period) as FilterPeriodOptionResponse

    fun findTechnology(value: String?, type: MapFilterType) = findOption(value, type, technology) as FilterTechnologyOptionResponse

    fun findProvider(value: String?, type: MapFilterType) = findOption(value, type, provider) as FilterProviderOptionResponse

    fun findOperator(value: String?, type: MapFilterType) = findOption(value, type, operator) as FilterOperatorOptionResponse

    fun findStatisticalDefault(type: MapFilterType) = findDefaultOption(type, statistics).title

    fun findPeriodDefault(type: MapFilterType) = findDefaultOption(type, period).title

    fun findTechnologyDefault(type: MapFilterType) = findDefaultOption(type, technology).title

    fun findProviderDefault(type: MapFilterType) = findDefaultOption(type, provider).title

    fun findOperatorDefault(type: MapFilterType) = findDefaultOption(type, operator).title

    fun findTime(): Pair<Int, Int> {
        return time
    }

    fun findTimeDefault(): String {
        val currentMonth = Calendar.getInstance().getCurrentLatestFinishedMonth()
        return "${currentMonth.first}-${currentMonth.second}"
    }

    private fun findOption(value: String?, type: MapFilterType, data: Map<MapFilterType, List<FilterBaseOptionResponse>>) =
        data[type]?.find { it.title == value } ?: findDefaultOption(type, data)

    private fun findDefaultOption(type: MapFilterType, data: Map<MapFilterType, List<FilterBaseOptionResponse>>) =
        (data[type] ?: data.values.first()).first { it.default }
}