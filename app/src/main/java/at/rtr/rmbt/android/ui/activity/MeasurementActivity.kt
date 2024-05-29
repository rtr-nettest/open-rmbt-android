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
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityMeasurementBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.dialog.SimpleDialog
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.MeasurementViewModel
import at.specure.data.entity.LoopModeRecord
import at.specure.data.entity.LoopModeState
import at.specure.location.LocationState
import at.specure.measurement.MeasurementState
import timber.log.Timber

private const val CODE_CANCEL = 0
private const val CODE_ERROR = 1

class MeasurementActivity : BaseActivity(), SimpleDialog.Callback {

    private val viewModel: MeasurementViewModel by viewModelLazy()
    private lateinit var binding: ActivityMeasurementBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMeasurementBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

//        binding = bindContentView(R.layout.activity_measurement)
        binding.state = viewModel.state

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
                binding.measurementBottomView?.speedChartDownloadUpload?.addGraphItems(it)
            }
        }

        viewModel.uploadGraphSource.listen(this) {
            if (viewModel.state.measurementState.get() == MeasurementState.UPLOAD) {
                binding.measurementBottomView?.speedChartDownloadUpload?.addGraphItems(it)
            }
        }

        viewModel.signalStrengthLiveData.listen(this) {
            if (it != null) {
                viewModel.state.detailedNetworkInfo.set(it)
            } else {
                viewModel.state.detailedNetworkInfo.set(null)
            }

            if (it?.networkInfo == null) {
                viewModel.state.setSignalStrength(null)
                viewModel.state.networkInfo.set(null)
                viewModel.state.isConnected.set(false)
            } else {
                viewModel.state.isConnected.set(true)
                viewModel.state.networkInfo.set(it.networkInfo)
                viewModel.state.setSignalStrength(it.signalStrengthInfo)
            }
        }

        viewModel.qosProgressLiveData.listen(this) {
            binding.measurementBottomView?.qosProgressContainer?.update(it)
        }

        viewModel.loopUuidLiveData.listen(this) { loopUUID ->
            if (loopUUID != null) {
                viewModel.loopProgressLiveData.observe(this@MeasurementActivity) { loopRecord ->
                    onLoopRecordChanged(loopRecord)
                }
            }
        }

        viewModel.timeToNextTestElapsedLiveData.listen(this) {
            binding.measurementBottomView?.loopMeasurementNextTestMinutesValue?.text = it
        }

        viewModel.timeProgressPercentsLiveData.listen(this) {
            binding.measurementBottomView?.loopMeasurementNextTestMinutesProgress?.progress = it
        }

        viewModel.locationStateLiveData.listen(this) {
            viewModel.state.gpsEnabled.set(it == LocationState.ENABLED)
        }

        Timber.d("Measurement state loop create: ${viewModel.state.measurementState.get()?.name}")

        viewModel.state.loopModeRecord.get()?.testsPerformed?.let { viewModel.state.setLoopProgress(it, viewModel.config.loopModeNumberOfTests) }

        viewModel.qosProgressLiveData.value?.let { binding.measurementBottomView?.qosProgressContainer?.update(it) }
    }

    private fun finishActivity(measurementFinished: Boolean) {
        Timber.d("Finish activity with measurement finished: $measurementFinished, testUUID: ${viewModel.testUUID}, measurementState: ${viewModel.state.measurementState.get()}, LoopModeActive: ${viewModel.state.isLoopModeActive.get()}, LoopModeState: ${viewModel.state.loopModeRecord.get()?.status}")
        if (measurementFinished) {
            if (viewModel.state.isLoopModeActive.get()) {
                if (viewModel.state.loopModeRecord.get()?.status == LoopModeState.FINISHED) {
                    this.finishAffinity()
                    LoopFinishedActivity.start(this)
                }
            } else {
                viewModel.testUUID?.let {
                    if (viewModel.state.measurementState.get() == MeasurementState.FINISH) {
                        ResultsActivity.start(this, it, ResultsActivity.ReturnPoint.HOME)
                        finish()
                        return
                    }
                }
                finish()
                HomeActivity.start(this)
            }
        }
    }

    private fun cancelMeasurement() {
        if (viewModel.state.isLoopModeActive.get()) {
            finishAffinity()
            LoopFinishedActivity.start(this)
        } else {
            finishAffinity()
            HomeActivity.startWithFragment(this, HomeActivity.Companion.HomeNavigationTarget.HOME_FRAGMENT_TO_SHOW)
        }
    }

    private fun onLoopRecordChanged(loopRecord: LoopModeRecord?) {
        Timber.d(
            "TestPerformed: ${loopRecord?.testsPerformed} \nloop mode status: ${loopRecord?.status} \nLoop local uuid: ${loopRecord?.localUuid}\nLoop remote uuid: ${loopRecord?.uuid}\nmeasurement state: ${viewModel.state.measurementState.get()}"
        )
        Timber.d("local loop UUID to read loop data: ${viewModel.loopUuidLiveData.value}")
        binding.curveLayout?.setLoopState(loopRecord?.status ?: LoopModeState.RUNNING)
        Timber.d("curve updated")
        viewModel.state.setLoopRecord(loopRecord)
        Timber.d("setting loop record in view model state")
        loopRecord?.testsPerformed?.let { testsPerformed ->
            viewModel.state.setLoopProgress(
                testsPerformed,
                viewModel.config.loopModeNumberOfTests
            )
            Timber.d("setting loop record test performed: $testsPerformed")
        }
        binding.measurementBottomView?.loopMeasurementNextTestMetersProgress?.progress =
            viewModel.state.loopNextTestPercent.get()
        Timber.d("setting next test progress")
        loopRecord?.status?.let { status ->
            if ((status == LoopModeState.IDLE) || (status == LoopModeState.FINISHED)) {
                binding.measurementBottomView?.speedChartDownloadUpload?.reset()
                binding.measurementBottomView?.qosProgressContainer?.reset()
            }
        }
        Timber.d("qos and speed chart containers reset check / done")
        binding.buttonSignal?.setOnClickListener {
            startActivity(Intent(this, SignalMeasurementActivity::class.java))
            Timber.d("setting signal measurement activity to run")
        }

        if (loopRecord?.status == LoopModeState.FINISHED || loopRecord?.status == LoopModeState.CANCELLED) {
            Timber.d("finishing activity because loop mode has ended")
            finishActivity(true)
        }
        Timber.d("updating loop mode after loop mode record change ended")
    }

    override fun onDialogPositiveClicked(code: Int) {
        if (code == CODE_CANCEL) {
            viewModel.cancelMeasurement()
        }
        // finish activity for in both cases
        finish()
        HomeActivity.startWithFragment(this, HomeActivity.Companion.HomeNavigationTarget.HOME_FRAGMENT_TO_SHOW)
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
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }
}
