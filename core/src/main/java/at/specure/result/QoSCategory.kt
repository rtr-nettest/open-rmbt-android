package at.specure.result

import java.util.Locale

enum class QoSCategory(val categoryName: String) {
    QOS_UNKNOWN(""),
    QOS_TRACEROUTE("TRACEROUTE"),
    QOS_TRACEROUTE_MASKED("TRACEROUTE_MASKED"),
    QOS_VOIP("VOIP"),
    QOS_UDP("UDP"),
    QOS_TCP("TCP"),
    QOS_DNS("DNS"),
    QOS_NON_TRANSPARENT_PROXY("NON_TRANSPARENT_PROXY"),
    QOS_HTTP_PROXY("HTTP_PROXY"),
    QOS_WEBSITE("WEBSITE");

    companion object {
        fun fromString(type: String): QoSCategory {
            QoSCategory.values().forEach { x ->
                if (x.categoryName.contentEquals(type.uppercase(Locale.ROOT))) {
                    return x
                }
            }
            return QOS_UNKNOWN
        }
    }
}