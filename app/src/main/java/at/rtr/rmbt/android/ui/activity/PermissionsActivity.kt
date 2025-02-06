package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityPermissionsBinding
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor

class PermissionsActivity : BaseActivity() {

    private lateinit var binding: ActivityPermissionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_permissions)

        window?.changeStatusBarColor(ToolbarTheme.WHITE)

        binding.accept.setOnClickListener {
            val i: Intent = Intent(
                this@PermissionsActivity,
                HomeActivity::class.java
            )
            // set the new task and clear flags
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(i)
            HomeActivity.start(this)
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(
                Intent(
                    context,
                    PermissionsActivity::class.java
                )
            )
    }
}