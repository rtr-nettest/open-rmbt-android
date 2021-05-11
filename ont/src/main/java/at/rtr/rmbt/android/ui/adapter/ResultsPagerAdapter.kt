package at.rtr.rmbt.android.ui.adapter

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import at.rtr.rmbt.android.ui.fragment.ResultsListFragment
import at.rtr.rmbt.android.ui.fragment.ResultsMapFragment

class ResultsPagerAdapter(manager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(manager, lifecycle) {

    private val fragments = listOf(ResultsListFragment(), ResultsMapFragment())

    override fun getItemCount(): Int = fragments.size
    override fun createFragment(position: Int) = fragments[position]
}