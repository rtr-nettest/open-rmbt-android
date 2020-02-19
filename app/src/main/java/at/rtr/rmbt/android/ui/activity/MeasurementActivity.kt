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
import androidx.lifecycle.observe
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityMeasurementBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.dialog.SimpleDialog
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.MeasurementViewModel
import at.specure.data.entity.LoopModeState
import at.specure.location.LocationProviderState
import at.specure.measurement.MeasurementState
import kotlinx.android.synthetic.main.activity_measurement.view.curve_layout
import kotlinx.android.synthetic.main.activity_measurement.view.measurementBottomView
import kotlinx.android.synthetic.main.measurement_bottom_view.view.loop_measurement_next_test_meters_progress
import kotlinx.android.synthetic.main.measurement_bottom_view.view.loop_measurement_next_test_minutes_progress
import kotlinx.android.synthetic.main.measurement_bottom_view.view.loop_measurement_next_test_minutes_value
import kotlinx.android.synthetic.main.measurement_bottom_view.view.qosProgressContainer
import kotlinx.android.synthetic.main.measurement_bottom_view.view.speedChartDownloadUpload
import timber.log.Timber

private const val CODE_CANCEL = 0
private const val CODE_ERROR = 1

class MeasurementActivity : BaseActivity(), SimpleDialog.Callback {

    private val viewModel: MeasurementViewModel by viewModelLazy()
    private lateinit var binding: ActivityMeasurementBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_measurement)
        binding.state = viewModel.state

        binding.buttonCancel.setOnClickListener { onCrossIconClicked() }

        viewModel.measurementFinishLiveData.listen(this) {
            finish()
            if (viewModel.state.isLoopModeActive.get()) {
                LoopFinishedActivity.start(this)
            } else {
                if (it) {
                    ResultsActivity.start(this, viewModel.testUUID)
                }
            }
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
                binding.root.measurementBottomView.speedChartDownloadUpload.addGraphItems(it)
            }
        }

        viewModel.uploadGraphSource.listen(this) {
            if (viewModel.state.measurementState.get() == MeasurementState.UPLOAD) {
                binding.root.measurementBottomView.speedChartDownloadUpload.addGraphItems(it)
            }
        }

        viewModel.signalStrengthLiveData.listen(this) {
            viewModel.state.signalStrengthInfo.set(it)
        }

        viewModel.activeNetworkLiveData.listen(this) {
            viewModel.state.networkInfo.set(it)
        }

        viewModel.qosProgressLiveData.listen(this) {
            binding.root.measurementBottomView.qosProgressContainer.update(it)
        }

        viewModel.loopUuidLiveData.listen(this) { loopUUID ->
            if (loopUUID != null) {
                viewModel.loopProgressLiveData.observe(this@MeasurementActivity) { loopRecord ->
                    Timber.d(
                        "TestPerformed: ${loopRecord?.testsPerformed} \nloop mode status: ${loopRecord?.status} \nviewModel: ${viewModel.state.measurementState.get()}"
                    )
                    viewModel.state.setLoopRecord(loopRecord)
                    loopRecord?.testsPerformed?.let { testsPerformed ->
                        viewModel.state.setLoopProgress(
                            testsPerformed,
                            viewModel.config.loopModeNumberOfTests
                        )
                    }
                    binding.root.measurementBottomView.loop_measurement_next_test_meters_progress.progress = viewModel.state.loopNextTestPercent.get()
                    loopRecord?.status?.let { status ->
                        viewModel.state.setLoopState(status)
                        if (status == LoopModeState.IDLE) {
                            binding.root.measurementBottomView.speedChartDownloadUpload.reset()
                            binding.root.measurementBottomView.speedChartDownloadUpload.reset()
                            binding.root.qosProgressContainer.reset()
                        }
                    }
                }
            }
        }

        viewModel.timeToNextTestElapsedLiveData.listen(this) {
            binding.root.measurementBottomView.loop_measurement_next_test_minutes_value.text = it
        }

        viewModel.timeProgressPercentsLiveData.listen(this) {
            binding.root.measurementBottomView.loop_measurement_next_test_minutes_progress.progress = it
        }

        viewModel.locationProviderStateLiveData.listen(this) {
            viewModel.state.gpsEnabled.set(it == LocationProviderState.ENABLED)
        }
        Timber.d("Measurement state loop create: ${viewModel.state.measurementState.get()?.name}")
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
        viewModel.attach(this)
        Timber.d("Measurement state loop start: ${viewModel.state.measurementState.get()?.name}")
    }

    override fun onStop() {
        super.onStop()
        viewModel.detach(this)
    }

    override fun onBackPressed() {
        onCrossIconClicked()
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
