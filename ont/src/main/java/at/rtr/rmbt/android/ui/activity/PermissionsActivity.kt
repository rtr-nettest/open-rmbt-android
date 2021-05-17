package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityPermissionsBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.fragment.PermissionsAccuracyFragment
import at.rtr.rmbt.android.ui.fragment.PermissionsAnalyticsFragment
import at.rtr.rmbt.android.ui.fragment.PermissionsClientIdFragment
import at.rtr.rmbt.android.ui.fragment.PermissionsSignalFragment
import at.rtr.rmbt.android.ui.fragment.PermissionsWelcomeFragment
import at.rtr.rmbt.android.viewmodel.PermissionsPage
import at.rtr.rmbt.android.viewmodel.PermissionsViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow

class PermissionsActivity : BaseActivity() {

    private lateinit var binding: ActivityPermissionsBinding
    private val viewModel: PermissionsViewModel by viewModelLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_permissions)

        supportFragmentManager.beginTransaction().replace(binding.container.id, get(PermissionsPage.WELCOME), PermissionsPage.WELCOME.toString())
            .commitAllowingStateLoss()

        viewModel.permissionsPageChannel.receiveAsFlow().map {
            supportFragmentManager.beginTransaction().replace(binding.container.id, get(it), it.toString()).commitAllowingStateLoss()
        }.launchIn(lifecycleScope)

        viewModel.flowCompletedChannel.receiveAsFlow().map {
            viewModel.permissionsWereAsked()
            finishAffinity()
            HomeActivity.start(this)
        }.launchIn(lifecycleScope)
    }

    override fun onBackPressed() {
        finish()
    }

    private fun get(page: PermissionsPage) = when (page) {
        PermissionsPage.ACCURACY -> PermissionsAccuracyFragment.newInstance()
        PermissionsPage.SIGNAL -> PermissionsSignalFragment.newInstance()
        PermissionsPage.ANALYTICS -> PermissionsAnalyticsFragment.newInstance()
        PermissionsPage.CLIENT_ID -> PermissionsClientIdFragment.newInstance()
        else -> PermissionsWelcomeFragment.newInstance()
    }

    companion object {
        const val REQUEST_CODE_ACCURACY = 1103
        const val REQUEST_CODE_SIGNAL = 1104

        fun start(context: Context) = context.startActivity(Intent(context, PermissionsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}