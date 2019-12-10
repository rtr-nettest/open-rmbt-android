package at.rtr.rmbt.android.ui.dialog

import android.app.Activity
import android.content.Intent
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

class InputSettingDialog : FullscreenDialog() {

    private lateinit var binding: DialogInputSettingBinding

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

        arguments?.getInt(KEY_INPUT_TYPE)?.let {
            binding.editTextValue.inputType = it

            if (it == InputType.TYPE_CLASS_NUMBER) {
                binding.editTextValue.filters =
                    arrayOf<InputFilter>(InputFilter.LengthFilter(MAX_VALUE_LENGTH))
            }
        }

        binding.labelTitle.text = arguments?.getString(KEY_DIALOG_TITLE)
        binding.editTextValue.setText(arguments?.getString(KEY_DEFAULT_VALUE))
        binding.editTextValue.text?.length?.let { binding.editTextValue.setSelection(it) }

        binding.buttonOkay.setOnClickListener {

            val value: String = binding.editTextValue.text.toString()
            if (value.isNotEmpty()) {

                val bundle = Bundle()
                bundle.putString(KEY_VALUE, value)
                val intent = Intent()
                intent.putExtras(bundle)

                targetFragment?.onActivityResult(
                    targetRequestCode,
                    Activity.RESULT_OK,
                    intent
                )
            }

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

    companion object {

        private const val KEY_DIALOG_TITLE: String = "key_dialog_title"
        private const val KEY_DEFAULT_VALUE: String = "key_default_value"
        private const val KEY_INPUT_TYPE: String = "key_input_type"
        private const val MAX_VALUE_LENGTH = 5
        const val KEY_VALUE: String = "key_value"

        fun instance(
            title: String,
            defaultValue: String,
            fragment: Fragment,
            requestCode: Int,
            inputType: Int = InputType.TYPE_CLASS_NUMBER
        ): FullscreenDialog {

            val inputSettingDialog = InputSettingDialog()
            inputSettingDialog.setTargetFragment(fragment, requestCode)
            inputSettingDialog.args {
                putString(KEY_DIALOG_TITLE, title)
                putString(KEY_DEFAULT_VALUE, defaultValue)
                putInt(KEY_INPUT_TYPE, inputType)
            }
            return inputSettingDialog
        }
    }
}