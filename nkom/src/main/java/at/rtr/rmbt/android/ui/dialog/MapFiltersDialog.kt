package at.rtr.rmbt.android.ui.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogFiltersBinding
import at.rtr.rmbt.android.di.Injector
import at.rtr.rmbt.android.viewmodel.MapFiltersViewModel

class MapFiltersDialog : FullscreenDialog(), MapFiltersConfirmationDialog.Callback {

    override val gravity: Int = Gravity.BOTTOM

    override val dimBackground: Boolean = false

    private lateinit var viewModel: MapFiltersViewModel
    private lateinit var binding: DialogFiltersBinding

    private val callback: Callback?
        get() = when {
            targetFragment is Callback -> targetFragment as Callback
            activity is Callback -> activity as Callback
            else -> null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null || !::viewModel.isInitialized) {
            val provider = ViewModelProviders.of(this, Injector.component.viewModelFactory())
            viewModel = provider.get(MapFiltersViewModel::class.java)
        } else {
            viewModel.onRestoreState(savedInstanceState)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_filters, container, false)
        binding.state = viewModel.state
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val displayedValuesT = viewModel.monthDisplayForYearHashMap[viewModel.yearList[0]]?.toTypedArray() ?: listOf<String>().toTypedArray()
        val currentYearMonths = viewModel.monthNumbersForYearHashMap[viewModel.yearList[0]]
        val maxValueT = currentYearMonths!!.size - 1

        binding.monthPicker.apply {
            displayedValues = displayedValuesT
            maxValue = maxValueT
            minValue = 0
            wrapSelectorWheel = false
            value = currentYearMonths!!.size - viewModel.currentMonthNumberToDisplay - 1
        }

        binding.yearPicker.apply {
            displayedValues = viewModel.yearDisplayNames.toTypedArray()
            maxValue = viewModel.yearList.size - 1
            minValue = 0
            wrapSelectorWheel = false
            value = minValue
        }

        binding.yearPicker.setOnValueChangedListener { picker, oldVal, newVal ->
            if (oldVal != newVal) {
                updateMonthPickerList(newVal)
            }
        }

        binding.close.setOnClickListener { dismiss() }
    }

    private fun updateMonthPickerList(newVal: Int) {
        val selectedMonth = binding.monthPicker.value
        val selectedYear = viewModel.yearList[newVal]
        binding.monthPicker.apply {
            value = minValue
            displayedValues = viewModel.monthDisplayForYearHashMap[selectedYear]?.toTypedArray() ?: listOf<String>().toTypedArray()
            maxValue = viewModel.monthNumbersForYearHashMap[selectedYear]!!.size - 1
            minValue = 0
            wrapSelectorWheel = false
//            value = if (selectedMonth in minValue..maxValue) {
//                selectedMonth
//            } else minValue

        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.onSaveState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        viewModel.onRestoreState(savedInstanceState)
    }

    override fun onOptionSelected(code: Int, value: String) {
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        callback?.onFiltersUpdated()
    }

    companion object {

        const val CODE_TYPE = 0
        const val CODE_SUBTYPE = 1
        const val CODE_STATISTICS = 2
        const val CODE_TECHNOLOGY = 3
        const val CODE_OPERATOR = 4
        const val CODE_PERIOD = 5
        const val CODE_PROVIDER = 6

        fun instance(fragment: Fragment, requestCode: Int): FullscreenDialog = MapFiltersDialog().apply { setTargetFragment(fragment, requestCode) }
    }

    interface Callback {
        fun onFiltersUpdated()
    }
}