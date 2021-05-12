package at.rtr.rmbt.android.ui.fragment

import android.content.Intent
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

    val pagerAdapter = ResultsPagerAdapter(childFragmentManager, lifecycle)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.pager.adapter = pagerAdapter
        binding.pager.isUserInputEnabled = false
        binding.pager.offscreenPageLimit = pagerAdapter.itemCount - 1

        val tabNames = resources.getStringArray(R.array.title_results_tabs)
        TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
            tab.text = tabNames[position]
        }.attach()

        binding.iconFilter.setOnClickListener {
            val openMode = if (binding.pager.currentItem == 0) ResultFiltersActivity.FilterMode.LIST else ResultFiltersActivity.FilterMode.MAP
            ResultFiltersActivity.start(this, openMode)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        repeat(pagerAdapter.fragments.size) { onActivityResult(requestCode, resultCode, data) }
    }

}