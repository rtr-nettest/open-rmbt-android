package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityDetailedFullscreenMapBinding

class DetailedFullscreenMapActivity : BaseActivity() {

    private lateinit var binding: ActivityDetailedFullscreenMapBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_detailed_fullscreen_map)
    }

    companion object {

        private const val KEY_TEST_UUID = "KEY_TEST_UUID"

        fun start(context: Context, testUUID: String) {
            val intent = Intent(context, DetailedFullscreenMapActivity::class.java)
            intent.putExtra(KEY_TEST_UUID, testUUID)
            context.startActivity(intent)
        }
    }
}
