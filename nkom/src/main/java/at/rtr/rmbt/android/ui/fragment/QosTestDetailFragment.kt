package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentQosTestDetailBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.adapter.QosTestGoalAdapter
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.QosTestDetailViewModel

class QosTestDetailFragment : BaseFragment() {

    private val qosTestDetailViewModel: QosTestDetailViewModel by viewModelLazy()
    private val binding: FragmentQosTestDetailBinding by bindingLazy()
    private val qosTestGoalAdapter: QosTestGoalAdapter by lazy { QosTestGoalAdapter() }

    override val layoutResId = R.layout.fragment_qos_test_detail

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.state = qosTestDetailViewModel.state

        qosTestDetailViewModel.state.apply {

            this.testUUID = arguments?.getString(KEY_TEST_UUID) ?: throw IllegalArgumentException("Please pass test UUID")
            this.testItemId.set(arguments?.getLong(KEY_TEST_ITEM_ID) ?: throw IllegalArgumentException("Please pass test Item ID"))
            this.testItemDescription.set(arguments?.getString(KEY_TEST_ITEM_DESCRIPTION))
            this.testItemDetail.set(arguments?.getString(KEY_TEST_ITEM_DETAILS))
            arguments?.getBoolean(KEY_TEST_SUCCESS)?.let { this.testSuccess.set(it) }
        }

        binding.recyclerViewTestGoals.apply {

            adapter = qosTestGoalAdapter
            val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            ContextCompat.getDrawable(context, R.drawable.history_item_divider)?.let {
                itemDecoration.setDrawable(it)
            }
            addItemDecoration(itemDecoration)
        }

        qosTestDetailViewModel.qosGoalsResultLiveData.listen(this) {
            qosTestDetailViewModel.state.qosTestGoalRecord.set(it)
            qosTestGoalAdapter.submitList(it)
        }
    }

    companion object {

        private const val KEY_TEST_UUID: String = "KEY_TEST_UUID"
        private const val KEY_TEST_ITEM_ID: String = "KEY_TEST_ITEM_ID"
        private const val KEY_TEST_ITEM_DESCRIPTION = "KEY_TEST_ITEM_DESCRIPTION"
        private const val KEY_TEST_ITEM_DETAILS = "KEY_TEST_ITEM_DETAILS"
        private const val KEY_TEST_SUCCESS = "KEY_TEST_SUCCESS"

        fun newInstance(
            testUUID: String,
            testItemId: Long,
            testItemDescription: String,
            testItemDetail: String,
            success: Boolean
        ): QosTestDetailFragment {

            Bundle().apply {
                putString(KEY_TEST_UUID, testUUID)
                putLong(KEY_TEST_ITEM_ID, testItemId)
                putString(KEY_TEST_ITEM_DESCRIPTION, testItemDescription)
                putString(KEY_TEST_ITEM_DETAILS, testItemDetail)
                putBoolean(KEY_TEST_SUCCESS, success)

                val fragment = QosTestDetailFragment()
                fragment.arguments = this
                return fragment
            }
        }
    }
}