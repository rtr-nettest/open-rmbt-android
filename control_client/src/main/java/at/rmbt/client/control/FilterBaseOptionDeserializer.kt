package at.rmbt.client.control

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class FilterBaseOptionDeserializer : JsonDeserializer<FilterBaseOptionResponse> {

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): FilterBaseOptionResponse? {
        val gson = Gson()
        json?.asJsonObject?.let {
            return when {
                it.has("statistical_method") -> gson.fromJson(json, FilterStatisticOptionResponse::class.java)
                it.has("technology") -> gson.fromJson(json, FilterTechnologyOptionResponse::class.java)
                it.has("provider") -> gson.fromJson(json, FilterProviderOptionResponse::class.java)
                it.has("period") -> gson.fromJson(json, FilterPeriodOptionResponse::class.java)
                it.has("operator") -> gson.fromJson(json, FilterOperatorOptionResponse::class.java)
                else -> throw IllegalStateException("Object $json cannot ba parsed as FilterBaseOptionResponse")
            }
        }
        return null
    }
}