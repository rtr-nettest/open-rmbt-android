package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableField

private const val KEY_TEXT = "KEY_TEXT"

class StatisticsViewState : ViewState {

    val text = ObservableField<String?>()

    override fun onRestoreState(bundle: Bundle?) {
        text.set(bundle?.getString(KEY_TEXT))
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.putString(KEY_TEXT, text.get())
    }
}