package at.specure.info.cell

enum class PrimaryDataSubscription(

    /**
     * Value that inform us about subscription ID type
     */
    val value: String
) {
    /**
     * Cell belongs to subscription ID which is primary data subscription
     */
    TRUE("true"),

    /**
     * Cell belongs to subscription ID which is not primary data subscription
     */
    FALSE("false"),

    /**
     * Unable to retrieve valid information about subscription ID or primary data subscription ID
     */
    UNKNOWN("unknown");

    companion object {
        fun fromString(type: String): PrimaryDataSubscription {
            values().forEach {
                if (it.value == type) {
                    return it
                }
            }
            return UNKNOWN
        }

        fun resolvePrimaryDataSubscriptionID(dataSubscriptionId: Int, cellSubscriptionId: Int?): PrimaryDataSubscription {
            return when {
                dataSubscriptionId == CellInfoWatcherImpl.INVALID_SUBSCRIPTION_ID || cellSubscriptionId == CellInfoWatcherImpl.INVALID_SUBSCRIPTION_ID -> PrimaryDataSubscription.UNKNOWN
                dataSubscriptionId != CellInfoWatcherImpl.INVALID_SUBSCRIPTION_ID && cellSubscriptionId == dataSubscriptionId -> PrimaryDataSubscription.TRUE
                else -> PrimaryDataSubscription.FALSE
            }
        }
    }
}