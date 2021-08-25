package at.rtr.rmbt.android.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityPermissionsExplanationBinding
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor

class PermissionsExplanationActivity : BaseActivity() {

    private lateinit var binding: ActivityPermissionsExplanationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_permissions_explanation)

        window?.changeStatusBarColor(ToolbarTheme.WHITE)

        binding.close.setOnClickListener { finish() }
    }

    companion object {
        fun start(fragment: Fragment) =
            fragment.startActivity(
                Intent(
                    fragment.requireContext(),
                    PermissionsExplanationActivity::class.java
                )
            )
    }
}