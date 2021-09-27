package at.specure.util

import android.content.Context
import at.rmbt.client.control.FilterOperatorOptionResponse
import at.rmbt.client.control.FilterPeriodOptionResponse
import at.rmbt.client.control.FilterProviderOptionResponse
import at.rmbt.client.control.FilterStatisticOptionResponse
import at.rmbt.client.control.FilterTechnologyOptionResponse
import at.rmbt.client.control.MapTypeOptionsResponse
import at.rmbt.client.control.data.MapFilterType
import javax.inject.Inject
import javax.inject.Singleton

private const val KEY_TYPE = "TYPE"
private const val KEY_SUBTYPE = "SUBTYPE"
private const val KEY_STATISTICAL = "STATISTICAL"
private const val KEY_TIME_RANGE = "TIME_RANGE"
private const val KEY_OPERATOR = "OPERATOR"
private const val KEY_PROVIDER = "PROVIDER"
private const val KEY_TECHNOLOGY = "TECHNOLOGY"
private const val KEY_TIME = "TIME"

@Singleton
class ActiveFilter @Inject constructor(context: Context, private val valuesStorage: FilterValuesStorage) {

    private val preferences = context.getSharedPreferences("map_filters.pref", Context.MODE_PRIVATE)

    var type: MapFilterType
        get() = MapFilterType.values()[preferences.getInt(KEY_TYPE, MapFilterType.MOBILE.ordinal)]
        set(value) = preferences.edit().putInt(KEY_TYPE, value.ordinal).apply()

    var subtype: MapTypeOptionsResponse
        get() = valuesStorage.findSubtype(preferences.getString(KEY_SUBTYPE, "Download"), type)
        set(value) = preferences.edit().putString(KEY_SUBTYPE, value.title).apply()

    var statistical: FilterStatisticOptionResponse
        get() = valuesStorage.findStatistical(preferences.getString(KEY_STATISTICAL, valuesStorage.findStatisticalDefault(type)), type)
        set(value) = preferences.edit().putString(KEY_STATISTICAL, value.title).apply()

    var timeRange: FilterPeriodOptionResponse
        get() = valuesStorage.findPeriod(preferences.getString(KEY_TIME_RANGE, valuesStorage.findPeriodDefault(type)), type)
        set(value) = preferences.edit().putString(KEY_TIME_RANGE, value.title).apply()

    var provider: FilterProviderOptionResponse
        get() = valuesStorage.findProvider(preferences.getString(KEY_PROVIDER, valuesStorage.findProviderDefault(type)), type)
        set(value) = preferences.edit().putString(KEY_PROVIDER, value.title).apply()

    var operator: FilterOperatorOptionResponse
        get() = valuesStorage.findOperator(preferences.getString(KEY_OPERATOR, valuesStorage.findOperatorDefault(type)), type)
        set(value) {
            preferences.edit().putString(KEY_OPERATOR, value.title).apply()
        }

    var technology: FilterTechnologyOptionResponse
        get() = valuesStorage.findTechnology(preferences.getString(KEY_TECHNOLOGY, valuesStorage.findTechnologyDefault(type)), type)
        set(value) = preferences.edit().putString(KEY_TECHNOLOGY, value.title).apply()

    var time: Pair<Int, Int>
        get() {
            val currentMonthYear = preferences.getString(KEY_TIME, valuesStorage.findTimeDefault())
            val parsedYear = currentMonthYear?.split("-")?.get(0)?.toInt() ?: 0
            val parsedMonth = currentMonthYear?.split("-")?.get(1)?.toInt() ?: 0
            return Pair<Int, Int>(parsedYear, parsedMonth)
        }
        set(value) = preferences.edit().putString(KEY_TIME, "${value.first}-${value.second}").apply()
}