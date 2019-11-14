package at.rtr.rmbt.android.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogLocationInfoBinding
import at.rtr.rmbt.android.di.Injector
import at.rtr.rmbt.android.util.listen
import at.specure.location.LocationInfoLiveData
import at.specure.location.LocationProviderState
import at.specure.location.LocationProviderStateLiveData
import javax.inject.Inject

class LocationInfoDialog : FullscreenDialog() {

    private lateinit var binding: DialogLocationInfoBinding

    @Inject
    lateinit var locationInfoLiveData: LocationInfoLiveData

    @Inject
    lateinit var locationProviderStateLiveData: LocationProviderStateLiveData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Injector.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_location_info, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationProviderStateLiveData.listen(this) {
            if (it != LocationProviderState.ENABLED) dismiss()
        }

        locationInfoLiveData.listen(this) {
            binding.locationInfo = it
        }
    }

    companion object {

        fun instance(): FullscreenDialog = LocationInfoDialog()
    }
}