package at.rtr.rmbt.android.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityResultFiltersBinding
import at.rtr.rmbt.android.databinding.ViewResultFiltersSectionBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.adapter.ResultFiltersAdapter
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.ResultListFiltersViewModel

class ResultFiltersActivity : BaseActivity() {

    lateinit var binding: ActivityResultFiltersBinding
    private val listViewModel: ResultListFiltersViewModel by viewModelLazy()

    private val adapters = mutableListOf<ResultFiltersAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_result_filters)

        val openMode = FilterMode.values()[intent?.extras?.getInt(KEY_OPEN_MODE) ?: 0]
        when (openMode) {
            FilterMode.LIST -> prepareListFilters()
            FilterMode.MAP -> prepareMapFilters()
        }

        binding.close.setOnClickListener {
            val data = Intent()
            when (openMode) {
                FilterMode.LIST -> {
                    val selectedDevices = adapters[1].selected.map { it.option }.toSet()
                    listViewModel.updateDeviceFilters(selectedDevices)

                    val selectedNetworks = adapters[0].selected.map { it.option }.toSet()
                    listViewModel.updateNetworkFilters(selectedNetworks)
                }
                FilterMode.MAP -> {
                }
            }
            setResult(RESULT_OK, data)
            finish()
        }
    }


    private fun prepareListFilters() {
        val networksBinding = ViewResultFiltersSectionBinding.inflate(layoutInflater, null, false)
        networksBinding.title.text = getString(R.string.text_filter_networks)
        binding.sections.addView(networksBinding.root)

        val devicesBinding = ViewResultFiltersSectionBinding.inflate(layoutInflater, null, false)
        devicesBinding.title.text = getString(R.string.text_filter_devices)
        binding.sections.addView(devicesBinding.root)

        listViewModel.devicesLiveData.listen(this) {
            val adapter = ResultFiltersAdapter(it)
            devicesBinding.list.adapter = adapter
            adapters.add(adapter)
            devicesBinding.root.requestLayout()
        }

        listViewModel.networksLiveData.listen(this) {
            val adapter = ResultFiltersAdapter(it)
            networksBinding.list.adapter = adapter
            adapters.add(adapter)
            networksBinding.root.requestLayout()
        }
    }

    private fun prepareMapFilters() {

    }

    companion object {
        const val CODE = 18194
        private const val KEY_OPEN_MODE = "key_mode"

        fun start(activity: Activity?, openMode: FilterMode) {
            val starter = Intent(activity, ResultFiltersActivity::class.java)
                .putExtra(KEY_OPEN_MODE, openMode.ordinal)
            activity?.startActivityForResult(starter, CODE)
        }
    }

    enum class FilterMode {
        LIST, MAP
    }
}