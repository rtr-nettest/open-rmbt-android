package at.specure.location.cell

data class CellLocationInfo(
    val timestampMillis: Long,
    val timestampNanos: Long,
    val locationId: Int?,
    val areaCode: Int?,
    val scramblingCode: Int
)