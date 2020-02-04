package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.View
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
    // private val adapter: TestResultDetailAdapter by lazy { TestResultDetailAdapter() }
    private lateinit var qosTestDetailPagerAdapter: QosTestDetailPagerAdapter

    override val layoutResId = R.layout.fragment_qos_test_detail_pager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        qosTestDetailPagerViewModel.state.apply {

            this.testUUID = arguments?.getString(KEY_TEST_UUID) ?: throw IllegalArgumentException("Please pass test UUID")
            this.category = arguments?.getSerializable(KEY_QOS_CATEGORY) as QoSCategory
        }

        qosTestDetailPagerViewModel.qosTestItemsLiveData.listen(this) {

            qosTestDetailPagerAdapter = QosTestDetailPagerAdapter(requireContext(), requireActivity().supportFragmentManager, it)
            binding.viewPagerQosTestDetail.adapter = qosTestDetailPagerAdapter
            binding.tabLayoutQosTestDetail.setupWithViewPager(binding.viewPagerQosTestDetail)
        }
    }

    companion object {

        private const val KEY_TEST_UUID: String = "KEY_TEST_UUID"
        private const val KEY_QOS_CATEGORY: String = "KEY_QOS_CATEGORY"

        fun newInstance(testUUID: String, category: QoSCategory): QosTestDetailPagerFragment {

            Bundle().apply {

                putString(KEY_TEST_UUID, testUUID)
                putSerializable(KEY_QOS_CATEGORY, category)
                val fragment = QosTestDetailPagerFragment()
                fragment.arguments = this
                return fragment
            }
        }
    }
}