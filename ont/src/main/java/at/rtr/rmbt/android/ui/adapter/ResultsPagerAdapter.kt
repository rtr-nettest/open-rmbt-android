package at.rtr.rmbt.android.ui.adapter

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import at.rtr.rmbt.android.ui.fragment.results.ResultsListFragment
import at.rtr.rmbt.android.ui.fragment.results.ResultsMapFragment

class ResultsPagerAdapter(manager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(manager, lifecycle) {

    val fragments = listOf(ResultsListFragment(), ResultsMapFragment())

    override fun getItemCount(): Int = fragments.size
    override fun createFragment(position: Int) = fragments[position]
}