package at.rtr.rmbt.android.ui.fragment.results

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentResultsBinding
import at.rtr.rmbt.android.ui.activity.HomeActivity
import at.rtr.rmbt.android.ui.activity.ResultFiltersActivity
import at.rtr.rmbt.android.ui.activity.SyncActivity
import at.rtr.rmbt.android.ui.fragment.BaseFragment
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import com.google.android.material.tabs.TabLayout

class ResultsFragment : BaseFragment() {

    override val layoutResId: Int = R.layout.fragment_results

    private val binding: FragmentResultsBinding by bindingLazy()

    private val fragments = listOf<Fragment>(HistoryFragment(), ResultsMapFragment())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null && !isAdded) {
            childFragmentManager.beginTransaction().add(binding.pager.id, fragments[0]).commitAllowingStateLoss()
        }

        activity?.window?.changeStatusBarColor(ToolbarTheme.WHITE)

        val tabNames = resources.getStringArray(R.array.title_results_tabs)
        tabNames.onEach {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(it))
        }

        childFragmentManager.beginTransaction().add(binding.pager.id, HistoryFragment()).commitAllowingStateLoss()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                childFragmentManager.beginTransaction().replace(binding.pager.id, fragments[tab?.position ?: 0])
                    .commitAllowingStateLoss()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        binding.iconFilter.setOnClickListener {
            val openMode =
                if (binding.tabLayout.selectedTabPosition == 0) ResultFiltersActivity.FilterMode.LIST else ResultFiltersActivity.FilterMode.MAP
            ResultFiltersActivity.start(this, openMode)
        }

        binding.iconSync.setOnClickListener {
            SyncActivity.start(this)
        }

        binding.emptyLink.setOnClickListener {
            HomeActivity.startWithFragment(requireContext(), HomeActivity.Companion.HomeNavigationTarget.HOME_FRAGMENT_TO_SHOW)
        }
    }

    fun onDataLoaded(empty: Boolean) {
        binding.empty = empty
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        fragments.onEach {
            it.onActivityResult(requestCode, resultCode, data)
        }
    }
}