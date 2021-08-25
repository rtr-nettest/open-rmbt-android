package at.rtr.rmbt.android.ui.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.ui.fragment.QosTestDetailFragment
import at.specure.data.entity.QosTestItemRecord

class QosTestDetailPagerAdapter(
    private val context: Context,
    fragmentManager: FragmentManager,
    private val qosTestItems: List<QosTestItemRecord>
) :
    FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val fragments = mutableMapOf<Long, Fragment>()

    override fun getItem(position: Int): Fragment {

        val item = qosTestItems[position]

        return if (fragments.contains(item.qosTestId)) {
            fragments[item.qosTestId]!!
        } else {

            val fragment: Fragment =
                QosTestDetailFragment.newInstance(item.testUUID, item.qosTestId, item.testSummary, item.testDescription, item.success)
            fragments[item.qosTestId] = fragment
            fragment
        }
    }

    override fun getCount(): Int {
        return qosTestItems.size
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        super.destroyItem(container, position, obj)
        val qosTestId = qosTestItems[position].qosTestId
        fragments.remove(qosTestId)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return String.format(context.getString(R.string.qos_test_number), position + 1)
    }
}