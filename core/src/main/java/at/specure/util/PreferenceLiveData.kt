package at.specure.util

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData

abstract class PreferenceLiveData<T>(protected val preferences: SharedPreferences, private val key: String, private val defaultValue: T?) :
    MutableLiveData<T>(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (this.key == key) {
            postValue(readValue(this.key, defaultValue))
        }
    }

    override fun onActive() {
        super.onActive()
        preferences.registerOnSharedPreferenceChangeListener(this)
        postValue(readValue(this.key, defaultValue))
    }

    override fun onInactive() {
        super.onInactive()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    protected abstract fun readValue(key: String, defaultValue: T?): T?
}

class StringPreferenceLiveData(preferences: SharedPreferences, key: String, defaultValue: String?) :
    PreferenceLiveData<String>(preferences, key, defaultValue) {

    override fun readValue(key: String, defaultValue: String?): String? = preferences.getString(key, defaultValue)
}

class StringSetPreferenceLiveData(preferences: SharedPreferences, key: String, defaultValue: Set<String>?) :
    PreferenceLiveData<Set<String>?>(preferences, key, defaultValue) {

    override fun readValue(key: String, defaultValue: Set<String>?): Set<String>? = preferences.getStringSet(key, defaultValue)
}