package at.specure.eval

data class PingStats(
    val average: Long?, // ms
    val median: Long?,  // ms
    val totalCountWithNulls: Int,
    val totalCountWithoutNulls: Int
)
