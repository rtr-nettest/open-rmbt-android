package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle

/**
 * Basic interface for ViewStates
 */
interface ViewState {

    fun onRestoreState(bundle: Bundle?) {}

    fun onSaveState(bundle: Bundle?) {}
}