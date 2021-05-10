package at.rtr.rmbt.android.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentBasicResultBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.util.TestUuidType
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.BasicResultViewModel
import at.specure.data.Classification
import at.specure.data.NetworkTypeCompat
import at.specure.data.entity.TestResultGraphItemRecord
import at.specure.data.entity.TestResultRecord
import timber.log.Timber

class BasicResultFragment : BaseFragment() {

    private val viewModel: BasicResultViewModel by viewModelLazy()
    private val binding: FragmentBasicResultBinding by bindingLazy()

    override val layoutResId = R.layout.fragment_basic_result

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.state = viewModel.state

        val uuidTypeOrdinal = arguments?.getInt(KEY_UUID_TYPE) ?: throw IllegalArgumentException("Please pass UUID type")
        viewModel.state.useLatestResults = arguments?.getBoolean(KEY_RELOAD_HISTORY, false) ?: false
        viewModel.state.uuidType = TestUuidType.values()[uuidTypeOrdinal]
        viewModel.state.testUUID = arguments?.getString(KEY_TEST_UUID) ?: throw IllegalArgumentException("Please pass test UUID")

        when (viewModel.state.uuidType) {
            TestUuidType.TEST_UUID -> {
                viewModel.testServerResultLiveData.listen(this) { result ->
                    viewModel.state.testResult.set(result)

                    loadGraphItems(viewModel.state.downloadGraphData, viewModel.downloadGraphLiveData, TestResultGraphItemRecord.Type.DOWNLOAD)
                    loadGraphItems(viewModel.state.uploadGraphData, viewModel.uploadGraphLiveData, TestResultGraphItemRecord.Type.UPLOAD)
                }
            }
            TestUuidType.LOOP_UUID -> {
                binding.downloadChart.visibility = View.GONE
                binding.uploadChart.visibility = View.GONE
                viewModel.loopResultLiveData.listen(this) { result ->
                    result?.let {
                        viewModel.state.testResult.set(
                            TestResultRecord(
                                uuid = result.loopUuid,
                                uploadSpeedKbs = (result.uploadMedian * 1000).toLong(),
                                downloadSpeedKbs = (result.downloadMedian * 1000).toLong(),
                                pingMillis = result.pingMedian.toDouble(),
                                clientOpenUUID = "",
                                testOpenUUID = "",
                                timezone = "",
                                shareText = "",
                                shareTitle = "",
                                timestamp = 0L,
                                timeText = "",
                                networkTypeRaw = 0,
                                networkTypeText = "",
                                uploadClass = Classification.NONE,
                                downloadClass = Classification.NONE,
                                signalClass = Classification.NONE,
                                pingClass = Classification.NONE,
                                networkType = NetworkTypeCompat.TYPE_UNKNOWN,
                                isLocalOnly = false,
                                locationText = null,
                                longitude = null,
                                latitude = null,
                                networkProviderName = null,
                                networkName = null,
                                signalStrength = null
                            )
                        )
                    }
                }
            }
        }

        viewModel.loadingLiveData.listen(this) {
            if (viewModel.state.testResult.get() == null) {
                binding.textFailedToLoad.visibility = if (it) View.GONE else View.VISIBLE
            } else {
                binding.textFailedToLoad.visibility = View.GONE
            }
        }
        Timber.d("history loading results from $this")
        viewModel.loadTestResults()
    }

    private fun loadGraphItems(
        graphLoadedData: List<TestResultGraphItemRecord>?,
        graphLiveData: LiveData<List<TestResultGraphItemRecord>>,
        type: TestResultGraphItemRecord.Type
    ) {
        if (graphLoadedData.isNullOrEmpty()) {
            graphLiveData.listen(this) {
                when (type) {
                    TestResultGraphItemRecord.Type.DOWNLOAD -> viewModel.state.downloadGraphData = it
                    TestResultGraphItemRecord.Type.UPLOAD -> viewModel.state.uploadGraphData = it
                }
                showGraphItems(type)
            }
        } else {
            showGraphItems(type)
        }
    }

    private fun showGraphItems(type: TestResultGraphItemRecord.Type) {
        val isActivityInForeground = this.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        if (isActivityInForeground) {
            when (type) {
                TestResultGraphItemRecord.Type.DOWNLOAD -> {
                    binding.downloadChart.addResultGraphItems(
                        viewModel.state.downloadGraphData,
                        viewModel.state.testResult.get()?.networkType ?: NetworkTypeCompat.TYPE_UNKNOWN
                    )
                }
                TestResultGraphItemRecord.Type.UPLOAD -> {
                    binding.uploadChart.addResultGraphItems(
                        viewModel.state.uploadGraphData,
                        viewModel.state.testResult.get()?.networkType ?: NetworkTypeCompat.TYPE_UNKNOWN
                    )
                }
            }
        }
    }

    companion object {

        private const val KEY_TEST_UUID: String = "KEY_TEST_UUID"
        private const val KEY_UUID_TYPE: String = "KEY_UUID_TYPE"
        private const val KEY_RELOAD_HISTORY: String = "KEY_RELOAD_HISTORY"

        fun newInstance(testUUID: String, uuidType: TestUuidType): BasicResultFragment {

            return newInstance(testUUID, uuidType, false)
        }

        fun newInstance(testUUID: String, uuidType: TestUuidType, reloadHistory: Boolean): BasicResultFragment {

            val args = Bundle()
            args.putString(KEY_TEST_UUID, testUUID)
            args.putInt(KEY_UUID_TYPE, uuidType.ordinal)
            args.putBoolean(KEY_RELOAD_HISTORY, reloadHistory)

            val fragment = BasicResultFragment()
            fragment.arguments = args
            return fragment
        }
    }
}