package at.rtr.rmbt.android.ui.dialog

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogSimpleBinding

class SimpleDialog : FullscreenDialog() {

    private lateinit var binding: DialogSimpleBinding

    override val gravity: Int
        get() = Gravity.CENTER

    override val cancelable: Boolean
        get() = builder.cancelable

    override val dimBackground: Boolean
        get() = true

    private val callback: Callback?
        get() = when {
            parentFragment is Callback -> parentFragment as Callback
            activity is Callback -> activity as Callback
            else -> null
        }

    private val builder: Builder
        get() = arguments?.getParcelable(KEY_BUILDER) ?: throw IllegalArgumentException("arguments is missing")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_simple, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (builder.positiveTextRes == 0) {
            binding.buttonPositive.visibility = View.GONE
        } else {
            binding.buttonPositive.setText(builder.positiveTextRes)
        }

        if (builder.negativeTextRes == 0) {
            binding.buttonNegative.visibility = View.GONE
        } else {
            binding.buttonNegative.setText(builder.negativeTextRes)
        }

        binding.buttonPositive.setOnClickListener {
            callback?.onDialogPositiveClicked(builder.code)
            dismissAllowingStateLoss()
        }

        binding.buttonNegative.setOnClickListener {
            callback?.onDialogNegativeClicked(builder.code)
            dismissAllowingStateLoss()
        }

        if (builder.messageText == null) {
            binding.textMessage.setText(builder.messageTextRes)
        } else {
            binding.textMessage.text = builder.messageText
        }
    }

    interface Callback {

        fun onDialogPositiveClicked(code: Int)

        fun onDialogNegativeClicked(code: Int)
    }

    class Builder constructor() : Parcelable {

        var messageText: String? = null
        var messageTextRes = 0
        var positiveTextRes = 0
        var negativeTextRes = 0
        var cancelable = true
        var code = 0

        constructor(parcel: Parcel) : this() {
            messageText = parcel.readString()
            messageTextRes = parcel.readInt()
            positiveTextRes = parcel.readInt()
            negativeTextRes = parcel.readInt()
            cancelable = parcel.readInt() == 1
            code = parcel.readInt()
        }

        override fun writeToParcel(dest: Parcel?, flags: Int) {
            dest?.writeString(messageText)
            dest?.writeInt(messageTextRes)
            dest?.writeInt(positiveTextRes)
            dest?.writeInt(negativeTextRes)
            dest?.writeInt(if (cancelable) 1 else 0)
            dest?.writeInt(code)
        }

        override fun describeContents() = 0

        fun positiveText(positiveTextRes: Int): Builder {
            this.positiveTextRes = positiveTextRes
            return this
        }

        fun negativeText(negativeTextRes: Int): Builder {
            this.negativeTextRes = negativeTextRes
            return this
        }

        fun messageText(messageTextRes: Int): Builder {
            this.messageTextRes = messageTextRes
            return this
        }

        fun messageText(messageText: String): Builder {
            this.messageText = messageText
            return this
        }

        fun cancelable(cancelable: Boolean): Builder {
            this.cancelable = cancelable
            return this
        }

        fun show(manager: FragmentManager, code: Int) {
            val tag = SimpleDialog::class.java.simpleName

            manager.findFragmentByTag(tag)?.let {
                if (it is DialogFragment) {
                    it.dismissAllowingStateLoss()
                }
            }

            this.code = code
            val dialog = SimpleDialog()
            val args = Bundle()
            args.putParcelable(KEY_BUILDER, this)
            dialog.arguments = args
            dialog.show(manager, tag)
        }

        companion object CREATOR : Parcelable.Creator<Builder> {
            override fun createFromParcel(parcel: Parcel): Builder {
                return Builder(parcel)
            }

            override fun newArray(size: Int): Array<Builder?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object {
        private const val KEY_BUILDER = "KEY_BUILDER"
    }
}