package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.adapter.HistoryAdapter
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.CoverageResultViewModel
import kotlin.math.max

/**
 * Lists the individual segments (each <=400 fences) of a coverage loop measurement. Tapping a
 * segment opens its regular single-segment result (map + "Test details"), exactly as before.
 */
class CoverageLoopSegmentsActivity : BaseActivity() {

    private val viewModel: CoverageResultViewModel by viewModelLazy()
    private val adapter: HistoryAdapter by lazy { HistoryAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coverage_loop_segments)

        val root = findViewById<View>(R.id.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, windowInsets ->
                val insetsSystemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                val insetsDisplayCutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
                v.updatePadding(
                    top = max(insetsSystemBars.top, insetsDisplayCutout.top),
                    left = max(insetsSystemBars.left, insetsDisplayCutout.left),
                    right = max(insetsSystemBars.right, insetsDisplayCutout.right),
                    bottom = max(insetsSystemBars.bottom, insetsDisplayCutout.bottom),
                )
                windowInsets
            }
        }

        val loopUUID = intent.getStringExtra(KEY_LOOP_UUID)
        check(!loopUUID.isNullOrEmpty()) { "loopUUID was not passed to segments activity" }

        findViewById<ImageView>(R.id.buttonBack).setOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        adapter.actionCallback = { CoverageResultsActivity.start(this, it.testUUID, returnToCaller = true) }

        viewModel.loopSegmentsLiveData.listen(this) { segments ->
            // Latest measurement on top.
            adapter.items = segments.sortedByDescending { it.time }
        }
        viewModel.loadLoopSegments(loopUUID)
    }

    override fun onHandledException(exception: HandledException?) { }

    companion object {

        private const val KEY_LOOP_UUID = "KEY_LOOP_UUID"

        fun start(context: Context, loopUUID: String) {
            val intent = Intent(context, CoverageLoopSegmentsActivity::class.java)
            intent.putExtra(KEY_LOOP_UUID, loopUUID)
            context.startActivity(intent)
        }
    }
}
