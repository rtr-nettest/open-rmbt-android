package at.rtr.rmbt.android.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogCancelMeasurementBinding

class CancelMeasurementDialog : FullscreenDialog() {

    private lateinit var binding: DialogCancelMeasurementBinding
    private var callback: CancelMeasurementCallback? = null

    override val gravity = Gravity.CENTER
    override val dimBackground = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment is CancelMeasurementCallback) {
            callback = parentFragment as CancelMeasurementCallback
        } else if (activity is CancelMeasurementCallback) {
            callback = activity as CancelMeasurementCallback
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_cancel_measurement, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonCancel.setOnClickListener {
            callback?.onCancel()
            dismissAllowingStateLoss()
        }
        binding.buttonContinue.setOnClickListener {
            dismiss()
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    companion object {

        fun instance(): FullscreenDialog = CancelMeasurementDialog()
    }
}

interface CancelMeasurementCallback {
    fun onCancel()
}