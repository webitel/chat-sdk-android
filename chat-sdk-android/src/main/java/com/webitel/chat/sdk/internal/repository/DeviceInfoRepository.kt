package com.webitel.chat.sdk.internal.repository

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import com.webitel.chat.sdk.BuildConfig
import com.webitel.chat.sdk.internal.client.ChatClientImpl.Companion.logger
import com.webitel.chat.sdk.internal.repository.storage.DeviceInfoStorage
import java.util.UUID


internal class DeviceInfoRepository(
    private val context: Application,
    private val storage: DeviceInfoStorage
) {
    private var userAgent: String? = null

    fun getDeviceId(): String {
        var savedId = storage.getDeviceId()

        if (savedId.isNullOrEmpty()) {
            savedId = UUID.randomUUID().toString()
            storage.saveDeviceId(savedId)
        }

        return savedId
    }


    fun getUserAgent(): String {
        return userAgent ?: buildUserAgent().also { userAgent = it }
    }


    private fun buildUserAgent(): String {
        val pm = context.packageManager
        val packageName = context.packageName

        val (appName, versionName) = try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {

                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }

            val name = packageInfo.applicationInfo?.loadLabel(pm)?.toString() ?: "app"
            val version = packageInfo.versionName ?: "unknown"
            name to version
        } catch (e: Exception) {
            logger.error("DeviceInfoRepository", "Failed to get app info: ${e.message}")
            "app" to "unknown"
        }

        val osVersion = Build.VERSION.RELEASE.ifEmpty { "unknown" }
        val device = Build.MODEL.takeIf { it.isNotEmpty() } ?: "device"
        return "$appName/$versionName (Android $osVersion; $device) ${sdkVersion()}"
    }

    private fun sdkVersion(): String {
        return "chat-sdk-android/${ BuildConfig.VERSION_NAME}"
    }
}