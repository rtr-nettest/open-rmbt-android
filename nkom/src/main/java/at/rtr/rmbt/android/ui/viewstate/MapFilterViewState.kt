package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import at.rmbt.client.control.FilterOperatorOptionResponse
import at.rmbt.client.control.FilterPeriodOptionResponse
import at.rmbt.client.control.FilterProviderOptionResponse
import at.rmbt.client.control.FilterStatisticOptionResponse
import at.rmbt.client.control.FilterTechnologyOptionResponse
import at.rmbt.client.control.MapTypeOptionsResponse

private const val KEY_TYPE = "type"
private const val KEY_TYPE_STRING = "type_str"
private const val KEY_SUBTYPE = "subtype"
private const val KEY_STATISTICAL = "statistical"
private const val KEY_PROVIDER = "provider"
private const val KEY_OPERATOR = "operator"
private const val KEY_TIME_RANGE = "timeRange"
private const val KEY_TECHNOLOGY = "technology"

private const val KEY_STATISTICAL_TITLE = "title_statistical"
private const val KEY_PROVIDER_TITLE = "title_provider"
private const val KEY_OPERATOR_TITLE = "title_operator"
private const val KEY_TIME_RANGE_TITLE = "title_timeRange"
private const val KEY_TECHNOLOGY_TITLE = "title_technology"

private const val KEY_OPERATOR_VISIBILITY = "operator_visibility"
private const val KEY_TECHNOLOGY_VISIBILITY = "technology_visibility"
private const val KEY_PROVIDER_VISIBILITY = "provider_visibility"

class MapFilterViewState : ViewState {

    var type: ObservableField<String> = ObservableField()
    var subtype: ObservableField<MapTypeOptionsResponse> = ObservableField()
    var statistical: ObservableField<FilterStatisticOptionResponse?> = ObservableField()
    var provider: ObservableField<FilterProviderOptionResponse> = ObservableField()
    var operator: ObservableField<FilterOperatorOptionResponse> = ObservableField()
    var timeRange: ObservableField<FilterPeriodOptionResponse> = ObservableField()
    var technology: ObservableField<FilterTechnologyOptionResponse> = ObservableField()

    var displayType: ObservableField<String> = ObservableField()
    var operatorVisibility = ObservableBoolean(true)
    var technologyVisibility = ObservableBoolean(true)
    var providerVisibility = ObservableBoolean(true)

    var statisticsTitle = ObservableField<String>()
    var periodTitle = ObservableField<String>()
    var operatorTitle = ObservableField<String>()
    var providerTitle = ObservableField<String>()
    var technologyTitle = ObservableField<String>()

    override fun onSaveState(bundle: Bundle?) {
        super.onSaveState(bundle)

        bundle?.apply {
            putParcelable(KEY_SUBTYPE, subtype.get())
            putString(KEY_TYPE, type.get())
            putParcelable(KEY_STATISTICAL, statistical.get())
            putParcelable(KEY_PROVIDER, provider.get())
            putParcelable(KEY_OPERATOR, operator.get())
            putParcelable(KEY_TIME_RANGE, timeRange.get())
            putParcelable(KEY_TECHNOLOGY, technology.get())
            putString(KEY_TYPE_STRING, displayType.get())

            putBoolean(KEY_OPERATOR_VISIBILITY, operatorVisibility.get())
            putBoolean(KEY_PROVIDER_VISIBILITY, providerVisibility.get())
            putBoolean(KEY_TECHNOLOGY_VISIBILITY, technologyVisibility.get())

            putString(KEY_STATISTICAL_TITLE, statisticsTitle.get())
            putString(KEY_PROVIDER_TITLE, providerTitle.get())
            putString(KEY_TIME_RANGE_TITLE, periodTitle.get())
            putString(KEY_OPERATOR_TITLE, operatorTitle.get())
            putString(KEY_TECHNOLOGY_TITLE, technologyTitle.get())
        }
    }

    override fun onRestoreState(bundle: Bundle?) {
        super.onRestoreState(bundle)

        bundle?.run {
            displayType.set(getString(KEY_TYPE_STRING))
            type.set(getString(KEY_TYPE))
            subtype.set(getParcelable(KEY_SUBTYPE))
            statistical.set(getParcelable(KEY_STATISTICAL))
            provider.set(getParcelable(KEY_PROVIDER))
            operator.set(getParcelable(KEY_OPERATOR))
            timeRange.set(getParcelable(KEY_TIME_RANGE))
            technology.set(getParcelable(KEY_TECHNOLOGY))

            operatorVisibility.set(getBoolean(KEY_OPERATOR_VISIBILITY))
            providerVisibility.set(getBoolean(KEY_PROVIDER_VISIBILITY))
            technologyVisibility.set(getBoolean(KEY_TECHNOLOGY_VISIBILITY))

            statisticsTitle.set(getString(KEY_STATISTICAL_TITLE))
            periodTitle.set(getString(KEY_TIME_RANGE_TITLE))
            operatorTitle.set(getString(KEY_OPERATOR_TITLE))
            providerTitle.set(getString(KEY_PROVIDER_TITLE))
            technologyTitle.set(getString(KEY_TECHNOLOGY_TITLE))
        }
    }
}