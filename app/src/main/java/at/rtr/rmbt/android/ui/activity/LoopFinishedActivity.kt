package at.rtr.rmbt.android.ui.activity

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityLoopFinishedBinding
import at.specure.measurement.MeasurementService
import kotlin.math.max

class LoopFinishedActivity : BaseActivity() {

    private lateinit var binding: ActivityLoopFinishedBinding

    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_loop_finished)
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
        binding.buttonGoToResults.setOnClickListener {
            this.finishAffinity()
            HomeActivity.startWithFragment(this, HomeActivity.Companion.HomeNavigationTarget.HISTORY_FRAGMENT_TO_SHOW)
        }

        binding.buttonRunAgain.setOnClickListener {
            this.finishAffinity()
            HomeActivity.startWithFragment(this, HomeActivity.Companion.HomeNavigationTarget.HOME_FRAGMENT_TO_SHOW)
            LoopConfigurationActivity.start(this)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        this.finishAffinity()
        HomeActivity.startWithFragment(this, HomeActivity.Companion.HomeNavigationTarget.HOME_FRAGMENT_TO_SHOW)
    }

    override fun onResume() {
        super.onResume()
        notificationManager.cancel(MeasurementService.NOTIFICATION_LOOP_FINISHED_ID)
        notificationManager.cancel(MeasurementService.NOTIFICATION_ID)
    }

    companion object {

        fun start(context: Context) = context.startActivity(Intent(context, LoopFinishedActivity::class.java))
    }
}