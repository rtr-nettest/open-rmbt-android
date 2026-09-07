package at.rtr.rmbt.android.util

/** How many times a usage-terms screen is shown to an expert-mode user since installation. */
const val EXPERT_TERMS_DISPLAY_LIMIT = 1

/** How many times a usage-terms screen is shown to a non-expert user since installation. */
const val NON_EXPERT_TERMS_DISPLAY_LIMIT = 10

/**
 * The number of times a usage-terms screen may be shown, based on the CURRENT mode. The count itself
 * is stored per screen and shared across modes, so - as required - a user who switches between expert
 * and non-expert still sees at most 10 terms screens in total while a non-expert. For example, an
 * expert who has already seen it once (count 1) will see it again after switching to non-expert
 * (limit 10), up to 10 total.
 */
fun termsDisplayLimit(isExpert: Boolean): Int =
    if (isExpert) EXPERT_TERMS_DISPLAY_LIMIT else NON_EXPERT_TERMS_DISPLAY_LIMIT
