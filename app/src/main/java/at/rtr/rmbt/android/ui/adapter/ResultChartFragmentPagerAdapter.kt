package at.rtr.rmbt.android.ui.adapter

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import at.rtr.rmbt.android.ui.fragment.ResultChartFragment

class ResultChartFragmentPagerAdapter(fragmentManager: FragmentManager) :
    FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val fragments = mutableMapOf<Int, Fragment>()
    private val NUM_ITEMS = 3

    override fun getItem(position: Int): Fragment {
        val fragment: Fragment = ResultChartFragment.newInstance(position)
        fragments[position] = fragment
        return fragment
    }

    override fun getCount(): Int {
        return NUM_ITEMS
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        super.destroyItem(container, position, `object`)
        fragments.remove(position)
    }

    fun getFragment(position: Int): Fragment? {
        return fragments[position]
    }
}