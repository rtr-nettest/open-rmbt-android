package at.rtr.rmbt.android.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivitySyncBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.dialog.SimpleDialog
import at.rtr.rmbt.android.ui.fragment.sync.SyncEnterFragment
import at.rtr.rmbt.android.ui.fragment.sync.SyncRequestFragment
import at.rtr.rmbt.android.ui.fragment.sync.SyncStarterFragment
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.SyncDevicesViewModel
import at.rtr.rmbt.android.viewmodel.SyncPage
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow

class SyncActivity : BaseActivity() {

    private lateinit var binding: ActivitySyncBinding
    private val viewModel: SyncDevicesViewModel by viewModelLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sync)

        window?.changeStatusBarColor(ToolbarTheme.WHITE)

        supportFragmentManager.beginTransaction().replace(binding.container.id, get(SyncPage.STARTER), SyncPage.STARTER.toString())
            .commitAllowingStateLoss()

        binding.buttonCancel.setOnClickListener { finish() }

        viewModel.errorLiveData.listen(this) {
            SimpleDialog.Builder()
                .messageText(it.getText(this))
                .positiveText(R.string.button_close)
                .show(supportFragmentManager, 0)
        }

        viewModel.pageChannel
            .receiveAsFlow()
            .map { supportFragmentManager.beginTransaction().replace(binding.container.id, get(it), it.toString()).commitAllowingStateLoss() }
            .launchIn(lifecycleScope)

        viewModel.completedChannel.receiveAsFlow().map {
            setResult(RESULT_OK)
            finish()
        }.launchIn(lifecycleScope)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun get(page: SyncPage) = when (page) {
        SyncPage.STARTER -> SyncStarterFragment.newInstance()
        SyncPage.REQUEST -> SyncRequestFragment.newInstance()
        SyncPage.ENTER -> SyncEnterFragment.newInstance()
    }

    companion object {
        private const val CODE_SYNC = 1954

        fun start(fragment: Fragment) = fragment.startActivityForResult(Intent(fragment.requireContext(), SyncActivity::class.java), CODE_SYNC)
    }
}