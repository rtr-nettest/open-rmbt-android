package at.rtr.rmbt.android.ui.activity

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityLoopFinishedBinding
import at.specure.measurement.MeasurementService

class LoopFinishedActivity : BaseActivity() {

    private lateinit var binding: ActivityLoopFinishedBinding

    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_loop_finished)

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