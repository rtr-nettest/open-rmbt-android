package at.rtr.rmbt.android.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import at.rtr.rmbt.android.R
import at.specure.data.NetworkTypeCompat

class ResultsTechnologyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var technology: NetworkTypeCompat? = null
        set(value) {
            removeAllViews()

            when (value) {
                NetworkTypeCompat.TYPE_2G -> addMobile(context.getString(R.string.technology_2g))
                NetworkTypeCompat.TYPE_3G -> addMobile(context.getString(R.string.technology_3g))
                NetworkTypeCompat.TYPE_5G_AVAILABLE, NetworkTypeCompat.TYPE_4G -> addMobile(context.getString(R.string.technology_4g))
                NetworkTypeCompat.TYPE_WLAN -> addView(ImageView(context).apply {
                    layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setImageResource(R.drawable.ic_results_wifi)
                    requestLayout()
                })
                NetworkTypeCompat.TYPE_UNKNOWN -> {
                    R.drawable.ic_history_no_internet
                }
                NetworkTypeCompat.TYPE_LAN,
                NetworkTypeCompat.TYPE_BROWSER -> {
                    R.drawable.ic_browser
                }
                NetworkTypeCompat.TYPE_5G_NSA,
                NetworkTypeCompat.TYPE_5G -> addMobile(context.getString(R.string.technology_5g))
            }
            field = value
            invalidate()
        }

    @SuppressLint("InflateParams")
    private fun addMobile(text: String) {
        val view = LayoutInflater.from(context).inflate(R.layout.view_result_technology, null, false)
        view.findViewById<TextView>(R.id.technology).text = text
        addView(view)
    }
}