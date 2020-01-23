package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableField

private const val KEY_CURRENT_DEVICE_SYNC_CODE = "KEY_DEVICE_SYNC_CODE"
private const val KEY_OTHER_DEVICE_SYNC_CODE = "KEY_OTHER_DEVICE_SYNC_CODE"

class SyncDeviceViewState : ViewState {

    val currentDeviceSyncCode = ObservableField<String>()
    val otherDeviceSyncCode = ObservableField<String>()

    override fun onRestoreState(bundle: Bundle?) {
        super.onRestoreState(bundle)

        bundle?.let {
            currentDeviceSyncCode.set(it.getString(KEY_CURRENT_DEVICE_SYNC_CODE))
            otherDeviceSyncCode.set(it.getString(KEY_OTHER_DEVICE_SYNC_CODE))
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        super.onSaveState(bundle)
        bundle?.let {
            it.putString(KEY_CURRENT_DEVICE_SYNC_CODE, currentDeviceSyncCode.get())
            it.putString(KEY_OTHER_DEVICE_SYNC_CODE, otherDeviceSyncCode.get())
        }
    }
}