package at.rtr.rmbt.android.ui.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogFiltersConfirmationBinding
import at.rtr.rmbt.android.ui.adapter.HistoryFilterConfirmationAdapter
import at.rtr.rmbt.android.util.args
import kotlin.math.min

class HistoryFiltersConfirmationDialog : FullscreenDialog() {

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

    private val adapter = HistoryFilterConfirmationAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_filters_confirmation, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.init(arguments?.getStringArray(ARG_DATA)?.toMutableList(), arguments?.getStringArray(ARG_SELECTED)?.toMutableSet())
        val title = arguments?.getString(ARG_TITLE)

        binding.label.text = title
        binding.buttonText = getString(R.string.text_filter_confirm)
        binding.items.adapter = adapter
        binding.accept.setOnClickListener {
            callback?.onOptionSelected(targetRequestCode, adapter.selected)
            dismiss()
        }

        binding.items.post {
            binding.items.layoutParams.height = min((binding.root.measuredHeight * 0.6f).toInt(), binding.items.measuredHeight)
            binding.root.requestLayout()
        }
    }

    companion object {

        private const val ARG_DATA = "ARG_DATA"
        private const val ARG_SELECTED = "ARG_SELECTED"
        private const val ARG_TITLE = "ARG_TITLE"

        fun instance(
            fragment: Fragment,
            requestCode: Int,
            title: String,
            data: Set<String>?,
            selected: Set<String>?
        ): FullscreenDialog =
            HistoryFiltersConfirmationDialog().apply {
                setTargetFragment(fragment, requestCode)
                args {
                    putStringArray(ARG_DATA, data?.toTypedArray())
                    putStringArray(ARG_SELECTED, selected?.toTypedArray())
                    putString(ARG_TITLE, title)
                }
            }
    }

    interface Callback {
        fun onOptionSelected(code: Int, selected: Set<String>)
    }
}