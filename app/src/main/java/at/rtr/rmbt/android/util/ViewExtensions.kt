package at.rtr.rmbt.android.util

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import at.rtr.rmbt.android.R
import android.graphics.Paint
import android.graphics.Rect
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
fun Float.format(): String = DecimalFormat("#.##").format(this)