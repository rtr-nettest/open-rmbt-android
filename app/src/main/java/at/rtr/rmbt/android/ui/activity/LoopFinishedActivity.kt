package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityLoopFinishedBinding

class LoopFinishedActivity : BaseActivity() {

    private lateinit var binding: ActivityLoopFinishedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_loop_finished)

        binding.buttonGoToResults.setOnClickListener {
            HomeActivity.startWithFragment(this, HomeActivity.Companion.HomeNavigationTarget.HISTORY_FRAGMENT_TO_SHOW)
            finishAffinity()
        }

        binding.buttonRunAgain.setOnClickListener {
            LoopConfigurationActivity.start(this)
            finish()
        }
    }

    companion object {

        fun start(context: Context) = context.startActivity(Intent(context, LoopFinishedActivity::class.java))
    }
}