package at.rtr.rmbt.android.ui.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import at.rmbt.client.control.data.MapPresentationType
import at.rmbt.client.control.data.MapStyleType
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.LayoutMapLayersBinding
import at.rtr.rmbt.android.util.args

class MapLayersDialog : FullscreenDialog() {

    override val gravity: Int = Gravity.BOTTOM

    override val dimBackground: Boolean = false

    private lateinit var binding: LayoutMapLayersBinding

    private val callback: Callback?
        get() = when {
            targetFragment is Callback -> targetFragment as Callback
            activity is Callback -> activity as Callback
            else -> null
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.layout_map_layers, container, false)
        with(arguments?.getInt(KEY_STYLE)) {
            if (this != null && this != NO_VALUE) {
                binding.mapStyle = MapStyleType.values()[this]
            }
        }
        with(arguments?.getInt(KEY_TYPE)) {
            if (this != null && this != NO_VALUE) {
                binding.mapType = MapPresentationType.values()[this]
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.standard.setOnClickListener { processSelectedStyleOption(MapStyleType.STANDARD) }
        binding.satellite.setOnClickListener { processSelectedStyleOption(MapStyleType.SATELLITE) }
        binding.hybrid.setOnClickListener { processSelectedStyleOption(MapStyleType.HYBRID) }

        if (arguments?.getBoolean(KEY_NO_SATELLITE_OR_HYBRID) == true) {
            binding.satellite.isVisible = false
            binding.hybrid.isVisible = false
        }

        binding.heatmap.setOnClickListener { processSelectedTypeOption(MapPresentationType.AUTOMATIC) }
        binding.points.setOnClickListener { processSelectedTypeOption(MapPresentationType.POINTS) }
        binding.community.setOnClickListener { processSelectedTypeOption(MapPresentationType.COMMUNITIES) }

        binding.iconClose.setOnClickListener { dismiss() }
    }

    private fun processSelectedStyleOption(option: MapStyleType) {
        callback?.onStyleSelected(option)
        binding.mapStyle = option
    }

    private fun processSelectedTypeOption(option: MapPresentationType) {
        callback?.onTypeSelected(option)
        binding.mapType = option
    }

    companion object {

        const val KEY_STYLE = "STYLE"
        const val KEY_TYPE = "TYPE"
        const val KEY_NO_SATELLITE_OR_HYBRID = "KEY_NO_SATELLITE_OR_HYBRID"

        private const val NO_VALUE = -1

        fun instance(
            fragment: Fragment? = null,
            requestCode: Int? = NO_VALUE,
            activeStyle: Int? = NO_VALUE,
            activeType: Int? = NO_VALUE,
            noSatelliteOrHybrid: Boolean = false
        ): FullscreenDialog =
            MapLayersDialog()
                .apply {
                    requestCode?.let { setTargetFragment(fragment, it) }
                    args {
                        activeStyle?.let { putInt(KEY_STYLE, it) }
                        activeType?.let { putInt(KEY_TYPE, it) }
                        putBoolean(KEY_NO_SATELLITE_OR_HYBRID, noSatelliteOrHybrid)
                    }
                }
    }

    interface Callback {
        fun onStyleSelected(style: MapStyleType)
        fun onTypeSelected(type: MapPresentationType)
    }
}