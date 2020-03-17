package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rtr.rmbt.android.BuildConfig
import at.rtr.rmbt.android.util.ConfigValue
import javax.inject.Inject

class ConfigCheckViewModel @Inject constructor() : BaseViewModel() {

    private val _incorrectValuesLiveData = MutableLiveData<Map<String, String>>()

    val incorrectValuesLiveData: LiveData<Map<String, String>>
        get() = _incorrectValuesLiveData

    fun checkConfig() {
        val fields = BuildConfig::class.java.declaredFields
        val accessObject = Any()
        val incorrectValues = fields
            .filter {
                it.type.name == ConfigValue::class.java.name
            }
            .map {
                it.get(accessObject) as ConfigValue
            }
            .filter {
                it.value.contains("example")
            }
            .associateBy {
                it.name
            }
            .mapValues {
                it.value.value
            }

        if (incorrectValues.isNotEmpty()) {
            _incorrectValuesLiveData.postValue(incorrectValues)
        }
    }
}