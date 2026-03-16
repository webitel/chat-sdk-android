package com.webitel.chat.sdk.internal.repository

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import com.webitel.chat.sdk.internal.client.ChatClientImpl.Companion.logger
import com.webitel.chat.sdk.internal.repository.storage.DeviceInfoStorage
import java.util.UUID

internal class DeviceInfoRepository(
    private val storage: DeviceInfoStorage
) {

    fun getDeviceId(): String {
        var savedId = storage.getDeviceId()

        if (savedId.isNullOrEmpty()) {
            savedId = generateDeviceId()
            storage.saveDeviceId(savedId)
        }

        return savedId
    }


    fun getUserAgent(application: Application): String {
        val packageManager = application.packageManager
        val packageName = application.packageName

        var appName = "Unknown"
        var versionName = "Unknown"

        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }

            appName = packageInfo.applicationInfo?.loadLabel(packageManager)?.toString() ?: "Unknown"
            versionName = packageInfo.versionName ?: "Unknown"

        } catch (e: Exception) {
            logger.error("DeviceInfoRepository", "Failed to get app info: ${e.message}")
        }

        val result = StringBuilder(64)
        result.append("$appName/")
        result.append(versionName) // such as 1.1.0 System.getProperty("java.vm.version")
        result.append(" (Linux; U; Android ")
        val version = Build.VERSION.RELEASE
        result.append(version.ifEmpty { "1.0" })

        // add the model for the release build
        if ("REL" == Build.VERSION.CODENAME) {
            val model = Build.MODEL
            if (model.isNotEmpty()) {
                result.append("; ")
                result.append(model)
            }
        }

        val id = Build.ID
        if (id.isNotEmpty()) {
            result.append(" Build/")
            result.append(id)
        }
        result.append(")")

        return result.toString()
    }


    private fun generateDeviceId(): String {
        return UUID.randomUUID().toString()
    }
}