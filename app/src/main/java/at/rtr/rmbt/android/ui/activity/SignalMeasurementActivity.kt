package at.rtr.rmbt.android.ui.activity

import android.os.Bundle
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivitySignalMeasurementBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.HomeViewModel

class SignalMeasurementActivity : BaseActivity() {

    private val viewModel: HomeViewModel by viewModelLazy()
    private lateinit var binding: ActivitySignalMeasurementBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_signal_measurement)
        binding.isActive = false
        binding.isPaused = false

        viewModel.activeSignalMeasurementLiveData.listen(this) {
            binding.isActive = it
        }

        viewModel.pausedSignalMeasurementLiveData.listen(this) {
            binding.isPaused = it
        }

        binding.buttonStart.setOnClickListener {
            viewModel.startSignalMeasurement()
        }

        binding.buttonStop.setOnClickListener {
            viewModel.stopSignalMeasurement()
        }

        binding.buttonPause.setOnClickListener {
            viewModel.pauseSignalMeasurement()
        }

        binding.buttonResume.setOnClickListener {
            viewModel.resumeSignalMeasurement()
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.attach(this)
    }

    override fun onStop() {
        super.onStop()
        viewModel.detach(this)
    }
}