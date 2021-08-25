package at.rtr.rmbt.android.ui.activity

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityMeasurementServerSelectionBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.adapter.MeasurementServerSelectionAdapter
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.MeasurementServerSelectionViewModel

/**
 * Used for Home Slider.
 */
class MeasurementServerSelectionActivity : BaseActivity() {

    private lateinit var binding: ActivityMeasurementServerSelectionBinding
    private val viewModel: MeasurementServerSelectionViewModel by viewModelLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_measurement_server_selection)

        val adapter = MeasurementServerSelectionAdapter(viewModel.measurementServers, viewModel::markAsSelected)
        binding.servers.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
        binding.servers.adapter = adapter

        viewModel.selected.listen(this) { adapter.selected = it }

        binding.close.setOnClickListener { onBackPressed() }
    }
}