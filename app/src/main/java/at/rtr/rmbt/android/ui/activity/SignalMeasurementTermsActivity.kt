package at.rtr.rmbt.android.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivitySignalMeasurementTermsBinding
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor

class SignalMeasurementTermsActivity : BaseActivity() {

    private lateinit var binding: ActivitySignalMeasurementTermsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_signal_measurement_terms)
        window?.changeStatusBarColor(ToolbarTheme.WHITE)

        binding.decline.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        binding.accept.setOnClickListener {
            setResult(Activity.RESULT_OK)
            startActivity(Intent(this, SignalMeasurementActivity::class.java))
            finish()
        }
    }

    companion object {

        fun start(context: Context): Intent = Intent(context, SignalMeasurementTermsActivity::class.java)
    }
}