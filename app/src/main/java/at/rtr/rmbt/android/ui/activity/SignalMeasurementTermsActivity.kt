package at.rtr.rmbt.android.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivitySignalMeasurementTermsBinding
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import kotlin.math.max

class SignalMeasurementTermsActivity : BaseActivity() {

    private lateinit var binding: ActivitySignalMeasurementTermsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_signal_measurement_terms)
        window?.changeStatusBarColor(ToolbarTheme.WHITE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
                val insetsSystemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                val insetsDisplayCutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
                val topSafe = max(insetsSystemBars.top, insetsDisplayCutout.top)
                val leftSafe = max(insetsSystemBars.left, insetsDisplayCutout.left)
                val rightSafe = max(insetsSystemBars.right, insetsDisplayCutout.right)
                val bottomSafe = max(insetsSystemBars.bottom, insetsDisplayCutout.bottom)

                v.updatePadding(
                    right = rightSafe,
                    left = leftSafe,
                    top = topSafe,
                    bottom = bottomSafe
                )
                WindowInsetsCompat.CONSUMED
            }
        }

        binding.decline.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        binding.accept.setOnClickListener {
            setResult(Activity.RESULT_OK)
            SignalMeasurementActivity.start(this)
            finish()
        }
    }

    companion object {

        fun start(context: Context): Intent = Intent(context, SignalMeasurementTermsActivity::class.java)
    }
}