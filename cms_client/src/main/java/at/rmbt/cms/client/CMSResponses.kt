package at.rmbt.cms.client

open class PageTranslationResponse(
    val id: String? = null,
    val name: String? = null,
    val language: String? = null,
    val content: String? = null
)

data class PageResponse(
    val translations: List<PageTranslationResponse>
) : PageTranslationResponse()