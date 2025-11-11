package at.specure.measurement.coverage.domain.models

import at.specure.eval.PingStats

data class PingData(
    val pingStatistics: PingStats?,
    val error: Throwable?
)