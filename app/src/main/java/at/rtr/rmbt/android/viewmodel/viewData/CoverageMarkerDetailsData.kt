package at.rtr.rmbt.android.viewmodel.viewData

data class CoverageMarkerDetailsData(
    val id: Long,
    val networkType: Int?,
    val networkTypeLabel: String?,
    val provider: String?,
    val signalStrength: String?,
    val signalClass: Int?,
    val pingMillis: Long?,
    val timestamp: Long?,
)