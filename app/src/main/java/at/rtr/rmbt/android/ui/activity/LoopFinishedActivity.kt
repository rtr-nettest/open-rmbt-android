package at.rtr.rmbt.android.ui.activity

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityLoopFinishedBinding
import at.rtr.rmbt.android.ui.dialog.SimpleDialog
import at.specure.measurement.MeasurementService
import kotlin.math.max

class LoopFinishedActivity : BaseActivity(), SimpleDialog.Callback {

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
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                this@LoopFinishedActivity.finishAffinity()
                HomeActivity.startWithFragment(this@LoopFinishedActivity, HomeActivity.Companion.HomeNavigationTarget.HOME_FRAGMENT_TO_SHOW)
            }
        })

        if (savedInstanceState == null) {
            showLoopFinishedDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        notificationManager.cancel(MeasurementService.NOTIFICATION_LOOP_FINISHED_ID)
        notificationManager.cancel(MeasurementService.NOTIFICATION_ID)
    }

    private fun showLoopFinishedDialog() {
        SimpleDialog.Builder()
            .messageText(R.string.loop_mode_completed_message)
            .positiveText(R.string.loop_mode_completed_ok)
            .cancelable(true)
            .show(supportFragmentManager, CODE_LOOP_FINISHED_DIALOG, TAG_LOOP_FINISHED_DIALOG)
    }

    override fun onDialogPositiveClicked(code: Int) {
        if (code == CODE_LOOP_FINISHED_DIALOG) {
            notificationManager.cancel(MeasurementService.NOTIFICATION_LOOP_FINISHED_ID)
            notificationManager.cancel(MeasurementService.NOTIFICATION_ID)
            this.finishAffinity()
            HomeActivity.startWithFragment(this, HomeActivity.Companion.HomeNavigationTarget.HISTORY_FRAGMENT_TO_SHOW)
        }
    }

    override fun onDialogNegativeClicked(code: Int) {
        // no negative button on the loop finished dialog
    }

    companion object {

        private const val CODE_LOOP_FINISHED_DIALOG = 1
        private const val TAG_LOOP_FINISHED_DIALOG = "LOOP_FINISHED_DIALOG"

        fun start(context: Context) = context.startActivity(Intent(context, LoopFinishedActivity::class.java))
    }
}