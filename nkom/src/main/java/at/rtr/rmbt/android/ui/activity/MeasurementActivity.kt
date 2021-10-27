/*
 *
 *  Licensed under the Apache License, Version 2.0 (the “License”);
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an “AS IS” BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.core.content.res.ResourcesCompat
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityMeasurementBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.dialog.SimpleDialog
import at.rtr.rmbt.android.ui.fragment.BasicResultFragment
import at.rtr.rmbt.android.ui.fragment.BasicResultFragment.DataLoadedListener
import at.rtr.rmbt.android.ui.fragment.SimpleResultsListFragment
import at.rtr.rmbt.android.util.TestUuidType
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.MeasurementViewModel
import at.specure.data.entity.LoopModeRecord
import at.specure.data.entity.LoopModeState
import at.specure.info.TransportType
import at.specure.location.LocationState
import at.specure.measurement.MeasurementState
import timber.log.Timber

private const val CODE_CANCEL = 0
private const val CODE_ERROR = 1

class MeasurementActivity : BaseActivity(), SimpleDialog.Callback {

    private val viewModel: MeasurementViewModel by viewModelLazy()
    private lateinit var binding: ActivityMeasurementBinding
    private var resultFragment: BasicResultFragment? = null
    var loopMedianValuesReloadNeeded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMeasurementBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.state = viewModel.state

        viewModel.connectivityInfoLiveData.listen(this) {
            val imageDrawableId = when (it?.transportType) {
                TransportType.WIFI -> R.drawable.image_home_wifi
                TransportType.CELLULAR -> R.drawable.image_home_cellular
                TransportType.ETHERNET -> R.drawable.image_home_ethernet
                else -> R.drawable.image_home
            }
            binding.image.setImageDrawable(
                ResourcesCompat.getDrawable(resources, imageDrawableId, applicationContext.theme)
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding.buttonCancel.setOnClickListener { onCrossIconClicked() }

        viewModel.measurementFinishLiveData.listen(this) {
            Timber.d("MeasurementViewModel finished activity = listened $it")
            finishActivity(it)
        }

        viewModel.measurementCancelledLiveData.listen(this) {
            Timber.d("MeasurementViewModel cancelled activity = listened $it")
            cancelMeasurement()
        }

        viewModel.measurementErrorLiveData.listen(this) {
            SimpleDialog.Builder()
                .messageText(R.string.test_dialog_error_text)
                .positiveText(R.string.input_setting_dialog_ok)
                .cancelable(false)
                .show(supportFragmentManager, CODE_ERROR)
        }

        viewModel.downloadGraphSource.listen(this) {
            if (viewModel.state.measurementState.get() == MeasurementState.DOWNLOAD) {
                binding.speedLine.addGraphItems(it)
            }
        }

        viewModel.uploadGraphSource.listen(this) {
            if (viewModel.state.measurementState.get() == MeasurementState.UPLOAD) {
                binding.speedLine.addGraphItems(it)
            }
        }

        viewModel.activeNetworkLiveData.listen(this) {
            if (it != null) {
                viewModel.state.networkInfo.set(it)
                viewModel.state.isConnected.set(true)
            } else {
                viewModel.state.networkInfo.set(null)
                viewModel.state.isConnected.set(false)
            }
        }

        viewModel.qosProgressLiveData.listen(this) {
            // todo qos progress
//            binding.measurementBottomView?.qosProgressContainer?.update(it)
        }

        viewModel.loopUuidLiveData.listen(this) { loopUUID ->
            if (loopUUID != null) {
                viewModel.loopProgressLiveData.observe(this@MeasurementActivity) { loopRecord ->
                    onLoopRecordChanged(loopRecord)
                }
            }
        }

        viewModel.timeToNextTestElapsedLiveData.listen(this) {
            binding.blockLoopWaiting.textNextTime.text = it
        }

        viewModel.locationStateLiveData.listen(this) {
            viewModel.state.gpsEnabled.set(it == LocationState.ENABLED)
        }

        viewModel.resultWaitingToBeSentLiveData.listen(this) {
            if (!it) {
                showBasicResultsFragment()
            }
        }

        Timber.d("Measurement state loop create: ${viewModel.state.measurementState.get()?.name}")

        viewModel.state.loopModeRecord.get()?.testsPerformed?.let { viewModel.state.setLoopProgress(it, viewModel.config.loopModeNumberOfTests) }

//        viewModel.qosProgressLiveData.value?.let { binding.measurementBottomView?.qosProgressContainer?.update(it) }
    }

    private fun finishActivity(measurementFinished: Boolean) {
        if (measurementFinished) {
            if (viewModel.state.isLoopModeActive.get()) {
                if (viewModel.state.loopModeRecord.get()?.status == LoopModeState.FINISHED || viewModel.state.loopModeRecord.get()?.status == LoopModeState.CANCELLED) {
                    finish()
                    LoopFinishedActivity.start(this)
                }
            } else {
                finish()
                viewModel.testUUID?.let {
                    if (viewModel.state.measurementState.get() == MeasurementState.FINISH)
                        ResultsActivity.start(this, it, ResultsActivity.ReturnPoint.HOME)
                }
            }
        }
    }

    private fun cancelMeasurement() {
        if (viewModel.state.isLoopModeActive.get() && (viewModel.state.loopModeRecord.get()?.status == LoopModeState.FINISHED || viewModel.state.loopModeRecord.get()?.status == LoopModeState.CANCELLED)) {
            LoopFinishedActivity.start(this)
        } else {
            finishAffinity()
            HomeActivity.startWithFragment(
                this,
                HomeActivity.Companion.HomeNavigationTarget.HOME_FRAGMENT_TO_SHOW
            )
        }
    }

    private fun onLoopRecordChanged(loopRecord: LoopModeRecord?) {
        Timber.d(
            "TestPerformed: ${loopRecord?.testsPerformed} \nloop mode status: ${loopRecord?.status} \nLoop local uuid: ${loopRecord?.localUuid}\nLoop remote uuid: ${loopRecord?.uuid}\nviewModel: ${viewModel.state.measurementState.get()}"
        )
        Timber.d("local loop UUID to read loop data: ${viewModel.loopUuidLiveData.value}")
        viewModel.state.setLoopRecord(loopRecord)
        loopRecord?.testsPerformed?.let { testsPerformed ->
            viewModel.state.setLoopProgress(
                testsPerformed,
                viewModel.config.loopModeNumberOfTests
            )
        }
        binding.blockLoopWaiting.textNextDistance.text = viewModel.state.loopNextTestDistanceMeters.get()
        loopRecord?.status?.let { status ->
            when (status) {
                LoopModeState.IDLE -> {
                    showBasicResultsFragment()
                }
                LoopModeState.RUNNING -> {
                    resultFragment?.let { supportFragmentManager.beginTransaction().remove(resultFragment!!).commitNow() }
                    resultFragment = null
                }
                else -> {
                } // do nothing
            }
        }

        if (loopRecord?.testsPerformed != null && loopRecord.testsPerformed >= 2 && loopMedianValuesReloadNeeded) {
            Timber.d("Loading median values on Loop record changed")
            loopMedianValuesReloadNeeded = false
            viewModel.initializeLoopData(loopRecord.localUuid)
        }

        if (loopRecord?.status == LoopModeState.FINISHED || loopRecord?.status == LoopModeState.CANCELLED) {
            finishActivity(true)
        }
    }

    private fun showBasicResultsFragment() {
        Timber.d("history test result Show fragment: ${viewModel.state.loopModeRecord.get()?.status == LoopModeState.IDLE}, ${resultFragment == null},  ${viewModel.resultWaitingToBeSentLiveData.value == false}")
        if (viewModel.state.loopModeRecord.get()?.status == LoopModeState.IDLE && resultFragment == null && viewModel.resultWaitingToBeSentLiveData.value == false) {
            viewModel.state.loopModeRecord.get()?.uuid?.let {
                resultFragment = BasicResultFragment.newInstance(it, TestUuidType.LOOP_UUID, true)
                (resultFragment as BasicResultFragment).onDataLoadedListener = object : DataLoadedListener {

                    override fun onDataLoaded() {
                        val resultListFragment = SimpleResultsListFragment.newInstance(it, false)
                        supportFragmentManager.beginTransaction()
                            .replace(binding.resultListContainer.id, resultListFragment as SimpleResultsListFragment).commitNow()
                    }
                }
                supportFragmentManager.beginTransaction().replace(binding.resultContainer.id, resultFragment as BasicResultFragment).commitNow()
            }
        }
    }

    override fun onDialogPositiveClicked(code: Int) {
        if (code == CODE_CANCEL) {
            viewModel.cancelMeasurement()
        }
        // finish activity for in both cases
        finish()
    }

    override fun onDialogNegativeClicked(code: Int) {
        // Do nothing
    }

    override fun onStart() {
        super.onStart()
        Timber.d("MeasurementViewModel START")
        viewModel.attach(this)
    }

    override fun onStop() {
        super.onStop()
        viewModel.detach(this)
    }

    override fun onBackPressed() {
        onCrossIconClicked()
    }

    override fun onResume() {
        super.onResume()
        loopMedianValuesReloadNeeded = true
        Timber.d("MeasurementViewModel RESUME")
    }

    private fun onCrossIconClicked() {
        SimpleDialog.Builder()
            .messageText(R.string.title_cancel_measurement)
            .positiveText(R.string.text_cancel_measurement)
            .negativeText(R.string.text_continue_measurement)
            .cancelable(false)
            .show(supportFragmentManager, CODE_CANCEL)
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, MeasurementActivity::class.java)
            context.startActivity(intent)
        }
    }
}
