package at.specure.result

enum class QoECategory(val categoryName: String) {
    QOE_UNKNOWN(""),
    QOE_AUDIO_STREAMING("streaming_audio_streaming"),
    QOE_VIDEO_SD("video_sd"),
    QOE_VIDEO_HD("video_hd"),
    QOE_VIDEO_UHD("video_uhd"),
    QOE_GAMING("gaming"),
    QOE_GAMING_CLOUD("gaming_cloud"),
    QOE_GAMING_STREAMING("gaming_streaming"),
    QOE_GAMING_DOWNLOAD("gaming_download"),
    QOE_VOIP("voip"),
    QOE_VIDEO_TELEPHONY("video_telephony"),
    QOE_VIDEO_CONFERENCING("video_conferencing"),
    QOE_MESSAGING("messaging"),
    QOE_WEB("web"),
    QOE_CLOUD("cloud");

    companion object {
        fun fromString(type: String): QoECategory {
            QoECategory.values().forEach { x ->
                if (x.categoryName == type) {
                    return x
                }
            }
            return QOE_UNKNOWN
        }
    }
}