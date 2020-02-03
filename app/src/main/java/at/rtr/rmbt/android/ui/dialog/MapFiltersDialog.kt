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
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.MapFiltersViewModel

class MapFiltersDialog : FullscreenDialog(), MapFiltersConfirmationDialog.Callback {

    override val gravity: Int = Gravity.BOTTOM

    override val dimBackground: Boolean = false

    private lateinit var viewModel: MapFiltersViewModel
    private lateinit var binding: DialogFiltersBinding

    init {
        retainInstance = true
    }

    private val callback: Callback?
        get() = when {
            targetFragment is Callback -> targetFragment as Callback
            activity is Callback -> activity as Callback
            else -> null
        }

    private var dialog: FullscreenDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null || !::viewModel.isInitialized) {
            val provider = ViewModelProviders.of(this, Injector.component.viewModelFactory())
            viewModel = provider.get(MapFiltersViewModel::class.java)
            viewModel.obtain()
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

        binding.type.setOnClickListener {
            viewModel.typesLiveData.listen(this) {
                dialog = MapFiltersConfirmationDialog.instance(
                    this,
                    CODE_TYPE,
                    getString(R.string.title_filters_type),
                    it as ArrayList<String>,
                    it.indexOf(it.find { it == viewModel.state.type.get() })
                )
                dialog?.show(fragmentManager)
            }
        }

        binding.statistic.setOnClickListener {
            viewModel.statisticsLiveData.value?.let {
                dialog = MapFiltersConfirmationDialog.instance(
                    this,
                    CODE_STATISTICS,
                    it.first,
                    it.second.map { it.title } as ArrayList<String>,
                    it.second.indexOf(it.second.find { it == viewModel.state.statistical.get() })
                )
                dialog?.show(fragmentManager)
            }
        }

        binding.period.setOnClickListener {
            viewModel.periodLiveData.value?.let {
                dialog = MapFiltersConfirmationDialog.instance(
                    this,
                    CODE_PERIOD,
                    it.first,
                    it.second.map { it.title } as ArrayList<String>,
                    it.second.indexOf(it.second.find { it == viewModel.state.timeRange.get() })
                )
                dialog?.show(fragmentManager)
            }
        }

        binding.operator.setOnClickListener {
            viewModel.operatorLiveData.value?.let {
                if (it.second.isNotEmpty()) {
                    dialog = MapFiltersConfirmationDialog.instance(
                        this,
                        CODE_OPERATOR,
                        it.first,
                        it.second.map { it.title } as ArrayList<String>,
                        it.second.indexOf(it.second.find { it == viewModel.state.operator.get() })
                    )
                }
            }
            dialog?.show(fragmentManager)
        }

        binding.provider.setOnClickListener {
            viewModel.providerLiveData.value?.let {
                if (it.second.isNotEmpty()) {
                    dialog = MapFiltersConfirmationDialog.instance(
                        this,
                        CODE_PROVIDER,
                        it.first,
                        it.second.map { it.title } as ArrayList<String>,
                        it.second.indexOf(it.second.find { it == viewModel.state.provider.get() })
                    )
                }
            }
            dialog?.show(fragmentManager)
        }

        binding.technology.setOnClickListener {
            viewModel.technologyData.value?.let {
                dialog = MapFiltersConfirmationDialog.instance(
                    this,
                    CODE_TECHNOLOGY,
                    it.first,
                    it.second.map { it.title } as ArrayList<String>,
                    it.second.indexOf(it.second.find { it == viewModel.state.technology.get() })
                )
                dialog?.show(fragmentManager)
            }
        }

        binding.iconClose.setOnClickListener { dismiss() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.onSaveState(outState)
    }

    override fun onOptionSelected(code: Int, value: String) {
        when (code) {
            CODE_TYPE -> {
                viewModel.subtypesLiveData.listen(this) {
                    viewModel.subtypesLiveData.removeObservers(this)
                    dialog = MapFiltersConfirmationDialog.instance(
                        this,
                        CODE_SUBTYPE,
                        getString(R.string.title_filters_subtype),
                        it.second.map { it.title } as ArrayList<String>,
                        it.second.indexOf(it.second.find { it.title == viewModel.state.subtype.get()?.title } ?: 0)
                    )
                    dialog?.show(fragmentManager)
                }
                viewModel.markTypeAsSelected(value)
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
        dialog?.dismiss()
        dialog = null
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