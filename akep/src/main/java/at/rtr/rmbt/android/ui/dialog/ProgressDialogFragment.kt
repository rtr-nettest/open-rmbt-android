package at.rtr.rmbt.android.ui.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import at.rtr.rmbt.android.R

class ProgressDialogFragment : FullscreenDialog() {

    override val cancelable: Boolean
        get() = false

    override val dimBackground: Boolean
        get() = false

    override val gravity: Int
        get() = Gravity.CENTER

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_progress, container, false)
    }
}