package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableField

private const val KEY_NETWORKS = "networks"
private const val KEY_DEVICES = "devices"

class ResultListFiltersViewState : ViewState {

    var networks: ObservableField<String> = ObservableField()
    var devices: ObservableField<String> = ObservableField()

    var defaultNetwors: Set<String>? = null
    var defaultDevices: Set<String>? = null

    var activeNetworks: Set<String>? = null
    var activeDevices: Set<String>? = null

    override fun onSaveState(bundle: Bundle?) {
        super.onSaveState(bundle)
        bundle?.apply {
            putString(KEY_NETWORKS, networks.get())
            putString(KEY_DEVICES, devices.get())
        }
    }

    override fun onRestoreState(bundle: Bundle?) {
        super.onRestoreState(bundle)
        bundle?.run {
            networks.set(getString(KEY_NETWORKS))
            devices.set(getString(KEY_DEVICES))
        }
    }
}