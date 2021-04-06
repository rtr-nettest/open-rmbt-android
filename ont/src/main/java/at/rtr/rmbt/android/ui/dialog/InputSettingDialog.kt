package at.rtr.rmbt.android.ui.dialog

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogInputSettingBinding
import at.rtr.rmbt.android.util.args
import at.rtr.rmbt.android.util.onTextChanged
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class InputSettingDialog : FullscreenDialog() {

    private lateinit var binding: DialogInputSettingBinding
    private var requestCode: Int = -1

    private val callback: Callback?
        get() = when {
            targetFragment is Callback -> targetFragment as Callback
            activity is Callback -> activity as Callback
            else -> null
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_input_setting, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getBoolean(KEY_IS_CANCELABLE)?.let {
            dialog?.setCanceledOnTouchOutside(it)
            isCancelable = it
        }

        arguments?.getInt(KEY_INPUT_TYPE)?.let {
            binding.editTextValue.inputType = it

            if (it == InputType.TYPE_CLASS_NUMBER) {
                binding.editTextValue.filters =
                    arrayOf<InputFilter>(InputFilter.LengthFilter(MAX_VALUE_LENGTH))
            }
        }

        arguments?.getInt(KEY_CODE)?.let { requestCode = it }

        binding.labelTitle.text = arguments?.getString(KEY_DIALOG_TITLE)
        binding.editTextValue.setText(arguments?.getString(KEY_DEFAULT_VALUE))
        binding.editTextValue.text?.length?.let { binding.editTextValue.setSelection(it) }

        binding.editTextValue.text?.length?.let { binding.editTextValue.setSelection(it) }

        val isEmptyInputAllowed = arguments?.getBoolean(KEY_IS_ALLOWED_EMPTY_INPUT, false) ?: false

        launch {
            val minValue = arguments?.getInt(KEY_MIN_VALUE)
            val maxValue = arguments?.getInt(KEY_MAX_VALUE)
            binding.editTextValue.onTextChanged().collect {
                if (isEmptyInputAllowed) {
                    binding.buttonOkay.isEnabled = true
                } else if ((minValue != null) && (maxValue != null) && ((minValue != 0) && (maxValue != 0) && minValue < maxValue)) {
                    checkNumber(it.toIntOrNull(), minValue, maxValue)
                } else {
                    binding.buttonOkay.isEnabled = it.isNotBlank()
                }
            }
        }

        binding.buttonOkay.setOnClickListener {
            callback?.onSelected(binding.editTextValue.text.toString(), requestCode)
            dismiss()
        }

        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        dialog?.let {
            dialog.window?.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.CENTER)
            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
    }

    private fun checkNumber(value: Int?, minValue: Int, maxValue: Int): Boolean {
        if (value == null || !isNumberValid(
                value,
                minValue,
                maxValue
            )
        ) {
            try {
                parentFragmentManager.let { fragManager ->
                    SimpleDialog.Builder()
                        .messageText(
                            String.format(
                                getString(R.string.loop_mode_max_delay_invalid),
                                minValue,
                                maxValue
                            )
                        )
                        .positiveText(android.R.string.ok)
                        .cancelable(false)
                        .show(fragManager, CODE_DIALOG_INVALID)
                }
            } catch (e: IllegalStateException) {
                // ignored
            }
            return false
        }
        return true
    }

    private fun isNumberValid(value: Int, minValue: Int, maxValue: Int) =
        value in minValue..maxValue

    companion object {

        private const val KEY_DIALOG_TITLE: String = "key_dialog_title"
        private const val KEY_DEFAULT_VALUE: String = "key_default_value"
        private const val KEY_INPUT_TYPE: String = "key_input_type"
        private const val KEY_IS_CANCELABLE: String = "key_is_cancelable"
        private const val KEY_IS_ALLOWED_EMPTY_INPUT: String = "key_is_allowed_empty_input"
        private const val KEY_CODE = "code"
        private const val MAX_VALUE_LENGTH = 8
        private const val KEY_MAX_VALUE = "key_max_value"
        private const val KEY_MIN_VALUE = "key_min_value"
        private const val CODE_DIALOG_INVALID = -1
        const val KEY_VALUE: String = "key_value"

        fun instance(
            title: String,
            defaultValue: String,
            fragment: Fragment? = null,
            requestCode: Int = -1,
            inputType: Int = InputType.TYPE_CLASS_NUMBER,
            isCancelable: Boolean = true,
            isEmptyInputAllowed: Boolean = false
        ): FullscreenDialog {

            val inputSettingDialog = InputSettingDialog()
            fragment?.let { inputSettingDialog.setTargetFragment(it, requestCode) }
            inputSettingDialog.args {
                putString(KEY_DIALOG_TITLE, title)
                putString(KEY_DEFAULT_VALUE, defaultValue)
                putInt(KEY_INPUT_TYPE, inputType)
                putBoolean(KEY_IS_CANCELABLE, isCancelable)
                putBoolean(KEY_IS_ALLOWED_EMPTY_INPUT, isEmptyInputAllowed)
                putInt(KEY_CODE, requestCode)
            }
            return inputSettingDialog
        }

        fun instance(
            title: String,
            defaultValue: String,
            fragment: Fragment? = null,
            requestCode: Int = -1,
            inputType: Int = InputType.TYPE_CLASS_NUMBER,
            isCancelable: Boolean = true,
            isEmptyInputAllowed: Boolean = false,
            minValue: Int,
            maxValue: Int
        ): FullscreenDialog {

            val inputSettingDialog = InputSettingDialog()
            fragment?.let { inputSettingDialog.setTargetFragment(it, requestCode) }
            inputSettingDialog.args {
                putString(KEY_DIALOG_TITLE, title)
                putString(KEY_DEFAULT_VALUE, defaultValue)
                putInt(KEY_INPUT_TYPE, inputType)
                putInt(KEY_MIN_VALUE, minValue)
                putInt(KEY_MAX_VALUE, maxValue)
                putBoolean(KEY_IS_CANCELABLE, isCancelable)
                putBoolean(KEY_IS_ALLOWED_EMPTY_INPUT, isEmptyInputAllowed)
                putInt(KEY_CODE, requestCode)
            }
            return inputSettingDialog
        }
    }

    interface Callback {
        fun onSelected(value: String, requestCode: Int)
    }
}