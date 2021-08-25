package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.viewpager.widget.ViewPager
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentQosTestDetailPagerBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.adapter.QosTestDetailPagerAdapter
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.QosTestDetailPagerViewModel
import at.specure.result.QoSCategory

class QosTestDetailPagerFragment : BaseFragment() {

    private val qosTestDetailPagerViewModel: QosTestDetailPagerViewModel by viewModelLazy()
    private val binding: FragmentQosTestDetailPagerBinding by bindingLazy()
    private lateinit var qosTestDetailPagerAdapter: QosTestDetailPagerAdapter

    override val layoutResId = R.layout.fragment_qos_test_detail_pager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        qosTestDetailPagerViewModel.state.apply {

            this.testUUID = arguments?.getString(KEY_TEST_UUID) ?: throw IllegalArgumentException("Please pass test UUID")
            this.category = arguments?.getSerializable(KEY_QOS_CATEGORY) as QoSCategory
            arguments?.getInt(KEY_POSITION)?.let { this.position = it }
        }

        qosTestDetailPagerViewModel.qosTestItemsLiveData.listen(this) {

            qosTestDetailPagerAdapter = QosTestDetailPagerAdapter(requireContext(), requireActivity().supportFragmentManager, it)
            binding.viewPagerQosTestDetail.adapter = qosTestDetailPagerAdapter
            binding.tabLayoutQosTestDetail.setupWithViewPager(binding.viewPagerQosTestDetail)
            binding.viewPagerQosTestDetail.currentItem = qosTestDetailPagerViewModel.state.position
        }

        binding.viewPagerQosTestDetail.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                qosTestDetailPagerViewModel.state.position = position
            }
        })
    }

    companion object {

        private const val KEY_TEST_UUID: String = "KEY_TEST_UUID"
        private const val KEY_QOS_CATEGORY: String = "KEY_QOS_CATEGORY"
        private const val KEY_POSITION: String = "KEY_POSITION"

        fun newInstance(testUUID: String, category: QoSCategory, position: Int): QosTestDetailPagerFragment {

            Bundle().apply {

                putString(KEY_TEST_UUID, testUUID)
                putSerializable(KEY_QOS_CATEGORY, category)
                putInt(KEY_POSITION, position)
                val fragment = QosTestDetailPagerFragment()
                fragment.arguments = this
                return fragment
            }
        }
    }
}