package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentQosTestsSummaryBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.adapter.QosTestSummaryAdapter
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.QosTestsSummaryViewModel
import at.specure.result.QoSCategory

class QosTestsSummaryFragment : BaseFragment() {

    private val qosTestsSummaryViewModel: QosTestsSummaryViewModel by viewModelLazy()
    private val binding: FragmentQosTestsSummaryBinding by bindingLazy()
    private val qosTestSummaryAdapter: QosTestSummaryAdapter by lazy { QosTestSummaryAdapter() }

    override val layoutResId = R.layout.fragment_qos_tests_summary

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.state = qosTestsSummaryViewModel.state

        qosTestsSummaryViewModel.state.apply {

            this.testUUID = arguments?.getString(KEY_TEST_UUID) ?: throw IllegalArgumentException("Please pass test UUID")
            this.categoryDescription.set(arguments?.getString(KEY_QOS_CATEGORY_DESCRIPTION) ?: throw IllegalStateException("Please pass category description"))
            this.category = arguments?.getSerializable(KEY_QOS_CATEGORY) as QoSCategory
        }

        binding.recyclerViewTests.apply {

            adapter = qosTestSummaryAdapter
            val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            ContextCompat.getDrawable(context, R.drawable.history_item_divider)?.let {
                itemDecoration.setDrawable(it)
            }
            binding.recyclerViewTests.addItemDecoration(itemDecoration)
        }

        qosTestsSummaryViewModel.qosTestItemsLiveData.listen(this) {

            qosTestsSummaryViewModel.state.qosTestItemRecords.set(it)
            qosTestSummaryAdapter.submitList(it)
        }

        qosTestSummaryAdapter.actionCallback = {

            val fragment = QosTestDetailPagerFragment.newInstance(qosTestsSummaryViewModel.state.testUUID, qosTestsSummaryViewModel.state.category, it)
            requireActivity().supportFragmentManager.beginTransaction()
                .addToBackStack(fragment.javaClass.name)
                .replace(R.id.fragment_content, fragment)
                .commit()
        }
    }

    companion object {

        private const val KEY_TEST_UUID: String = "KEY_TEST_UUID"
        private const val KEY_QOS_CATEGORY: String = "KEY_QOS_CATEGORY"
        private const val KEY_QOS_CATEGORY_DESCRIPTION: String = "KEY_QOS_CATEGORY_DESCRIPTION"
        private const val KEY_CATEGORY_NAME: String = "KEY_CATEGORY_NAME"

        fun newInstance(testUUID: String?, categoryDescription: String?, category: QoSCategory, categoryName: String?): QosTestsSummaryFragment {

            Bundle().apply {

                putString(KEY_TEST_UUID, testUUID)
                putSerializable(KEY_QOS_CATEGORY, category)
                putString(KEY_QOS_CATEGORY_DESCRIPTION, categoryDescription)
                putString(KEY_CATEGORY_NAME, categoryName)
                val fragment = QosTestsSummaryFragment()
                fragment.arguments = this
                return fragment
            }
        }
    }
}