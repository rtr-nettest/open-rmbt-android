package at.rmbt.cms.client

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class PageRequestCallback(
    private val _context: Context,
    private val _liveData: MutableLiveData<String>
    ) : Callback<PageResponse> {
    override fun onResponse(call: Call<PageResponse>, response: Response<PageResponse>) {
        if (response.isSuccessful && response.body() != null){
            response.body()!!.let {
                val translation = it.translations.findLast { t -> t.language == Locale.getDefault().language }
                if (translation != null) {
                    _liveData.postValue(translation.content)
                } else {
                    _liveData.postValue(it.content)
                }
            }
        } else {
            onFailure(call, Throwable())
        }
    }
    override fun onFailure(call: Call<PageResponse>, t: Throwable) {
        t.message?.let { Log.e(PageRequestCallback::class.java.name, it) }
        _liveData.postValue(_context.getString(R.string.request_failure))
    }
}