package at.specure.eval

data class PingStats(
    val average: Double?, // ms
    val median: Double?,  // ms
    val totalCountWithNulls: Int,
    val totalCountWithoutNulls: Int
)
