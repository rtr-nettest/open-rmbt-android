package at.rtr.rmbt.android.util

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import at.rtr.rmbt.android.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.DecimalFormat

/**
 * Change statusBarColor according to theme
 */
fun Window.changeStatusBarColor(theme: ToolbarTheme) {

    if (theme == ToolbarTheme.BLUE) {
        statusBarColor = ContextCompat.getColor(context, R.color.toolbar_blue)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            decorView.systemUiVisibility = 0
        }
    } else if (theme == ToolbarTheme.GRAY) {
        statusBarColor = ContextCompat.getColor(context, R.color.toolbar_gray)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    } else if (theme == ToolbarTheme.WHITE) {
        statusBarColor = ContextCompat.getColor(context, R.color.colorPrimary)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }
}

/**
 * Allows to put arguments in Dialog Fragment with Bundle as {this} reference
 */
fun DialogFragment.args(block: Bundle.() -> Unit): DialogFragment {
    val bundle = Bundle()
    block.invoke(bundle)
    arguments = bundle
    return this
}

/**
 * calculates the approximate width of a text, depending on a demo text
 * @param demoText
 * @return
 */
fun Paint.calcTextWidth(demoText: String): Float {
    return measureText(demoText)
}

/**
 * calculates the approximate height of a text, depending on a demo text
 * @param demoText
 * @return
 */
fun Paint.calcTextHeight(demoText: String): Float {

    val r = Rect()
    r.set(0, 0, 0, 0)
    getTextBounds(demoText, 0, demoText.length, r)
    return r.height().toFloat()
}

/**
 * This function is used for format value up-to 2 decimal
 */
fun Float.format(): String = DecimalFormat("@@").format(this)

/**
 * This function is used for format value up-to 2 decimal
 */
fun Long.format(): String = DecimalFormat("@@").format(this)

/**
 * Inflates and creates binding for a new view with [layoutId] view is attached to current [ViewGroup]
 */
fun <T : ViewDataBinding> ViewGroup.bindWith(@LayoutRes layoutId: Int): T {
    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    return DataBindingUtil.inflate(inflater, layoutId, this, false)
}

/**
 * Requests focus for current EditText and opens soft keyboard
 */
fun EditText.showKeyboard() {
    post {
        requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}

/**
 * Callback when Done is pressed on keyboard
 */
fun EditText.onDone(callback: () -> Unit) {
    setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            callback.invoke()
            true
        }
        false
    }
}

fun EditText.onTextChanged(): Flow<String> = callbackFlow {

    val watcher = object : TextWatcher {

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            offer((s ?: "").toString())
        }
    }

    addTextChangedListener(watcher)

    awaitClose { removeTextChangedListener(watcher) }
}