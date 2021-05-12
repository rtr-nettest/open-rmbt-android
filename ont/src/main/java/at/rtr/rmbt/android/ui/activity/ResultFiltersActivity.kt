package at.rtr.rmbt.android.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
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

    private val adapters = mutableMapOf<String, ResultFiltersAdapter>()

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
                    val selectedDevices = adapters[getString(R.string.text_filter_devices)]!!.selected.map { it.option }.toSet()
                    listViewModel.updateDeviceFilters(selectedDevices)

                    val selectedNetworks = adapters[getString(R.string.text_filter_networks)]!!.selected.map { it.option }.toSet()
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
        adapters.clear()

        val networksBinding = ViewResultFiltersSectionBinding.inflate(layoutInflater, null, false)
        networksBinding.title.text = getString(R.string.text_filter_networks)
        val networkAdapter = ResultFiltersAdapter()
        networksBinding.list.adapter = networkAdapter
        adapters[getString(R.string.text_filter_networks)] = networkAdapter
        binding.sections.addView(networksBinding.root)

        val devicesBinding = ViewResultFiltersSectionBinding.inflate(layoutInflater, null, false)
        devicesBinding.title.text = getString(R.string.text_filter_devices)
        val devicesAdapter = ResultFiltersAdapter()
        devicesBinding.list.adapter = devicesAdapter
        adapters[getString(R.string.text_filter_devices)] = devicesAdapter
        binding.sections.addView(devicesBinding.root)

        listViewModel.devicesLiveData.listen(this) {
            devicesAdapter.items = it
            devicesBinding.root.requestLayout()
        }

        listViewModel.networksLiveData.listen(this) {
            networkAdapter.items = it
            networksBinding.root.requestLayout()
        }
    }

    private fun prepareMapFilters() {

    }

    companion object {
        const val CODE = 18194
        private const val KEY_OPEN_MODE = "key_mode"

        fun start(fragment: Fragment?, openMode: FilterMode) {
            val starter = Intent(fragment?.requireContext(), ResultFiltersActivity::class.java)
                .putExtra(KEY_OPEN_MODE, openMode.ordinal)
            fragment?.startActivityForResult(starter, CODE)
        }
    }

    enum class FilterMode {
        LIST, MAP
    }
}