package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityMeasurementBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.dialog.CancelMeasurementCallback
import at.rtr.rmbt.android.ui.dialog.CancelMeasurementDialog
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.MeasurementViewModel

class MeasurementActivity : BaseActivity(), CancelMeasurementCallback {

    private val viewModel: MeasurementViewModel by viewModelLazy()
    private lateinit var binding: ActivityMeasurementBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_measurement)
        binding.state = viewModel.state

        binding.buttonCancel.setOnClickListener { onCrossIconClicked() }

        viewModel.measurementFinishLiveData.listen(this) {
            finish()
            ResultsActivity.start(this)
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

    override fun onBackPressed() {
        onCrossIconClicked()
    }

    override fun onCancel() {
        viewModel.cancelMeasurement()
        finish()
    }

    private fun onCrossIconClicked() {
        CancelMeasurementDialog.instance().show(this)
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, MeasurementActivity::class.java)
            context.startActivity(intent)
        }
    }
}