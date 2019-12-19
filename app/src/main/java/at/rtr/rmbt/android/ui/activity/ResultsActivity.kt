package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityResultsBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.adapter.ResultQoEAdapter
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.ResultViewModel

class ResultsActivity : BaseActivity() {

    private val viewModel: ResultViewModel by viewModelLazy()
    private lateinit var binding: ActivityResultsBinding
    private val adapter: ResultQoEAdapter by lazy { ResultQoEAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_results)
        binding.state = viewModel.state
        val testUUID = intent.getStringExtra(KEY_TEST_UUID)
        check(!testUUID.isNullOrEmpty()) { "TestUUID was not passed to result activity" }
        viewModel.state.testUUID = testUUID
        viewModel.testServerResultLiveData.listen(this) {
            viewModel.state.testResult.set(it)
        }
        viewModel.qoeResultLiveData.listen(this) {
            viewModel.state.qoeRecords.set(it)
            adapter.submitList(it)
        }
        binding.qoeResultsRecyclerView.adapter = adapter
        binding.qoeResultsRecyclerView.apply {
            val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            ContextCompat.getDrawable(context, R.drawable.history_item_divider)?.let {
                itemDecoration.setDrawable(it)
            }
            binding.qoeResultsRecyclerView.addItemDecoration(itemDecoration)
        }
        binding.buttonBack.setOnClickListener {
            super.onBackPressed()
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.loadTestResults()
    }

    companion object {
        private const val KEY_TEST_UUID = "KEY_TEST_UUID"

        fun start(context: Context, testUUID: String) {
            val intent = Intent(context, ResultsActivity::class.java)
            intent.putExtra(KEY_TEST_UUID, testUUID)
            context.startActivity(intent)
        }
    }
}