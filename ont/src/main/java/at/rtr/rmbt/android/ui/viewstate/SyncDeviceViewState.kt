package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableField

private const val KEY_CURRENT_DEVICE_SYNC_CODE = "KEY_DEVICE_SYNC_CODE"
private const val KEY_OTHER_DEVICE_SYNC_CODE = "KEY_OTHER_DEVICE_SYNC_CODE"
private const val KEY_VISIBILITY_STATE = "KEY_VISIBILITY_STATE"
private const val KEY_SYNCED_TITLE = "KEY_SYNCED_TITLE"
private const val KEY_SYNCED_TEXT = "KEY_SYNCED_TEXT"

class SyncDeviceViewState : ViewState {

    val currentDeviceSyncCode = ObservableField<String>()
    val otherDeviceSyncCode = ObservableField<String>()
    val visibilityState = ObservableField<VisibilityState>().apply { set(VisibilityState.DESCRIPTION) }
    val syncedTitle = ObservableField<String>()
    val syncedText = ObservableField<String>()

    override fun onRestoreState(bundle: Bundle?) {
        super.onRestoreState(bundle)

        bundle?.run {
            currentDeviceSyncCode.set(getString(KEY_CURRENT_DEVICE_SYNC_CODE))
            otherDeviceSyncCode.set(getString(KEY_OTHER_DEVICE_SYNC_CODE))
            visibilityState.set(VisibilityState.values()[getInt(KEY_VISIBILITY_STATE, VisibilityState.DESCRIPTION.ordinal)])
            syncedTitle.set(getString(KEY_SYNCED_TITLE))
            syncedText.set(getString(KEY_SYNCED_TEXT))
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        super.onSaveState(bundle)
        bundle?.run {
            putString(KEY_CURRENT_DEVICE_SYNC_CODE, currentDeviceSyncCode.get())
            putString(KEY_OTHER_DEVICE_SYNC_CODE, otherDeviceSyncCode.get())
            putInt(KEY_VISIBILITY_STATE, visibilityState.get()?.ordinal ?: VisibilityState.DESCRIPTION.ordinal)
            putString(KEY_SYNCED_TITLE, syncedTitle.get())
            putString(KEY_SYNCED_TEXT, syncedText.get())
        }
    }

    enum class VisibilityState {
        DESCRIPTION,
        SHOW_CODE,
        ENTER_CODE,
        SYNC_SUCCESS
    }
}