package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.specure.data.TermsAndConditions
import at.specure.data.entity.TacRecord
import at.specure.data.repository.TacRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

class TermsAcceptanceViewModel @Inject constructor(private val tac: TermsAndConditions, private val tacRepository: TacRepository) : BaseViewModel() {

    private val _tacContentLiveData = MutableLiveData<TacRecord>()

    val tacContentLiveData: LiveData<TacRecord?>
        get() {
            Timber.d("TAC: ${Locale.getDefault().language}, version: ${tac.tacVersion ?: -1}, tac url: ${tac.tacUrl ?: tac.defaultUrl}")
            return _tacContentLiveData
        }

    fun getTac() = launch {
        tacRepository.getTac(Locale.getDefault().language)
            .flowOn(Dispatchers.IO)
            .collect {
                _tacContentLiveData.postValue(it)
            }
    }
}