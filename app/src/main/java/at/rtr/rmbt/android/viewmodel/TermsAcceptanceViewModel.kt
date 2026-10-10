package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.specure.data.TermsAndConditions
import at.specure.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

class TermsAcceptanceViewModel @Inject constructor(private val tac: TermsAndConditions, private val settingsRepository: SettingsRepository) :
    BaseViewModel() {

    private val _tacContentLiveData = MutableLiveData<String>()

    val tacContentLiveData: LiveData<String?>
        get() {
            return _tacContentLiveData
        }

    fun getTac() = launch(CoroutineName("getTAC")) {
        settingsRepository.getTermsAndConditions()
            .flowOn(Dispatchers.IO)
            .collect {
                _tacContentLiveData.postValue(it)
            }
    }

    fun updateTermsAcceptance(accepted: Boolean) {
        tac.tacAccepted = accepted
        if (accepted) {
            // Record which version was accepted: the version currently known from the server, or -
            // when accepting offline before any server fetch - the bundled terms version. Never
            // record below the bundled version (the server value can still be stale/lower than what
            // this apk bundles at accept time), so the startup bundled-vs-accepted gate is satisfied
            // and we don't re-prompt on every launch. A later settings fetch then only re-prompts if
            // the server has a strictly newer version.
            tac.acceptedTacVersion = maxOf(tac.tacVersion ?: tac.bundledTermsVersion, tac.bundledTermsVersion)
        }
    }
}