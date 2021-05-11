package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.View
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.ui.activity.ResultFiltersActivity
import at.rtr.rmbt.android.databinding.FragmentResultsBinding
import at.rtr.rmbt.android.ui.adapter.ResultsPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator

class ResultsFragment : BaseFragment() {

    override val layoutResId: Int = R.layout.fragment_results

    private val binding: FragmentResultsBinding by bindingLazy()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pagerAdapter = ResultsPagerAdapter(childFragmentManager, lifecycle)
        binding.pager.adapter = pagerAdapter
        binding.pager.isUserInputEnabled = false
        binding.pager.offscreenPageLimit = pagerAdapter.itemCount - 1

        val tabNames = resources.getStringArray(R.array.title_results_tabs)
        TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
            tab.text = tabNames[position]
        }.attach()

        binding.iconFilter.setOnClickListener {
            val openMode = if (binding.pager.currentItem == 0) ResultFiltersActivity.FilterMode.LIST else ResultFiltersActivity.FilterMode.MAP
            ResultFiltersActivity.start(activity, openMode)
        }
    }

}