package at.rtr.rmbt.android.ui.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogFiltersBinding
import at.rtr.rmbt.android.di.Injector
import at.rtr.rmbt.android.util.listen
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
            val provider = ViewModelProvider(this, Injector.component.viewModelFactory())
            viewModel = provider[MapFiltersViewModel::class.java]
            viewModel.obtain()
        } else {
            viewModel.onRestoreState(savedInstanceState)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_filters, container, false)
        binding.state = viewModel.state
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insetsSystemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val insetsDisplayCutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val insetsTouch = windowInsets.getInsets(WindowInsetsCompat.Type.tappableElement())

            val topSafeMargin = maxOf(insetsSystemBars.top, insetsDisplayCutout.top, insetsTouch.top)
            val lefSafetMargin = maxOf(insetsSystemBars.left, insetsDisplayCutout.left, insetsTouch.left)
            val rightSafeMargin = maxOf(insetsSystemBars.right, insetsDisplayCutout.right, insetsTouch.right)

            binding.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = topSafeMargin
                leftMargin = lefSafetMargin
                rightMargin = rightSafeMargin
            }

            WindowInsetsCompat.CONSUMED
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.type.setOnClickListener {
            viewModel.typesLiveData.listen(this) {
                viewModel.typesLiveData.removeObservers(this)
                MapFiltersConfirmationDialog.instance(
                    this,
                    CODE_TYPE,
                    getString(R.string.title_filters_type),
                    it as ArrayList<String>,
                    it.indexOf(it.find { it == viewModel.state.type.get() })
                ).show(parentFragmentManager)
            }
        }

        binding.statistic.setOnClickListener {
            viewModel.statisticsLiveData.value?.let {
                MapFiltersConfirmationDialog.instance(
                    this,
                    CODE_STATISTICS,
                    it.first,
                    it.second.map { it.title } as ArrayList<String>,
                    it.second.indexOf(it.second.find { it == viewModel.state.statistical.get() })
                ).show(parentFragmentManager)
            }
        }

        binding.period.setOnClickListener {
            viewModel.periodLiveData.value?.let {
                MapFiltersConfirmationDialog.instance(
                    this,
                    CODE_PERIOD,
                    it.first,
                    it.second.map { it.title } as ArrayList<String>,
                    it.second.indexOf(it.second.find { it == viewModel.state.timeRange.get() })
                ).show(parentFragmentManager)
            }
        }

        binding.operator.setOnClickListener {
            viewModel.operatorLiveData.value?.let {
                if (it.second.isNotEmpty()) {
                    MapFiltersConfirmationDialog.instance(
                        this,
                        CODE_OPERATOR,
                        it.first,
                        it.second.map { it.title } as ArrayList<String>,
                        it.second.indexOf(it.second.find { it == viewModel.state.operator.get() })
                    ).show(parentFragmentManager)
                }
            }
        }

        binding.provider.setOnClickListener {
            viewModel.providerLiveData.value?.let {
                if (it.second.isNotEmpty()) {
                    MapFiltersConfirmationDialog.instance(
                        this,
                        CODE_PROVIDER,
                        it.first,
                        it.second.map { it.title } as ArrayList<String>,
                        it.second.indexOf(it.second.find { it == viewModel.state.provider.get() })
                    ).show(parentFragmentManager)
                }
            }
        }

        binding.technology.setOnClickListener {
            viewModel.technologyData.value?.let {
                MapFiltersConfirmationDialog.instance(
                    this,
                    CODE_TECHNOLOGY,
                    it.first,
                    it.second.map { it.title } as ArrayList<String>,
                    it.second.indexOf(it.second.find { it == viewModel.state.technology.get() })
                ).show(parentFragmentManager)
            }
        }

        binding.iconClose.setOnClickListener { dismiss() }
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
        when (code) {
            CODE_TYPE -> {
                viewModel.markTypeAsSelected(value)
                viewModel.subtypesLiveData.listen(this) {
                    if (it.first == value) {
                        viewModel.subtypesLiveData.removeObservers(this)
                        MapFiltersConfirmationDialog.instance(
                            this,
                            CODE_SUBTYPE,
                            getString(R.string.title_filters_subtype),
                            it.second.map { it.title } as ArrayList<String>,
                            it.second.indexOf(it.second.find { it.title == viewModel.state.subtype.get()?.title } ?: 0)
                        ).show(parentFragmentManager)
                    }
                }
            }
            CODE_SUBTYPE -> viewModel.markSubtypeAsSelected(value)
            CODE_STATISTICS -> viewModel.markStatisticsAsSelected(value)
            CODE_PERIOD -> viewModel.markPeriodAsSelected(value)
            CODE_OPERATOR -> viewModel.markOperatorAsSelected(value)
            CODE_TECHNOLOGY -> viewModel.markTechnologyAsSelected(value)
            CODE_PROVIDER -> viewModel.markProviderAsSelected(value)
        }
        if (code != CODE_TYPE) {
            callback?.onFiltersUpdated()
        }
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