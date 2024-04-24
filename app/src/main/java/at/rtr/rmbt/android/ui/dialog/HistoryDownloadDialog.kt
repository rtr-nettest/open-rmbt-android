package at.rtr.rmbt.android.ui.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogDownloadHistoryBinding
import at.rtr.rmbt.android.databinding.DialogFiltersHistoryBinding
import at.rtr.rmbt.android.di.Injector
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.HistoryDownloadViewModel
import javax.inject.Inject

class HistoryDownloadDialog : FullscreenDialog() {

    override val gravity: Int = Gravity.BOTTOM

    override val dimBackground: Boolean = false

    @Inject
    lateinit var viewModel: HistoryDownloadViewModel

    private lateinit var binding: DialogDownloadHistoryBinding

    init {
        retainInstance = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Injector.inject(this)
        viewModel.onRestoreState(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.dialog_download_history, container, false)
        binding.state = viewModel.state
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.historyItemsLiveData.listen(this) {
            viewModel.state.isHistoryEmpty.set(it.isNullOrEmpty())
        }

        binding.buttonDownloadPdf.setOnClickListener {
            downloadFile("pdf")
        }

        binding.buttonDownloadXlsx.setOnClickListener {
            downloadFile("xlsx")
        }

        binding.buttonDownloadCsv.setOnClickListener {
            downloadFile("csv")
        }

        viewModel.downloadFileLiveData.listen(this) {
            if (it.error != null) {
                binding.buttonDownloadCsv.isEnabled = true
                binding.buttonDownloadXlsx.isEnabled = true
                binding.buttonDownloadPdf.isEnabled = true
                if (it.error == "ERROR_DOWNLOAD") {
                    Toast.makeText(this.context, R.string.error_during_download, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this.context, R.string.error_opening_file, Toast.LENGTH_SHORT).show()
                }
            }
            if (it.progress != null && it.file == null) {
                binding.buttonDownloadCsv.isEnabled = false
                binding.buttonDownloadXlsx.isEnabled = false
                binding.buttonDownloadPdf.isEnabled = false
            } else {
                binding.buttonDownloadCsv.isEnabled = true
                binding.buttonDownloadXlsx.isEnabled = true
                binding.buttonDownloadPdf.isEnabled = true
            }
        }

        binding.iconClose.setOnClickListener { dismiss() }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.onSaveState(outState)
    }

    private fun downloadFile(format: String) {
        viewModel.downloadFile(format)
    }

    companion object {

        fun instance(fragment: Fragment, requestCode: Int): FullscreenDialog =
            HistoryDownloadDialog().apply { setTargetFragment(fragment, requestCode) }
    }
}
