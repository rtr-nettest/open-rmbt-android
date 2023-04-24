package at.specure.location.cell

data class CellLocationInfo(
    val timestampMillis: Long,
    val timestampNanos: Long,
    val locationId: Long?,
    val areaCode: Int?,
    val scramblingCode: Int
)