package at.rtr.rmbt.android.ui.adapter

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import at.rtr.rmbt.android.ui.fragment.ResultChartFragment
import at.specure.data.NetworkTypeCompat
import at.specure.data.entity.TestResultGraphItemRecord

class ResultChartFragmentPagerAdapter(
    fragmentManager: FragmentManager,
    private val openTestUUID: String,
    private val networkTypeCompat: NetworkTypeCompat,
    private val type: TestResultGraphItemRecord.Type
) :
    FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val fragments = mutableMapOf<TestResultGraphItemRecord.Type, Fragment>()

    override fun getItem(position: Int): Fragment {
        return if (fragments.contains(type)) {
            fragments[type]!!
        } else {
            val fragment: Fragment = ResultChartFragment.newInstance(type, openTestUUID, networkTypeCompat)
            fragments[type] = fragment
            fragment
        }
    }

    override fun getCount(): Int {
        return 1
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        super.destroyItem(container, position, obj)
        val type = TestResultGraphItemRecord.Type.values()[position]
        fragments.remove(type)
    }
}