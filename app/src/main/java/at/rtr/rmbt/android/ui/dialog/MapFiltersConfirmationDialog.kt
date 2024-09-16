package at.rtr.rmbt.android.ui.dialog

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogFiltersConfirmationBinding
import at.rtr.rmbt.android.ui.adapter.FilterConfirmationAdapter
import at.rtr.rmbt.android.util.args
import kotlin.math.min

class MapFiltersConfirmationDialog : FullscreenDialog() {

    override val gravity: Int = Gravity.BOTTOM

    override val cancelable: Boolean = true

    override val dimBackground: Boolean = false

    private lateinit var binding: DialogFiltersConfirmationBinding

    private val callback: Callback?
        get() = when {
            targetFragment is Callback -> targetFragment as Callback
            activity is Callback -> activity as Callback
            else -> null
        }

    private val adapter = FilterConfirmationAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_filters_confirmation, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getStringArrayList(ARG_DATA)?.let { adapter.items = it }
        arguments?.getInt(ARG_POSITION)?.let { adapter.selected = it }
        val title = arguments?.getString(ARG_TITLE)
        binding.buttonText =
            getString(if (targetRequestCode == MapFiltersDialog.CODE_TYPE) R.string.text_filter_continue else R.string.text_filter_confirm)

        binding.label.text = title
        binding.items.adapter = adapter
        binding.accept.setOnClickListener {
            adapter.items[adapter.selected]?.let { it1 -> callback?.onOptionSelected(targetRequestCode, it1) }
            dismiss()
        }
        setMaxHeightOfView(binding.items, 0.5f)
    }

    private fun setMaxHeightOfView(view: View, partOfScreenHeight: Float) {
        val window = dialog?.window
        val windowManager = window?.windowManager
        val metrics = windowManager?.currentWindowMetrics;
        val height = metrics?.bounds?.height()

        height?.let { screenHeight ->
            val maxHeight = (screenHeight * partOfScreenHeight).toInt()
            view.post {
                if (view.height > maxHeight) {
                    view.layoutParams.height = maxHeight
                    view.requestLayout()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ARG_POSITION, adapter.selected)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getInt(ARG_POSITION)?.let { adapter.selected = it }
    }

    companion object {

        private const val ARG_DATA = "ARG_DATA"
        private const val ARG_POSITION = "ARG_POSITION"
        private const val ARG_TITLE = "ARG_TITLE"

        fun instance(
            fragment: Fragment,
            requestCode: Int,
            title: String,
            data: ArrayList<String>,
            selectedPosition: Int
        ): FullscreenDialog =
            MapFiltersConfirmationDialog().apply {
                setTargetFragment(fragment, requestCode)
                args {
                    putStringArrayList(ARG_DATA, data)
                    putInt(ARG_POSITION, selectedPosition)
                    putString(ARG_TITLE, title)
                }
            }
    }

    interface Callback {
        fun onOptionSelected(code: Int, value: String)
    }
}