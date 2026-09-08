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
            // when accepting offline before any server fetch - the bundled terms version. A later
            // settings fetch then only re-prompts if the server has a strictly newer version.
            tac.acceptedTacVersion = tac.tacVersion ?: tac.bundledTermsVersion
        }
    }
}