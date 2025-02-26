package at.rtr.rmbt.android.ui.dialog

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import at.rtr.rmbt.android.R
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

open class FullscreenDialog : DialogFragment(), CoroutineScope {

    open val gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP

    open val dimBackground = false

    open val cancelable = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(cancelable)
        isCancelable = cancelable
        return dialog
    }

    fun setCanceledOnTouchOutside(cancelable: Boolean) {
        dialog?.setCanceledOnTouchOutside(cancelable)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        dialog?.let {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            dialog.window?.setLayout(width, height)
            dialog.window?.setGravity(gravity)
            dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            if (dimBackground) {
                dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
        }
    }

    fun show(activity: FragmentActivity?) {

        val supportFragmentManager = activity?.supportFragmentManager
        if (this.isAdded) {
            supportFragmentManager?.beginTransaction()?.let {
                val prev = supportFragmentManager.findFragmentByTag("dialog")
                if (prev != null) {
                    it.remove(prev)
                }
                it.addToBackStack(null)
                show(it, "dialog")
                dialog?.show()
            }
        }
    }

    fun show(fragmentManager: FragmentManager?) {
        if (this.isAdded) {
            fragmentManager?.beginTransaction()?.let {
                val prev = fragmentManager.findFragmentByTag("dialog")
                if (prev != null) {
                    it.remove(prev)
                }
                it.addToBackStack(null)
                show(it, "dialog")
                dialog?.show()
            }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = lifecycleScope.coroutineContext
}