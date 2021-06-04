package at.rmbt.cms.client

import androidx.lifecycle.MutableLiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class PageRequestCallback(val liveData: MutableLiveData<String>) : Callback<PageResponse> {
    override fun onResponse(call: Call<PageResponse>, response: Response<PageResponse>) {
        if (response.isSuccessful){
            response.body().let {
                val translation = it!!.translations.findLast { t -> t.language!!.equals(Locale.getDefault()) }
                if (translation != null) {
                    liveData.postValue(translation.content)
                } else {
                    liveData.postValue(it.content)
                }
            }
        }
    }
    override fun onFailure(call: Call<PageResponse>, t: Throwable) {
        liveData.postValue("An error appeared while retrieving data.")
    }
}