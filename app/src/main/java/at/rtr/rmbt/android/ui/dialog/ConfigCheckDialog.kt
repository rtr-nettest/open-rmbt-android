package at.rtr.rmbt.android.ui.dialog

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogCheckConfigBinding
import at.rtr.rmbt.android.util.args

class ConfigCheckDialog : FullscreenDialog() {

    private lateinit var binding: DialogCheckConfigBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_check_config, container, false)
        return binding.root
    }

    @Suppress("UNCHECKED_CAST")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val values = arguments!!.getSerializable(KEY_VALUES) as HashMap<String, String>
        val message = buildString {
            values.forEach {
                append(it.key)
                append('\n')
                append(it.value)
                append('\n')
                append('\n')
            }
        }
        val spannable = SpannableStringBuilder(message)
        values.forEach {
            val position = message.indexOf(it.key)
            if (position != -1) {
                spannable.setSpan(StyleSpan(Typeface.BOLD), position, position + it.key.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        binding.labelMessage.text = spannable
        binding.buttonOk.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    companion object {

        private const val KEY_VALUES = "KEY_VALUES"

        fun show(manager: FragmentManager, incorrectValues: Map<String, String>) {
            val dialog = ConfigCheckDialog().args {
                val hashMap = HashMap<String, String>()
                incorrectValues.forEach {
                    hashMap[it.key] = it.value
                }
                putSerializable(KEY_VALUES, hashMap)
            }
            dialog.show(manager, ConfigCheckDialog::class.java.name)
        }
    }
}