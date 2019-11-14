package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.View
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentMapBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.MapViewModel

class MapFragment : BaseFragment() {

    private val mapViewModel: MapViewModel by viewModelLazy()
    private val binding: FragmentMapBinding by bindingLazy()

    override val layoutResId = R.layout.fragment_map

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.state = mapViewModel.state

        mapViewModel.text.listen(this) {
            mapViewModel.state.text.set(it)
        }
    }
}