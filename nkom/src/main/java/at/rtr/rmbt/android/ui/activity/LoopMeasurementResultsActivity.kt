package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityLoopMeasurementResultsBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.adapter.LoopMeasurementAdapter
import at.rtr.rmbt.android.ui.fragment.BasicResultFragment
import at.specure.test.TestUuidType
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.viewmodel.ResultsListLoopViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LoopMeasurementResultsActivity : BaseActivity() {

    private lateinit var binding: ActivityLoopMeasurementResultsBinding
    private val viewModel: ResultsListLoopViewModel by viewModelLazy()
    private var resultFragment: BasicResultFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_loop_measurement_results)

        setTransparentStatusBar()
        window.changeStatusBarColor(ToolbarTheme.WHITE)

        val adapter = LoopMeasurementAdapter(true)
        binding.recyclerViewHistoryItems.adapter = adapter

        adapter.actionCallback = { ResultsActivity.start(this, it.testUUID) }

        binding.buttonCancel.setOnClickListener { finish() }

        val loopId = intent?.getStringExtra(KEY_LOOP_ID)
        checkNotNull(loopId)
        viewModel.loadLoopMeasurements(loopId).onEach {
            adapter.submitList(it)
        }.launchIn(lifecycleScope)
        showBasicResultsFragment(loopId)
    }

    companion object {
        private const val KEY_LOOP_ID = "key_loop_id"

        fun start(context: Context, loopId: String) =
            context.startActivity(Intent(context, LoopMeasurementResultsActivity::class.java).apply { putExtra(KEY_LOOP_ID, loopId) })
    }

    private fun showBasicResultsFragment(loopId: String) {
        resultFragment = BasicResultFragment.newInstance(loopId, TestUuidType.LOOP_UUID, false)
        supportFragmentManager.beginTransaction().replace(binding.resultContainer.id, resultFragment as BasicResultFragment).commitNow()
    }
}