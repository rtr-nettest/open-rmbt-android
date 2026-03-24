package at.rtr.rmbt.android.viewmodel.viewData

import at.specure.data.Classification

data class CoverageMarkerDetailsData(
    val id: Long,
    val networkType: Int?,
    val networkTypeLabel: String?,
    val provider: String?,
    val signalStrength: Int?,
    val signalClass: Classification?,
    val pingMillis: Long?,
    val timestamp: Long?,
    val isNotFinished: Boolean,
)