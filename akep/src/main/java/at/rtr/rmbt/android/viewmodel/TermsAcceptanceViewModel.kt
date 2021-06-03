package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.specure.data.TermsAndConditions
import at.specure.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
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

    fun getTac() = launch {
        settingsRepository.getTermsAndConditions()
            .flowOn(Dispatchers.IO)
            .collect {
                _tacContentLiveData.postValue(it)
            }
    }

    fun updateTermsAcceptance(accepted: Boolean) {
        tac.tacAccepted = accepted
    }
}