package com.webitel.chat.sdk.internal.repository.storage

import android.app.Application
import android.content.Context
import android.content.SharedPreferences


internal class DeviceInfoStorageSharedPref(application: Application): DeviceInfoStorage {

    private val STORE_KEY_DEVICE_ID_DATA = "device_id"
    private val SHARED_PREFS = "webitel_chat_sdk"

    private val mPreferences: SharedPreferences = application.getSharedPreferences(
        SHARED_PREFS,
        Context.MODE_PRIVATE
    )
    private val editor: SharedPreferences.Editor = mPreferences.edit()


    override fun getDeviceId(): String? {
        return mPreferences.getString(STORE_KEY_DEVICE_ID_DATA, "")
    }


    override fun saveDeviceId(id: String) {
        editor.putString(STORE_KEY_DEVICE_ID_DATA, id).commit()
    }


    override fun clear() {
        editor.putString(STORE_KEY_DEVICE_ID_DATA, null).commit()
    }
}