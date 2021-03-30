package at.rtr.rmbt.android.ui.dialog

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog

object Dialogs {

    fun show(
        context: Context,
        title: String,
        message: String,
        @StringRes positiveButtonRes: Int = android.R.string.ok,
        cancelable: Boolean = true,
        onDismiss: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonRes) { _, _ -> onDismiss?.invoke() }
            .setCancelable(cancelable)
            .show()
    }
}