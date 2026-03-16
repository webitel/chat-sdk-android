package com.webitel.chat.sdk.internal.repository.storage

internal interface DeviceInfoStorage {
    fun getDeviceId(): String?

    fun saveDeviceId(id: String)

    fun clear()
}