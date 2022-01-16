package at.rmbt.client.control

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class FilterBaseOptionDeserializerV2 : JsonDeserializer<MFTBaseResponseV2> {

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): MFTBaseResponseV2? {
        val gson = Gson()
        json?.asJsonObject?.let {
            return when {
                (!it.has("icon") || (it.get("icon").asString.isNotEmpty() && MapFilterTypeClass.MAP_FILTER_STATISTIC.stringValue == it.get("icon").asString)) -> gson.fromJson(json, MapFilterStatisticTypeResponseV2::class.java)
                (it.get("icon").asString.isNotEmpty() && MapFilterTypeClass.MAP_FILTER_PERIOD.stringValue == it.get("icon").asString) -> gson.fromJson(json, MapFilterPeriodTypeResponseV2::class.java)
                (it.get("icon").asString.isNotEmpty() && MapFilterTypeClass.MAP_OVERLAY_TYPE.stringValue == it.get("icon").asString) -> gson.fromJson(json, MapFilterOverlayTypeResponseV2::class.java)
                (it.get("icon").asString.isNotEmpty() && MapFilterTypeClass.MAP_FILTER_APPEARANCE.stringValue == it.get("icon").asString) -> gson.fromJson(json, MapFilterAppearanceTypeResponseV2::class.java)
                (it.get("icon").asString.isNotEmpty() && MapFilterTypeClass.MAP_FILTER_CARRIER.stringValue == it.get("icon").asString) -> gson.fromJson(json, MapFilterCarrierTypeResponseV2::class.java)
                it.has("provider") -> gson.fromJson(json, FilterProviderOptionResponse::class.java)
                it.has("operator") -> gson.fromJson(json, FilterOperatorOptionResponse::class.java)
                else -> throw IllegalStateException("Object $json cannot ba parsed as FilterBaseOptionResponse")
            }
        }
        return null
    }
}