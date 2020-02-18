package at.rtr.rmbt.android.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.PagerAdapter
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityLoopInstructionsBinding
import at.rtr.rmbt.android.databinding.ViewLoopModeInstructionBinding
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor

private const val PAGE_COUNT = 2

class LoopInstructionsActivity : BaseActivity(), Callback {

    private lateinit var binding: ActivityLoopInstructionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_loop_instructions)
        window?.changeStatusBarColor(ToolbarTheme.WHITE)

        binding.title.text = getString(R.string.title_loop_instruction_1)

        binding.pager.adapter = InstructionsAdapter(this, this)
        binding.pager.currentItem = 0
    }

    override fun onDeclined() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onFirstPageAccepted() {
        binding.title.text = getString(R.string.title_loop_instruction_2)
        binding.pager.setCurrentItem(1, true)
    }

    override fun onSecondPageAccepted() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    inner class InstructionsAdapter(context: Context, private val callback: Callback) : PagerAdapter() {

        private var items = listOf(context.getString(R.string.text_loop_instruction_1), context.getString(R.string.text_loop_instruction_2))

        override fun isViewFromObject(view: View, o: Any) = view == o
        override fun getCount() = PAGE_COUNT

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val binding = ViewLoopModeInstructionBinding.inflate(LayoutInflater.from(container.context))

            binding.content.text = items[position]

            binding.decline.setOnClickListener { callback.onDeclined() }
            binding.accept.setOnClickListener { if (position == PAGE_COUNT - 1) callback.onSecondPageAccepted() else callback.onFirstPageAccepted() }
            container.addView(binding.root)
            return binding.root
        }

        override fun destroyItem(container: ViewGroup, position: Int, o: Any) {
            container.removeView(o as View)
        }
    }

    companion object {
        fun start(fragment: Fragment, requestCode: Int) =
            fragment.startActivityForResult(Intent(fragment.requireContext(), LoopInstructionsActivity::class.java), requestCode)
    }
}

interface Callback {
    fun onDeclined()
    fun onFirstPageAccepted()
    fun onSecondPageAccepted()
}