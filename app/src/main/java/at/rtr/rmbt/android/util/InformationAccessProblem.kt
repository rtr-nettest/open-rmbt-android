package at.rtr.rmbt.android.util

import at.rtr.rmbt.android.R

enum class InformationAccessProblem(val titleID: Int, val descriptionId: Int) {
    NO_PROBLEM(
        0,
        0
    ),
    MISSING_READ_PHONE_STATE_PERMISSION(
        R.string.label_some_permission_denied,
        R.string.label_some_permission_denied_explanation
    ),
    MISSING_LOCATION_PERMISSION(
        R.string.label_some_permission_denied,
        R.string.label_some_permission_denied_explanation
    ),
    MISSING_PRECISE_LOCATION_PERMISSION(
        R.string.label_some_permission_denied,
        R.string.label_some_permission_denied_explanation
    ),
    MISSING_BACKGROUND_LOCATION_PERMISSION(
        R.string.label_some_permission_denied,
        R.string.label_some_permission_denied_explanation
    ),
    MISSING_LOCATION_ENABLED(
        R.string.label_location_access_disabled,
        R.string.label_location_access_disabled_explanation
    )
}
